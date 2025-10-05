package pages;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import model.CartModel;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/**
 * BillingPanel - shows line items, total, QR and handles order submission.
 *
 * Usage: new BillingPanel(parentMainFrame, cartModel)
 */
public class BillingPanel extends JPanel {
    private final MainFrame parent;
    private final CartModel cartModel;

    private JPanel itemsPanel;
    private JLabel totalLabel;
    private JButton proceedButton;
    private JPanel qrPanel;
    private JButton paymentCompletedBtn;

    // Local state for payment flow
    private Long currentOrderId = null;
    private String currentPaymentToken = null;

    public BillingPanel(MainFrame parent, CartModel cartModel) {
        this.parent = parent;
        this.cartModel = cartModel;
        initComponents();

        // Listen for cart changes to refresh billing view
        cartModel.addChangeListener(v -> SwingUtilities.invokeLater(() -> {
            rebuildItemsList();
            recalcTotalAsync();
        }));

        // initial build
        rebuildItemsList();
        recalcTotalAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Top bar
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(245,245,245));
        JButton back = new JButton("Back to Cart");
        back.addActionListener(e -> parent.showPage("CART"));
        JLabel title = new JLabel("Checkout / Billing", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        top.add(back, BorderLayout.WEST);
        top.add(title, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // center: items list
        itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(itemsPanel);
        sp.getVerticalScrollBar().setUnitIncrement(20);
        add(sp, BorderLayout.CENTER);

        // bottom: total + buttons + qr
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        totalLabel = new JLabel("Total: $ 0.00");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        left.add(totalLabel);
        bottom.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        proceedButton = new JButton("Proceed to Pay");
        paymentCompletedBtn = new JButton("Payment Completed");
        paymentCompletedBtn.setEnabled(false); // enabled only after showing QR
        right.add(proceedButton);
        right.add(paymentCompletedBtn);
        bottom.add(right, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        // qr panel overlay (initially hidden)
        qrPanel = new JPanel(new BorderLayout());
        qrPanel.setBackground(Color.WHITE);
        qrPanel.setBorder(BorderFactory.createTitledBorder("Scan QR to pay"));
        qrPanel.setPreferredSize(new Dimension(260, 260));
        qrPanel.setVisible(false);
        add(qrPanel, BorderLayout.EAST);

        // actions
        proceedButton.addActionListener(e -> {
            if (currentOrderId != null) {
                // use the existing order created by CartPage
                proceedToPayForExistingOrderAsync();
            } else {
                // user somehow navigated to Billing without an order
                JOptionPane.showMessageDialog(BillingPanel.this,
                    "No order found. Please press Checkout in the Cart first.",
                    "No order", JOptionPane.WARNING_MESSAGE);
            }
        });

        paymentCompletedBtn.addActionListener(e -> confirmPaymentAsync());
    }

    private void rebuildItemsList() {
        itemsPanel.removeAll();
        List<JSONObject> itemsSnapshot;
        synchronized (cartModel) {
            itemsSnapshot = List.copyOf(cartModel.getItems()); // snapshot to iterate safely
        }

        if (itemsSnapshot.isEmpty()) {
            JLabel empty = new JLabel("Your cart is empty.");
            empty.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
            itemsPanel.add(empty);
        } else {
            for (JSONObject item : itemsSnapshot) {
                JPanel row = makeBillingRow(item);
                itemsPanel.add(row);
                itemsPanel.add(Box.createRigidArea(new Dimension(0,8)));
            }
        }
        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    private JPanel makeBillingRow(JSONObject item) {
        int screenWidth = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
        int rowHeight = Math.max(72, (int)(screenWidth * 0.060));

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowHeight));
        row.setPreferredSize(new Dimension(0, rowHeight));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        int imgSize = Math.max(48, rowHeight - 16);
        JPanel imgPanel = new JPanel(new BorderLayout());
        imgPanel.setPreferredSize(new Dimension(imgSize, imgSize));
        imgPanel.setMinimumSize(new Dimension(imgSize, imgSize));
        imgPanel.setBackground(new Color(230, 230, 230));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);

        String pid = item.optString("productId", item.optString("id", "n/a"));
        int qty = item.optInt("quantity", 1);

        JLabel nameLabel = new JLabel("Product ID: " + pid);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, Math.max(12, rowHeight / 6)));

        JLabel qtyLabel = new JLabel("Qty: " + qty);
        qtyLabel.setFont(new Font("Segoe UI", Font.PLAIN, Math.max(11, rowHeight / 7)));
        qtyLabel.setForeground(Color.DARK_GRAY);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, Math.max(4, rowHeight/18))));
        infoPanel.add(qtyLabel);

        JLabel priceLabel = new JLabel("Price: fetching...");
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, Math.max(12, rowHeight / 7)));
        JPanel priceWrapper = new JPanel(new BorderLayout());
        priceWrapper.setOpaque(false);
        priceWrapper.add(priceLabel, BorderLayout.SOUTH);

        row.add(imgPanel, BorderLayout.WEST);
        row.add(infoPanel, BorderLayout.CENTER);
        row.add(priceWrapper, BorderLayout.EAST);

        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/products/" + pid);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                int rc = conn.getResponseCode();
                if (rc >= 200 && rc < 300) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    JSONObject prod = new JSONObject(sb.toString());
                    String name = prod.optString("name", "Product " + pid);
                    JLabel imgLabel = new JLabel("Img", SwingConstants.CENTER);
                    String imgField = prod.optString("image", null);
                    loadProductImage(imgLabel, imgField, imgSize, imgSize);
                    imgLabel.setFont(new Font("Segoe UI", Font.BOLD, Math.max(10, imgSize / 6)));
                    SwingUtilities.invokeLater(() -> imgPanel.add(imgLabel, BorderLayout.CENTER));
                    double price = prod.optDouble("price", 0.0);
                    SwingUtilities.invokeLater(() -> {
                        nameLabel.setText(name);
                        priceLabel.setText(String.format("$ %.2f each  —  Total: $ %.2f", price, price * qty));
                    });
                } else {
                    SwingUtilities.invokeLater(() -> priceLabel.setText("Price: N/A"));
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> priceLabel.setText("Price: error"));
            }
        }).start();

        return row;
    }

    /** Compute total by looping cart items and fetching each product price (async). */
    private void recalcTotalAsync() {
        new Thread(() -> {
            double tot = 0.0;
            List<JSONObject> snapshot;
            synchronized (cartModel) { snapshot = List.copyOf(cartModel.getItems()); }

            for (JSONObject item : snapshot) {
                String pid = item.optString("productId", item.optString("id", null));
                int qty = item.optInt("quantity", 1);
                if (pid == null) continue;
                try {
                    URL url = new URL("http://localhost:8080/products/" + pid);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    int rc = conn.getResponseCode();
                    if (rc >= 200 && rc < 300) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();
                        JSONObject prod = new JSONObject(sb.toString());
                        double price = prod.optDouble("price", 0.0);
                        tot += price * qty;
                    }
                } catch (Exception ignored) {}
            }
            final double totalFinal = tot;
            SwingUtilities.invokeLater(() -> totalLabel.setText(String.format("Total: $ %.2f", totalFinal)));
        }).start();
    }

    /** Simulates showing a QR — here we create a simple placeholder BufferedImage. */
    private void showQr(String token) {
        // create a simple QR placeholder image that also shows a short token snippet
        BufferedImage img = new BufferedImage(260, 260, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0,0,260,260);
        g.setColor(Color.BLACK);
        g.drawRect(0,0,259,259);
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        g.drawString("QR", 110, 130);
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        String snippet = token == null ? "" : (token.length() > 32 ? token.substring(0, 32) + "…" : token);
        g.drawString(snippet, 8, 248);
        g.dispose();

        qrPanel.removeAll();
        JLabel pic = new JLabel(new ImageIcon(img));
        qrPanel.add(pic, BorderLayout.CENTER);
        qrPanel.setVisible(true);
        paymentCompletedBtn.setEnabled(true);
        revalidate();
        repaint();
    }

    private void clearCart(){
        try{
            URL url=new URL("http://localhost:8080/cart");
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("content-type","application/json");
            conn.setRequestProperty("Authorization", "Bearer "+AuthManager.Token);
            conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
            conn.setDoOutput(true);

            int rc=conn.getResponseCode();
            if (rc>=200&&rc<300){
                System.out.print("Cleared");
            }
            else{
                System.out.print("Error:"+rc);
            }

            conn.disconnect();
        }catch(Exception e){}
    }

    private void loadProductImage(JLabel imgLabel, String imageField, int width, int height) {
        if (imageField == null || imageField.isEmpty()) {
            imgLabel.setText("[no image]");
            return;
        }

        try {
            String encoded = URLEncoder.encode(imageField, StandardCharsets.UTF_8);
            String imageUrl = "http://localhost:8080/uploads/" + encoded;

            imgLabel.setText("[loading...]");

            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int rc = conn.getResponseCode();
            if (rc >= 200 && rc < 300) {
                Image img = javax.imageio.ImageIO.read(conn.getInputStream());
                if (img != null) {
                    Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    SwingUtilities.invokeLater(() -> {
                        imgLabel.setText("");
                        imgLabel.setIcon(new ImageIcon(scaled));
                    });
                } else {
                    SwingUtilities.invokeLater(() -> imgLabel.setText("[no image]"));
                }
            } else {
                SwingUtilities.invokeLater(() -> imgLabel.setText("[no image]"));
            }

            conn.disconnect();
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> imgLabel.setText("[no image]"));
        }
    }

    public void reset() {
        // Hide QR
        qrPanel.setVisible(false);
        paymentCompletedBtn.setEnabled(false);
        proceedButton.setEnabled(true);

        // Rebuild items list from current cart
        rebuildItemsList();
        recalcTotalAsync();

        // clear flow state
        System.out.println("Resetting BillingPanel, currentOrderId=" + currentOrderId);

        if (currentOrderId == null) {
            currentPaymentToken = null;
        }
    }

    /**
     * Start of flow: create order (server will mark it PENDING),
     * then request payment token (server will mark it PAYMENT_PENDING).
     * After obtaining token we show QR and enable "Payment Completed" button.
     */
    
    public void setExistingOrderId(Long orderId) {
        this.currentOrderId = orderId;
        System.out.println("BillingPanel: orderId set to " + orderId);
        SwingUtilities.invokeLater(() -> proceedButton.setEnabled(orderId!=null));
    }   
    
    
    private void proceedToPayForExistingOrderAsync() {
        proceedButton.setEnabled(false);
        paymentCompletedBtn.setEnabled(false);
        qrPanel.setVisible(false);

        new Thread(() -> {
            try {
                Long orderId = currentOrderId;
                if (orderId == null) return;
                System.out.println("OrderId=" + currentOrderId + ", Auth Token=" + AuthManager.Token);
                URL payUrl = new URL("http://localhost:8080/payment/pay/" + orderId);
                HttpURLConnection payConn = (HttpURLConnection) payUrl.openConnection();
                payConn.setRequestMethod("POST");
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty()) payConn.setRequestProperty("Authorization", "Bearer " + token);
                payConn.setRequestProperty("Refresh-Token",AuthManager.Refresh);
                payConn.setDoOutput(true);

                int prc = payConn.getResponseCode();
                String presp;
                try (InputStream is = (prc >= 200 && prc < 300) ? payConn.getInputStream() : payConn.getErrorStream()) {
                    presp = new BufferedReader(new InputStreamReader(is)).lines().reduce("", (a,b) -> a+b);
                }
                payConn.disconnect();

                if (!(prc >= 200 && prc < 300)) {
                    final String diag = "Failed to initiate payment: " + prc + "\n" + presp;
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(BillingPanel.this, diag);
                        proceedButton.setEnabled(true);
                    });
                    return;
                }

                // parse token
                String payToken = extractStringFromJson(presp, "paymentToken", "token", "payment_token");
                if (payToken == null || payToken.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(BillingPanel.this, "Payment initiated but no token returned.\nResponse: " + presp);
                        proceedButton.setEnabled(true);
                    });
                    return;
                }
                currentPaymentToken = payToken;

                SwingUtilities.invokeLater(() -> {
                    showQr(currentPaymentToken);
                    paymentCompletedBtn.setEnabled(true);
                    proceedButton.setEnabled(false);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(BillingPanel.this, "Error initiating payment: " + ex.getMessage());
                    proceedButton.setEnabled(true);
                });
            }
        }).start();
    }


    
    

    /** Confirm payment using the stored token; backend will set to PAYMENT_COMPLETED. */
    private void confirmPaymentAsync() {
        if (currentPaymentToken == null) {
            JOptionPane.showMessageDialog(this, "No payment token available. Please press Proceed to Pay first.");
            return;
        }
        paymentCompletedBtn.setEnabled(false);

        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/payment/confirm");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Payment-Token", currentPaymentToken);
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty()) conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                int rc = conn.getResponseCode();
                String resp;
                try (InputStream is = (rc >= 200 && rc < 300) ? conn.getInputStream() : conn.getErrorStream()) {
                    resp = new BufferedReader(new InputStreamReader(is)).lines().reduce("", (a,b) -> a + b);
                }
                conn.disconnect();

                if (rc >= 200 && rc < 300) {
                    // success: clear cart, local cart model, navigate home
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(BillingPanel.this, "Payment confirmed! Order completed.");
                        clearCart();
                        cartModel.clear();
                        // reset local state and UI
                        currentOrderId = null;
                        currentPaymentToken = null;
                        qrPanel.setVisible(false);
                        paymentCompletedBtn.setEnabled(false);
                        proceedButton.setEnabled(true);
                        parent.showPage("HOME");
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(BillingPanel.this, "Payment confirmation failed: " + rc + "\n" + resp);
                        paymentCompletedBtn.setEnabled(true);
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(BillingPanel.this, "Error confirming payment: " + ex.getMessage());
                    paymentCompletedBtn.setEnabled(true);
                });
            }
        }).start();
    }

    // helper: try to parse several possible numeric id field names
    private Long extractLongFromJson(String json, String... possibleNames) {
        try {
            if (json == null || json.trim().isEmpty()) return null;
            JSONObject o = new JSONObject(json);
            for (String n : possibleNames) {
                if (o.has(n) && !o.isNull(n)) {
                    try {
                        return o.getLong(n);
                    } catch (Exception ignored) {
                        try { return Long.valueOf(o.getString(n)); } catch (Exception ex) {}
                    }
                }
            }
            // maybe root object has "data" or similar
            if (o.has("data") && o.get("data") instanceof JSONObject) {
                JSONObject d = o.getJSONObject("data");
                for (String n : possibleNames) {
                    if (d.has(n) && !d.isNull(n)) {
                        try {
                            return d.getLong(n);
                        } catch (Exception ignored) {
                            try { return Long.valueOf(d.getString(n)); } catch (Exception ex) {}
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // helper: extract string token field
    private String extractStringFromJson(String json, String... possibleNames) {
        try {
            if (json == null || json.trim().isEmpty()) return null;
            JSONObject o = new JSONObject(json);
            for (String n : possibleNames) {
                if (o.has(n) && !o.isNull(n)) {
                    return o.getString(n);
                }
            }
            if (o.has("data") && o.get("data") instanceof JSONObject) {
                JSONObject d = o.getJSONObject("data");
                for (String n : possibleNames) {
                    if (d.has(n) && !d.isNull(n)) return d.getString(n);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
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
