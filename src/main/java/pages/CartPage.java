package pages;

import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.*;
import org.json.*;
import model.CartModel;

/**
 * CartPage - displays user's cart, allows quantity changes and checkout.
 */
public class CartPage extends JPanel {

    // UI components (declared as fields for NetBeans-style)
    private javax.swing.JPanel topPanel;
    private javax.swing.JButton homeButton;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JScrollPane itemsScrollPane;
    private javax.swing.JPanel itemsPanel; // panel that holds item cards
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JLabel totalLabel;
    private javax.swing.JButton checkoutButton;

    private MainFrame parent;
    private final CartModel cartModel;

    public CartPage(MainFrame parent, CartModel cartModel) {
        this.parent = parent;
        this.cartModel = cartModel;
        initComponents();
        try {
            Theme.styleComponentTree(this);
        } catch (Throwable ignored) {
        }

        cartModel.addChangeListener(v -> {
            rebuildItemsUI();
            recalcTotal();
        });

        setSize(900, 600);

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
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Accept", "application/json");
                if (AuthManager.Token != null && !AuthManager.Token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + AuthManager.Token);
                    headers.put("Refresh-Token", AuthManager.Refresh);
                }
                String resp = HttpUtil.getString("http://localhost:8080/cart", headers);
                // HttpUtil throws on network error; treat returned body as successful payload
                // Expecting either JSON array or object that contains array
                org.json.JSONArray itemsArray;
                resp = resp.trim();
                if (resp.startsWith("[")) {
                    itemsArray = new org.json.JSONArray(resp);
                } else {
                    org.json.JSONObject wrap = new org.json.JSONObject(resp);
                    if (wrap.has("items") && wrap.get("items") instanceof org.json.JSONArray) {
                        itemsArray = wrap.getJSONArray("items");
                    } else {
                        itemsArray = new org.json.JSONArray();
                    }
                }

                cartModel.clear();
                for (int i = 0; i < itemsArray.length(); i++) {
                    cartModel.addItem(itemsArray.getJSONObject(i));
                }

