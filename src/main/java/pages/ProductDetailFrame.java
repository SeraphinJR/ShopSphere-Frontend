package pages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.json.*;

/**
 * ProductDetailFrame - shows full product details and reviews,
 * allows posting/editing/deleting user's reviews.
 *
 * Assumptions:
 * - GET /products/{id} returns a JSON object containing the product fields.
 * - GET /products/{id}/reviews returns JSON array of review objects:
 *     { "id": 123, "userId": 12, "userName": "Alice", "rating": 4.5, "text": "...", "createdAt": "..." }
 * - POST /products/{id}/reviews { rating, text } creates a review (requires Authorization header)
 * - PUT /reviews/{reviewId} { rating, text } edits a review
 * - DELETE /reviews/{reviewId} deletes a review
 *
 * Replace endpoints if your API differs.
 */
public class ProductDetailFrame extends JFrame {
    private final String productId;

    // UI
    private JLabel imageLabel = new JLabel();
    private JLabel titleLabel = new JLabel();
    private JLabel priceLabel = new JLabel();
    private JLabel originalPriceLabel = new JLabel();
    private JTextArea descArea = new JTextArea();
    private JLabel metaLabel = new JLabel();
    private JPanel featuresPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    private JPanel reviewsPanel = new JPanel();
    private JScrollPane reviewsScroll;
    private JSpinner ratingSpinner;
    private JTextArea reviewTextArea;
    private JButton postReviewBtn;


    public ProductDetailFrame(String productId) {
        super("Product details");
        this.productId = productId;
        initUI();
        fetchProductAndPopulate();
        fetchReviewsAndPopulate();
        setSize(860, 720);
        setLocationRelativeTo(null);
    }

