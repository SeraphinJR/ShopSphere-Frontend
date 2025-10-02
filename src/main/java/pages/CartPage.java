package pages;

import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.json.*;

/**
 * CartPage - displays user's cart, allows quantity changes and checkout.
 */
public class CartPage extends javax.swing.JFrame {

    // UI components (declared as fields for NetBeans-style)
    private javax.swing.JPanel topPanel;
    private javax.swing.JButton homeButton;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JScrollPane itemsScrollPane;
    private javax.swing.JPanel itemsPanel;              // panel that holds item cards
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JLabel totalLabel;
    private javax.swing.JButton checkoutButton;

    // Internal model: store items as JSONObject entries returned by backend
    // each item expected to have at least: productId (int or string), name, price (number), image (string), quantity (int)
    private java.util.List<JSONObject> cartItems = new ArrayList<>();

    public CartPage() {
        initComponents();

        setSize(900, 600);
        setLocationRelativeTo(null);

        // speed up scroll
        itemsScrollPane.getVerticalScrollBar().setUnitIncrement(24);

        // load cart data
        loadCartFromServer();

        setVisible(true);
    }

    // ------------------- Network / UI logic -------------------

    private void loadCartFromServer() {
        // run networking off EDT
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/cart"); // adapt if needed
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                String token = AuthManager.Token; // assume AuthManager exists
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int rc = conn.getResponseCode();
                InputStream is = (rc >= 200 && rc < 300) ? conn.getInputStream() : conn.getErrorStream();
                String resp = readStream(is);
                conn.disconnect();

                if (rc >= 200 && rc < 300) {
                    // Expecting either JSON array or object that contains array
                    JSONArray itemsArray;
                    resp = resp.trim();
                    if (resp.startsWith("[")) {
                        itemsArray = new JSONArray(resp);
                    } else {
                        // maybe backend wraps it in { items: [...] }
                        JSONObject wrap = new JSONObject(resp);
                        if (wrap.has("items") && wrap.get("items") instanceof JSONArray) {
                            itemsArray = wrap.getJSONArray("items");
                        } else {
                            // fallback: empty
                            itemsArray = new JSONArray();
                        }
                    }

                    cartItems.clear();
                    for (int i = 0; i < itemsArray.length(); i++) {
                        cartItems.add(itemsArray.getJSONObject(i));
                    }

                    SwingUtilities.invokeLater(() -> {
                        rebuildItemsUI();
                        recalcTotal();
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(CartPage.this, "Failed to load cart: " + rc + "\n" );
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(CartPage.this, "Error loading cart: " + ex.getMessage()));
            }
        }).start();
    }

    // Read whole input stream into a String
    private static String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // Rebuild the itemsPanel from cartItems
    private void rebuildItemsUI() {
        itemsPanel.removeAll();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));

        for (JSONObject item : cartItems) {
            JPanel card = makeCartItemCard(item);
            itemsPanel.add(card);
            itemsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    // Create UI card for a single cart item JSONObject
    private JPanel makeCartItemCard(JSONObject item) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8,8,8,8)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.setBackground(Color.WHITE);

        // Left: image
        JLabel imgLabel = new JLabel();
        imgLabel.setPreferredSize(new Dimension(120, 90));
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setVerticalAlignment(SwingConstants.CENTER);
        String imageField = item.optString("image", "");
        // try HTTP first, then resource fallback
        if (imageField != null && !imageField.isEmpty()) {
            new Thread(() -> {
                try {
                    Image img = null;
                    if (imageField.startsWith("http")) {
                        img = ImageIO.read(new URL(imageField));
                    } else {
                        // try resources path (if you bundle) OR server static path
                        try {
                            img = ImageIO.read(getClass().getResourceAsStream("/images/" + imageField));
                        } catch (Throwable t) {
                            // fallback to server static path
                            img = ImageIO.read(new URL("http://localhost:8080/images/" + imageField));
                        }
                    }
                    if (img != null) {
                        Image scaled = img.getScaledInstance(110, 90, Image.SCALE_SMOOTH);
                        SwingUtilities.invokeLater(() -> imgLabel.setIcon(new ImageIcon(scaled)));
                    }
                } catch (Exception ex) {
                    // ignore image loading issues; show placeholder text
                    SwingUtilities.invokeLater(() -> imgLabel.setText("[no image]"));
                }
            }).start();
        } else {
            imgLabel.setText("[no image]");
        }

        card.add(imgLabel, BorderLayout.WEST);

        // Center: name + unit price + small desc
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        JSONObject prod=null;
        try{
            URL url=new URL("http://localhost:8080/products/"+item.get("productId").toString());
            HttpURLConnection conn= (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept","application/json");
            conn.setRequestProperty("Authorization", "Bearer "+AuthManager.Token);
            
            int rc=conn.getResponseCode();
            if (rc>=200&&rc<300){
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder resp=new StringBuilder();
                String line;
                while((line=br.readLine())!=null){resp.append(line);}
                
                br.close();
                
                prod=new JSONObject(resp.toString());
                
            
            }
            
        }catch(Exception e){
        System.out.print("Error:"+e);}

        String name = prod.getString("name");
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        double unitPrice = prod.getDouble("price");
        JLabel priceLabel = new JLabel(String.format("Unit price: $ %.2f", unitPrice)); // currency symbol optional

        center.add(nameLabel, BorderLayout.NORTH);
        center.add(priceLabel, BorderLayout.CENTER);

        card.add(center, BorderLayout.CENTER);

        // Right: quantity controls + line total + remove
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);

        // quantity panel (label + spinner)
        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        qtyPanel.setOpaque(false);
        qtyPanel.add(new JLabel("Qty:"));

        int qty = item.optInt("quantity", 1);
        SpinnerNumberModel model = new SpinnerNumberModel(qty, 1, 1000, 1);
        JSpinner spinner = new JSpinner(model);
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(2);
        qtyPanel.add(spinner);

        // line total label
        double lineTotal = unitPrice * qty;
        JLabel lineTotalLabel = new JLabel(String.format("$ %.2f", lineTotal));
        lineTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // remove button
        JButton removeBtn = new JButton("Remove");
        removeBtn.setMargin(new Insets(3,8,3,8));

        // spinner change -> call update API
        spinner.addChangeListener(evt -> {
            int newQty = (Integer) spinner.getValue();
            // update local UI immediate
            lineTotalLabel.setText(String.format("$ %.2f", unitPrice * newQty));
            recalcTotal();

            // call backend to update
            updateCartQuantityAsync(item, newQty);
        });

        // remove button action
        removeBtn.addActionListener(ae -> {
            int confirm = JOptionPane.showConfirmDialog(CartPage.this, "Remove " + name + " from cart?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                removeCartItemAsync(item);
            }
        });

        right.add(qtyPanel);
        right.add(Box.createVerticalStrut(6));
        right.add(lineTotalLabel);
        right.add(Box.createVerticalStrut(8));
        right.add(removeBtn);
        right.setAlignmentY(Component.TOP_ALIGNMENT);

        card.add(right, BorderLayout.EAST);

        return card;
    }

    // Recalculate total from current cartItems and update totalLabel
    private void recalcTotal() {
        double tot = 0.0;
        for (JSONObject item : cartItems) {
            JSONObject prod=null;
            try{
                URL url=new URL("http://localhost:8080/products/"+item.get("productId").toString());
                HttpURLConnection conn= (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept","application/json");
                conn.setRequestProperty("Authorization", "Bearer "+AuthManager.Token);

                int rc=conn.getResponseCode();
                if (rc>=200&&rc<300){
                    BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder resp=new StringBuilder();
                    String line;
                    while((line=br.readLine())!=null){resp.append(line);}

                    br.close();

                    prod=new JSONObject(resp.toString());


                }

            }catch(Exception e){
            System.out.print("Error:"+e);}
            double unitPrice = prod.getDouble("price");
            int qty = item.getInt("quantity");
            tot += unitPrice * qty;
        }
        totalLabel.setText(String.format("Total: $ %.2f", tot));
    }

    // Async update quantity to backend
    private void updateCartQuantityAsync(JSONObject item, int newQty) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/cart"); // adapt endpoint
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty()) conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                // include identifiers your backend expects:
                // here we prefer productId if present
                if (item.has("productId")) body.put("productId", item.get("productId"));
                else if (item.has("id")) body.put("productId", item.get("id"));
                body.put("quantity", newQty);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("utf-8"));
                }

                int rc = conn.getResponseCode();
                String resp = readStream((rc >= 200 && rc < 300) ? conn.getInputStream() : conn.getErrorStream());
                conn.disconnect();

                if (rc >= 200 && rc < 300) {
                    // update local model quantity
                    item.put("quantity", newQty);
                    SwingUtilities.invokeLater(() -> {
                        recalcTotal();
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(CartPage.this, "Failed to update quantity: " + resp);
                        // Optionally reload cart from server to sync state
                        loadCartFromServer();
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(CartPage.this, "Error updating quantity: " + ex.getMessage()));
                // reload to ensure consistency
                loadCartFromServer();
            }
        }).start();
    }

    // Async remove item from cart
    private void removeCartItemAsync(JSONObject item) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/cart/"+item.getInt("productId")); // adapt endpoint
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Accept", "application/json");
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty()) conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                if (item.has("productId")) body.put("productId", item.get("productId"));
                else if (item.has("id")) body.put("productId", item.get("id"));

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("utf-8"));
                }

                int rc = conn.getResponseCode();
                String resp = readStream((rc >= 200 && rc < 300) ? conn.getInputStream() : conn.getErrorStream());
                conn.disconnect();

                if (rc >= 200 && rc < 300) {
                    // remove locally and refresh UI
                    cartItems.remove(item);
                    SwingUtilities.invokeLater(() -> {
                        rebuildItemsUI();
                        recalcTotal();
                    });
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(CartPage.this, "Failed to remove item: " + rc));
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(CartPage.this, "Error removing item: " + ex.getMessage()));
            }
        }).start();
    }

    // Async checkout
    private void checkoutAsync() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/cart/checkout"); // adapt endpoint
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty()) conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                // Could pass cart summary if needed; many servers use token to identify cart
                JSONObject body = new JSONObject();

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("utf-8"));
                }

                int rc = conn.getResponseCode();
                String resp = readStream((rc >= 200 && rc < 300) ? conn.getInputStream() : conn.getErrorStream());
                conn.disconnect();

                if (rc >= 200 && rc < 300) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(CartPage.this, "Checkout successful!");
                        // optionally clear UI and reload cart
                        cartItems.clear();
                        rebuildItemsUI();
                        recalcTotal();
                    });
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(CartPage.this, "Checkout failed: " + resp));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(CartPage.this, "Error during checkout: " + ex.getMessage()));
            }
        }).start();
    }

    // ------------------- GUI builder style initComponents -------------------
    @SuppressWarnings("unchecked")
    private void initComponents() {

        topPanel = new javax.swing.JPanel();
        homeButton = new javax.swing.JButton();
        titleLabel = new javax.swing.JLabel();
        itemsScrollPane = new javax.swing.JScrollPane();
        itemsPanel = new javax.swing.JPanel();
        bottomPanel = new javax.swing.JPanel();
        totalLabel = new javax.swing.JLabel();
        checkoutButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("ShopSphere - Cart");
        setPreferredSize(new java.awt.Dimension(900, 600));
        setBackground(new java.awt.Color(250, 250, 250));

        // topPanel
        topPanel.setBackground(new java.awt.Color(245, 245, 245));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        homeButton.setText("Home");
        homeButton.setFont(new java.awt.Font("Segoe UI", 0, 14));
        homeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                // open HomePage and dispose this
                new HomePage();
                CartPage.this.dispose();
            }
        });

        titleLabel.setFont(new java.awt.Font("Segoe UI", 1, 18));
        titleLabel.setText("Your Cart");

        javax.swing.GroupLayout topPanelLayout = new javax.swing.GroupLayout(topPanel);
        topPanel.setLayout(topPanelLayout);
        topPanelLayout.setHorizontalGroup(
            topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(topPanelLayout.createSequentialGroup()
                    .addComponent(homeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(12, 12, 12)
                    .addComponent(titleLabel)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        topPanelLayout.setVerticalGroup(
            topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(topPanelLayout.createSequentialGroup()
                    .addGroup(topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(homeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(titleLabel))
                    .addGap(0, 6, Short.MAX_VALUE))
        );

        // itemsPanel (inside scroll pane)
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBackground(Color.WHITE);
        itemsPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        itemsScrollPane.setViewportView(itemsPanel);

        // bottomPanel: total + checkout
        bottomPanel.setBackground(new java.awt.Color(245, 245, 245));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        totalLabel.setFont(new java.awt.Font("Segoe UI", 1, 16));
        totalLabel.setText("Total: $ 0.00");

        checkoutButton.setText("Checkout");
        checkoutButton.setFont(new java.awt.Font("Segoe UI", 0, 14));
        checkoutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                // confirm then call checkout
                int confirm = JOptionPane.showConfirmDialog(CartPage.this, "Proceed to checkout?", "Checkout", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    
                }
            }
        });

        javax.swing.GroupLayout bottomLayout = new javax.swing.GroupLayout(bottomPanel);
        bottomPanel.setLayout(bottomLayout);
        bottomLayout.setHorizontalGroup(
            bottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bottomLayout.createSequentialGroup()
                    .addComponent(totalLabel)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 300, Short.MAX_VALUE)
                    .addComponent(checkoutButton, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        bottomLayout.setVerticalGroup(
            bottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bottomLayout.createSequentialGroup()
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(bottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(totalLabel)
                        .addComponent(checkoutButton, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap())
        );

        // Main frame layout
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(topPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(itemsScrollPane)
                .addComponent(bottomPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(topPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(itemsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(bottomPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }
    
    public static void main(String[] args) {
        // Always start Swing apps on the Event Dispatch Thread
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Start with HomePage
                new CartPage();
                // Or, for testing cart directly:
                // new CartPage();
            }
        });
    }

 
    // ------------------- End of class -------------------
}