                SwingUtilities.invokeLater(() -> {
                    rebuildItemsUI();
                    recalcTotal();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(
                        () -> JOptionPane.showMessageDialog(CartPage.this, "Error loading cart: " + ex.getMessage()));
            }
        }).start();
    }

    // (removed unused helper readStream)

    // Rebuild the itemsPanel from cartModel
    private void rebuildItemsUI() {
        itemsPanel.removeAll();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));

        for (JSONObject item : cartModel.getItems()) {
            JPanel card = makeCartItemCard(item);
            itemsPanel.add(card);
            itemsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    // Helper to get product id as string
    private String getProductId(JSONObject item) {
        if (item == null)
            return null;
        if (item.has("productId"))
            return String.valueOf(item.get("productId"));
        if (item.has("id"))
            return String.valueOf(item.get("id"));
        return null;
    }

    // Create UI card for a single cart item JSONObject
    private JPanel makeCartItemCard(JSONObject item) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.setBackground(Color.BLACK);

        // Left: image
        JLabel imgLabel = new JLabel();
        imgLabel.setPreferredSize(new Dimension(120, 90));
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setVerticalAlignment(SwingConstants.CENTER);
        String imageField = item.has("image") ? item.optString("image", "") : "";
        if (imageField != null && !imageField.isEmpty()) {
            String imageUrl = imageField.startsWith("http") ? imageField
                    : "http://localhost:8080/uploads/" + imageField;
            new Thread(() -> {
                try {
                    byte[] imgBytes = HttpUtil.getBytes(imageUrl, null);
                    if (imgBytes != null && imgBytes.length > 0) {
                        java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imgBytes);
                        Image img = ImageIO.read(bis);
                        if (img != null) {
                            Image scaled = img.getScaledInstance(110, 90, Image.SCALE_SMOOTH);
                            SwingUtilities.invokeLater(() -> imgLabel.setIcon(new ImageIcon(scaled)));
                            return;
                        }
                    }
                } catch (Exception ex) {
                    // ignore image load failure
                }
            }).start();
        } else {
            imgLabel.setText("[no image]");
        }

        card.add(imgLabel, BorderLayout.WEST);

        // Center: name + price
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        String name = item.has("name") ? item.optString("name", "Loading...") : "Loading...";
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        double unitPrice = item.has("price") ? item.optDouble("price", 0.0) : 0.0;
        JLabel priceLabel = new JLabel(String.format("Unit price: $ %.2f", unitPrice));
        center.add(nameLabel, BorderLayout.NORTH);
        center.add(priceLabel, BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        // Right: quantity controls + line total + remove
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);

        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        qtyPanel.setOpaque(false);
        qtyPanel.add(new JLabel("Qty:"));
        int qty = item.optInt("quantity", 1);
        SpinnerNumberModel model = new SpinnerNumberModel(qty, 1, 1000, 1);
        JSpinner spinner = new JSpinner(model);
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(2);
        qtyPanel.add(spinner);

        double lineTotal = unitPrice * qty;
        JLabel lineTotalLabel = new JLabel(String.format("$ %.2f", lineTotal));
        lineTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JButton removeBtn = new JButton("Remove");
        removeBtn.setMargin(new Insets(3, 8, 3, 8));

        spinner.addChangeListener(evt -> {
            int newQty = (Integer) spinner.getValue();
            double curPrice = item.has("price") ? item.optDouble("price", 0.0) : 0.0;
            lineTotalLabel.setText(String.format("$ %.2f", curPrice * newQty));
            recalcTotal();
            updateCartQuantityAsync(item, newQty);
        });

        removeBtn.addActionListener(ae -> {
            int confirm = JOptionPane.showConfirmDialog(CartPage.this, "Remove " + name + " from cart?", "Confirm",
                    JOptionPane.YES_NO_OPTION);
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

        // If product details missing, fetch asynchronously and update UI
        if (!item.has("name") || !item.has("price")) {
            new Thread(() -> {
                try {
                    String pid = getProductId(item);
                    if (pid != null) {
                        try {
                            String pbody = HttpUtil.getString("http://localhost:8080/products/" + pid,
                                    java.util.Map.of());
                            org.json.JSONObject prod = new org.json.JSONObject(pbody);
                            if (prod.has("price"))
                                item.put("price", prod.getDouble("price"));
                            if (prod.has("name"))
                                item.put("name", prod.getString("name"));
                            if (prod.has("image"))
                                item.put("image", prod.getString("image"));
                            SwingUtilities.invokeLater(() -> {
                                nameLabel.setText(item.optString("name", "Unknown"));
                                priceLabel.setText(String.format("Unit price: $ %.2f", item.optDouble("price", 0.0)));
                                String img = item.optString("image", "");
                                if (img != null && !img.isEmpty()) {
                                    String imageUrl = img.startsWith("http") ? img
                                            : "http://localhost:8080/uploads/" + img;
                                    try {
                                        byte[] imgBytes = HttpUtil.getBytes(imageUrl, null);
                                        if (imgBytes != null && imgBytes.length > 0) {
                                            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(
                                                    imgBytes);
                                            Image imgObj = ImageIO.read(bis);
                                            if (imgObj != null) {
                                                Image scaled = imgObj.getScaledInstance(110, 90, Image.SCALE_SMOOTH);
                                                SwingUtilities
                                                        .invokeLater(() -> imgLabel.setIcon(new ImageIcon(scaled)));
                                            }
                                        }
                                    } catch (Exception ex) {
                                        // ignore image load failure
                                    }
                                }
                                int currentQty = spinner.getValue() instanceof Integer ? (Integer) spinner.getValue()
                                        : item.optInt("quantity", 1);
                                lineTotalLabel
                                        .setText(String.format("$ %.2f", item.optDouble("price", 0.0) * currentQty));
                                recalcTotal();
                            });
                        } catch (Exception ex) {
                            // ignore fetch errors
                        }
                    }
                } catch (Exception ex) {
                    // ignore fetch errors
                }
            }).start();
        }

        return card;
    }

    // Recalculate total from current cartItems and update totalLabel
    private void recalcTotal() {
        double tot = 0.0;
        for (JSONObject item : cartModel.getItems()) {
            double price = item.has("price") ? item.optDouble("price", Double.NaN) : Double.NaN;
            int qty = item.optInt("quantity", 1);
            if (!Double.isNaN(price)) {
                tot += price * qty;
            }
        }
        totalLabel.setText(String.format("Total: $ %.2f", tot));
    }

    // Async update quantity on server
    private void updateCartQuantityAsync(JSONObject item, int newQty) {
        new Thread(() -> {
            try {
                org.json.JSONObject body = new org.json.JSONObject();
                String pid = getProductId(item);
                if (pid != null)
                    body.put("productId", pid);
                body.put("quantity", newQty);
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Content-Type", "application/json");
                if (AuthManager.Token != null && !AuthManager.Token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + AuthManager.Token);
                    headers.put("Refresh-Token", AuthManager.Refresh);
                }
                try {
                    HttpUtil.postString("http://localhost:8080/cart", body.toString(), headers);
                    // assume success if no exception
                    item.put("quantity", newQty);
                    SwingUtilities.invokeLater(this::recalcTotal);
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(CartPage.this, "Failed to update quantity: " + ex.getMessage());
                        loadCartFromServer();
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(CartPage.this,
                        "Error updating quantity: " + ex.getMessage()));
                // reload to ensure consistency
                loadCartFromServer();
            }
        }).start();
    }

    // Async remove item from cart
    private void removeCartItemAsync(JSONObject item) {
        new Thread(() -> {
            try {
                String pid = getProductId(item);
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                if (AuthManager.Token != null && !AuthManager.Token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + AuthManager.Token);
                    headers.put("Refresh-Token", AuthManager.Refresh);
                }
                try {
                    if (pid != null) {
                        HttpUtil.delete("http://localhost:8080/cart/" + pid, null, headers);
                    } else {
                        org.json.JSONObject body = new org.json.JSONObject();
                        if (item.has("productId"))
                            body.put("productId", item.get("productId"));
                        else if (item.has("id"))
                            body.put("productId", item.get("id"));
                        HttpUtil.delete("http://localhost:8080/cart", body.toString(), headers);
                    }
                    cartModel.removeItem(item);
                    SwingUtilities.invokeLater(() -> {
                        rebuildItemsUI();
                        recalcTotal();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(CartPage.this,
                            "Failed to remove item: " + ex.getMessage()));
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(
                        () -> JOptionPane.showMessageDialog(CartPage.this, "Error removing item: " + ex.getMessage()));
            }
        }).start();
    }

    // Async checkout
    private void checkoutAsync() {
        new Thread(() -> {
            try {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Content-Type", "application/json");
                if (AuthManager.Token != null && !AuthManager.Token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + AuthManager.Token);
                    headers.put("Refresh-Token", AuthManager.Refresh);
                }
                try {
                    HttpUtil.postString("http://localhost:8080/cart/checkout", "{}", headers);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(CartPage.this, "Checkout successful!");
                        cartModel.clear();
                        rebuildItemsUI();
                        recalcTotal();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(
                            () -> JOptionPane.showMessageDialog(CartPage.this, "Checkout failed: " + ex.getMessage()));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(CartPage.this,
                        "Error during checkout: " + ex.getMessage()));
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

        setPreferredSize(new java.awt.Dimension(900, 600));
        setBackground(new java.awt.Color(250, 250, 250));

        // topPanel
        topPanel.setBackground(new java.awt.Color(245, 245, 245));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        homeButton.setText("Home");
        homeButton.setFont(new java.awt.Font("Segoe UI", 0, 14));
        homeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                // open HomePage
                parent.showPage("HOME");
            }
        });

        titleLabel.setFont(new java.awt.Font("Segoe UI", 1, 18));
        titleLabel.setText("Your Cart");

        javax.swing.GroupLayout topPanelLayout = new javax.swing.GroupLayout(topPanel);
        topPanel.setLayout(topPanelLayout);
        topPanelLayout.setHorizontalGroup(
                topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(topPanelLayout.createSequentialGroup()
                                .addComponent(homeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(titleLabel)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        topPanelLayout.setVerticalGroup(
                topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(topPanelLayout.createSequentialGroup()
                                .addGroup(topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(homeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 34,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(titleLabel))
                                .addGap(0, 6, Short.MAX_VALUE)));

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
                // confirm then go to billing page
                int confirm = JOptionPane.showConfirmDialog(CartPage.this, "Proceed to checkout?", "Checkout",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    parent.showPage("BILLING");
                }
            }
        });

        javax.swing.GroupLayout bottomLayout = new javax.swing.GroupLayout(bottomPanel);
        bottomPanel.setLayout(bottomLayout);
        bottomLayout.setHorizontalGroup(
                bottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bottomLayout.createSequentialGroup()
                                .addComponent(totalLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 300,
                                        Short.MAX_VALUE)
                                .addComponent(checkoutButton, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)));
        bottomLayout.setVerticalGroup(
                bottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bottomLayout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(bottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(totalLabel)
                                        .addComponent(checkoutButton, javax.swing.GroupLayout.PREFERRED_SIZE, 36,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap()));

        // Main frame layout
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(topPanel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(itemsScrollPane)
                        .addComponent(bottomPanel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(topPanel, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(itemsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 420,
                                        Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(bottomPanel, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));

        this.revalidate();
        this.repaint();
    }

}