    private void initUI() {
        setLayout(new BorderLayout(8, 8));

        // Top: title + image + prices
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(BorderFactory.createEmptyBorder(8,8,0,8));
        JPanel left = new JPanel(new BorderLayout());
        left.setPreferredSize(new Dimension(320, 320));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        left.add(imageLabel, BorderLayout.CENTER);
        top.add(left, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        priceLabel.setForeground(new Color(0, 128, 0));
        originalPriceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        originalPriceLabel.setForeground(Color.GRAY);
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setOpaque(false);

        right.add(titleLabel);
        right.add(Box.createRigidArea(new Dimension(0,6)));
        right.add(priceLabel);
        right.add(Box.createRigidArea(new Dimension(0,4)));
        right.add(originalPriceLabel);
        right.add(Box.createRigidArea(new Dimension(0,8)));
        right.add(new JScrollPane(descArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {{
            setPreferredSize(new Dimension(400, 140));
            setBorder(null);
            setOpaque(false);
            getViewport().setOpaque(false);
        }});
        right.add(Box.createRigidArea(new Dimension(0,8)));
        metaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        right.add(metaLabel);
        right.add(Box.createRigidArea(new Dimension(0,8)));
        right.add(new JLabel("Features:"));
        right.add(featuresPanel);

        top.add(right, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // Center: reviews list
        reviewsPanel.setLayout(new BoxLayout(reviewsPanel, BoxLayout.Y_AXIS));
        reviewsScroll = new JScrollPane(reviewsPanel);
        reviewsScroll.setBorder(BorderFactory.createTitledBorder("Reviews"));
        add(reviewsScroll, BorderLayout.CENTER);
        reviewsScroll.getVerticalScrollBar().setUnitIncrement(16);
        reviewsScroll.setPreferredSize(new Dimension(0, 400)); // or 350/400 if you want more space


        // South: review composer
        JPanel composer = new JPanel(new BorderLayout(8,8));
        composer.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        JPanel composerTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        composerTop.add(new JLabel("Your rating:"));
        ratingSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.5, 5.0, 0.5));
        composerTop.add(ratingSpinner);
        composer.add(composerTop, BorderLayout.NORTH);

        reviewTextArea = new JTextArea(4, 40);
        reviewTextArea.setLineWrap(true);
        reviewTextArea.setWrapStyleWord(true);
        composer.add(new JScrollPane(reviewTextArea), BorderLayout.CENTER);

        postReviewBtn = new JButton("Post Review");
        JPanel rightBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightBtn.add(postReviewBtn);
        composer.add(rightBtn, BorderLayout.SOUTH);

        add(composer, BorderLayout.SOUTH);

        // actions
        postReviewBtn.addActionListener(e -> postReviewAsync());

        // close behaviour
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    // ---------- Networking and UI population ----------

    private void fetchProductAndPopulate() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/products/" + productId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept","application/json");
                int rc = conn.getResponseCode();
                if (rc >= 200 && rc < 300) {
                    String body = readStream(conn.getInputStream());
                    JSONObject prod = new JSONObject(body);

                    String name = prod.optString("name", "Unnamed");
                    double price = prod.optDouble("price", 0.0);
                    double originalPrice = prod.optDouble("originalPrice", prod.optDouble("original_price", 0.0));
                    String description = prod.optString("description", "");
                    String category = prod.optString("category", "");
                    String image = prod.optString("image", "");
                    double rating = prod.optDouble("rating", 0.0);
                    int reviewCount = prod.optInt("reviewCount", prod.optInt("review_count", 0));
                    boolean inStock = prod.optBoolean("inStock", prod.optBoolean("in_stock", true));
                    JSONArray features = prod.optJSONArray("features");
                    long vendorId = prod.optLong("vendorId", prod.optLong("vendor_id", -1));

                    SwingUtilities.invokeLater(() -> {
                        titleLabel.setText(name);
                        priceLabel.setText(String.format("$ %.2f", price));
                        if (originalPrice > 0 && originalPrice > price) {
                            originalPriceLabel.setText(String.format("<html><strike>$ %.2f</strike></html>", originalPrice));
                        } else {
                            originalPriceLabel.setText("");
                        }
                        descArea.setText(description);
                        metaLabel.setText(String.format("Category: %s   Rating: %.1f (%d)   %s", category, rating, reviewCount, inStock ? "In stock" : "Out of stock"));

                        featuresPanel.removeAll();
                        if (features != null) {
                            for (int i = 0; i < features.length(); i++) {
                                String f = features.optString(i, null);
                                if (f != null && !f.isEmpty()) {
                                    JLabel fLabel = new JLabel("• " + f);
                                    fLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                                    featuresPanel.add(fLabel);
                                }
                            }
                        }
                        featuresPanel.revalidate();
                        featuresPanel.repaint();
                    });

                    // load image (off-EDT)
                    if (image != null && !image.isEmpty()) {
                        try {
                            Image img;
                            if (image.startsWith("http")) {
                                img = ImageIO.read(new URL(image));
                            } else {
                                // try local resource, else server static path
                                InputStream is = getClass().getResourceAsStream("/images/" + image);
                                if (is != null) img = ImageIO.read(is);
                                else img = ImageIO.read(new URL("http://localhost:8080/uploads/" + image));
                            }
                            if (img != null) {
                                Image scaled = img.getScaledInstance(300, 300, Image.SCALE_SMOOTH);
                                SwingUtilities.invokeLater(() -> imageLabel.setIcon(new ImageIcon(scaled)));
                            }
                        } catch (Exception ignored) {}
                    }

                } else {
                    String err = readStream(conn.getErrorStream());
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to load product: " + rc + "\n" + err));
                }
                conn.disconnect();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error loading product: " + ex.getMessage()));
            }
        }).start();
    }

    private void fetchReviewsAndPopulate() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/reviews/products/" + productId );
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept","application/json");
                int rc = conn.getResponseCode();
                if (rc >= 200 && rc < 300) {
                    String body = readStream(conn.getInputStream());
                    JSONArray arr = new JSONArray(body);

                    SwingUtilities.invokeLater(() -> {
                        reviewsPanel.removeAll();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject r = arr.getJSONObject(i);
                            JPanel rRow = makeReviewRow(r);
                            reviewsPanel.add(rRow);
                            reviewsPanel.add(Box.createRigidArea(new Dimension(0,8)));
                        }
                        reviewsPanel.revalidate();
                        reviewsPanel.repaint();
                    });
                } else {
                    // maybe none
                    SwingUtilities.invokeLater(() -> {
                        reviewsPanel.removeAll();
                        reviewsPanel.add(new JLabel("No reviews yet."));
                        reviewsPanel.revalidate();
                        reviewsPanel.repaint();
                    });
                }
                conn.disconnect();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    reviewsPanel.removeAll();
                    reviewsPanel.add(new JLabel("Error loading reviews: " + ex.getMessage()));
                    reviewsPanel.revalidate();
                    reviewsPanel.repaint();
                });
            }
        }).start();
    }

    private JPanel makeReviewRow(JSONObject r) {
        JPanel p = new JPanel(new BorderLayout(8, 4));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(6,6,6,6)
        ));
        p.setBackground(Color.WHITE);
        
        String userName = r.optString(r.getLong("userId")+"", r.optString("user", "User"));
        double rating = r.optDouble("rating",  r.optDouble("rating", 0.0));
        String text = r.optString("review", r.optString("comment", ""));
        long reviewId = r.optLong("id", -1);
        long userId = r.optLong("userId", -1);

        JLabel head = new JLabel(userName + " — " + rating + "★");
        head.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JTextArea body = new JTextArea(text);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setEditable(false);
        body.setBackground(Color.WHITE);
        body.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        body.setRows(Math.min(3, text.split("\\s+").length / 10 + 1));
        body.setBackground(Color.WHITE);
        body.setBorder(null);


        p.add(head, BorderLayout.NORTH);
        body.setPreferredSize(new Dimension(0,50));
        p.add(body,BorderLayout.CENTER);
        JSONArray ours=new JSONArray();
        try{
            URL url=new URL("http://localhost:8080/reviews/user");
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer "+AuthManager.Token);
            conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
            int rc=conn.getResponseCode();
            if(!(rc>=200 && rc<300)){
                System.out.println("Error:"+rc);
            }
            else{
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                
                ours=new JSONArray(response.toString());
            }
        }catch(Exception e){}
        for (int i=0;i<ours.length();i++){
            JSONObject rev=ours.getJSONObject(i);
            if(rev.getLong("userId")==userId){
                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,2));
                JButton editBtn = new JButton("Edit");
                JButton delBtn = new JButton("Delete");
                actions.add(editBtn);
                actions.add(delBtn);
                p.add(actions, BorderLayout.SOUTH);

                editBtn.addActionListener(e -> showEditDialogAndUpdate(reviewId, rating, text));
                delBtn.addActionListener(e -> {
                    int conf = JOptionPane.showConfirmDialog(this, "Delete your review?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (conf == JOptionPane.YES_OPTION) {
                        deleteReviewAsync(reviewId);
                    }
                });
            }
        }

        return p;
    }

    // show a small dialog to edit review, then call PUT
    private void showEditDialogAndUpdate(long reviewId, double currentRating, String currentText) {
        JPanel panel = new JPanel(new BorderLayout(6,6));
        JSpinner sp = new JSpinner(new SpinnerNumberModel(currentRating, 0.5, 5.0, 0.5));
        JTextArea ta = new JTextArea(currentText, 6, 40);
        panel.add(new JLabel("Rating:"), BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);
        panel.add(new JScrollPane(ta), BorderLayout.SOUTH);

        int ok = JOptionPane.showConfirmDialog(this, panel, "Edit review", JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) {
            double newRating = ((Number) sp.getValue()).doubleValue();
            String newText = ta.getText().trim();
            editReviewAsync(reviewId, newRating, newText);
        }
    }

    // POST review
    private void postReviewAsync() {
        double rating = ((Number) ratingSpinner.getValue()).doubleValue();
        String text = reviewTextArea.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type a review text.");
            return;
        }
        postReviewBtn.setEnabled(false);

        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/reviews/products/" + productId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty()){
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("Refresh-Token",AuthManager.Refresh);
                }
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("rating", rating);
                body.put("review", text);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("utf-8"));
                }

                int rc = conn.getResponseCode();
                String resp = rc>=200 && rc<300 ? readStream(conn.getInputStream()) : readStream(conn.getErrorStream());
                conn.disconnect();

                if (rc >= 200 && rc < 300) {
                    SwingUtilities.invokeLater(() -> {
                        reviewTextArea.setText("");
                        ratingSpinner.setValue(5.0);
                        fetchReviewsAndPopulate();
                        JOptionPane.showMessageDialog(this, "Review posted.");
                        postReviewBtn.setEnabled(true);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Failed to post review: " + rc + "\n" + resp);
                        postReviewBtn.setEnabled(true);
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Error posting review: " + ex.getMessage());
                    postReviewBtn.setEnabled(true);
                });
            }
        }).start();
    }

    // PUT edit
    private void editReviewAsync(long reviewId, double rating, String text) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/reviews/products/" + productId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty()){
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("Refresh-Token",AuthManager.Refresh);
                }
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("rating", rating);
                body.put("review", text);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("utf-8"));
                }

                int rc = conn.getResponseCode();
                String resp = rc>=200 && rc<300 ? readStream(conn.getInputStream()) : readStream(conn.getErrorStream());
                conn.disconnect();

                SwingUtilities.invokeLater(() -> {
                    if (rc >= 200 && rc < 300) {
                        fetchReviewsAndPopulate();
                        JOptionPane.showMessageDialog(this, "Review updated.");
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to update: " + rc + "\n" + resp);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error updating review: " + ex.getMessage()));
            }
        }).start();
    }

    // DELETE review
    private void deleteReviewAsync(long reviewId) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/reviews/products/" + productId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                }
                int rc = conn.getResponseCode();
                String resp = rc>=200 && rc<300 ? readStream(conn.getInputStream()) : readStream(conn.getErrorStream());
                conn.disconnect();

                SwingUtilities.invokeLater(() -> {
                    if (rc >= 200 && rc < 300) {
                        fetchReviewsAndPopulate();
                        JOptionPane.showMessageDialog(this, "Review deleted.");
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to delete: " + rc + "\n" + resp);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error deleting review: " + ex.getMessage()));
            }
        }).start();
    }

    private static String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
