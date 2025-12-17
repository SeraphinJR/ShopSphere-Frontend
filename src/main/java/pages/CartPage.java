package pages;

import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.json.*;
import model.CartModel;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.net.URLEncoder;

/**
 * CartPage - displays user's cart, allows quantity changes and checkout.
 *
 * Notes:
 * - Product details are fetched asynchronously per card (no blocking UI).
 * - Spinner updates are optimistic; server updates run in background and revert on failure.
 * - Total calculation runs off the EDT (background thread).
 */
public class CartPage extends JPanel {

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
    private MainFrame parent;
    private final CartModel cartModel;

    public CartPage(MainFrame parent, CartModel cartModel) {
        this.parent = parent;
        this.cartModel = cartModel;
        initComponents();

        // When cart model changes elsewhere, rebuild UI and recalc totals
        cartModel.addChangeListener(v -> {
            rebuildItemsUI();
            recalcTotalAsync();
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
                URL url = new URL("http://localhost:8080/cart");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
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
                        JSONObject wrap = new JSONObject(resp);
                        if (wrap.has("items") && wrap.get("items") instanceof JSONArray) {
                            itemsArray = wrap.getJSONArray("items");
                        } else {
                            itemsArray = new JSONArray();
                        }
                    }

                    cartModel.clear();
                    for (int i = 0; i < itemsArray.length(); i++) {
                        cartModel.addItem(itemsArray.getJSONObject(i));
                    }

                    SwingUtilities.invokeLater(() -> {
                        rebuildItemsUI();
                        recalcTotalAsync();
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
        }, "CartPage-loadCart").start();
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

    // Rebuild the itemsPanel from cartModel items
    private void rebuildItemsUI() {
        itemsPanel.removeAll();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));

        for (JSONObject item : cartModel.getItems()) {
            JPanel card = makeCartItemCard(item);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
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
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        card.setBackground(UIManager.getColor("Panel.background"));

        // center: placeholders
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);

        JLabel imgLabel = new JLabel("[loading image]");
        imgLabel.setPreferredSize(new Dimension(120, 90));
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setVerticalAlignment(SwingConstants.CENTER);

        JLabel nameLabel = new JLabel("Loading...");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel priceLabel = new JLabel("Unit price: $ 0.00");
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        priceLabel.setForeground(new Color(0, 128, 0));

        center.add(nameLabel, BorderLayout.NORTH);
        center.add(priceLabel, BorderLayout.CENTER);

        card.add(imgLabel, BorderLayout.WEST);
        card.add(center, BorderLayout.CENTER);

        // Right: quantity controls + line total + remove
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);

        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        qtyPanel.setOpaque(false);
        qtyPanel.add(new JLabel("Qty:"));

        int qtyInit = item.optInt("quantity", 1);
        SpinnerNumberModel model = new SpinnerNumberModel(qtyInit, 1, 1000, 1);
        JSpinner spinner = new JSpinner(model);
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(2);
        qtyPanel.add(spinner);

        // holder for unitPrice that will be set when the product fetch completes
        final double[] unitPriceHolder = new double[]{0.0};

        // line total label
        JLabel lineTotalLabel = new JLabel(String.format("$ %.2f", unitPriceHolder[0] * qtyInit));
        lineTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // remove button
        JButton removeBtn = new JButton("Remove");
        removeBtn.setMargin(new Insets(3, 8, 3, 8));

        right.add(qtyPanel);
        right.add(Box.createVerticalStrut(6));
        right.add(lineTotalLabel);
        right.add(Box.createVerticalStrut(8));
        right.add(removeBtn);
        right.setAlignmentY(Component.TOP_ALIGNMENT);

        card.add(right, BorderLayout.EAST);

        // spinner protection: ignore events while an update is inflight
        AtomicBoolean spinnerUpdating = new AtomicBoolean(false);
        AtomicInteger prevQty = new AtomicInteger(qtyInit);

        spinner.addChangeListener(evt -> {
            if (spinnerUpdating.get()) return; // ignore interim events
            int newQty = (Integer) spinner.getValue();
            int oldQty = prevQty.get();
            if (newQty == oldQty) return;

            // optimistic UI change for line total (use current unitPriceHolder)
            double localUnit = unitPriceHolder[0];
            lineTotalLabel.setText(String.format("$ %.2f", localUnit * newQty));
            recalcTotalAsync();

            // perform server update with callbacks
            spinnerUpdating.set(true);
            spinner.setEnabled(false);
            updateCartQuantityAsync(item, newQty,
                // onSuccess
                () -> {
                    prevQty.set(newQty);
                    item.put("quantity", newQty);
                    // reflect change visually (no full rebuild)
                    SwingUtilities.invokeLater(() -> {
                        spinnerUpdating.set(false);
                        spinner.setEnabled(true);
                        recalcTotalAsync();
                    });
                },
                // onFailure
                (errMsg) -> {
                    SwingUtilities.invokeLater(() -> {
                        // revert spinner, restore totals
                        spinner.setValue(oldQty);
                        spinnerUpdating.set(false);
                        spinner.setEnabled(true);
                        recalcTotalAsync();
                        JOptionPane.showMessageDialog(CartPage.this, "Failed to update quantity: " + errMsg, "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            );
        });

        // remove button action
        removeBtn.addActionListener(ae -> {
            int confirm = JOptionPane.showConfirmDialog(CartPage.this, "Remove item from cart?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                removeCartItemAsync(item);
            }
        });

        // Asynchronously fetch product details (name, price, image) and update card when ready
        new Thread(() -> {
            try {
                String productId = String.valueOf(item.opt("productId"));
                URL url = new URL("http://localhost:8080/products/" + productId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                if (AuthManager.Token != null && !AuthManager.Token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                }
                int rc = conn.getResponseCode();
                if (rc >= 200 && rc < 300) {
                    String body = readStream(conn.getInputStream());
                    JSONObject prod = new JSONObject(body);
                    String name = prod.optString("name", "Product");
                    double unit = prod.optDouble("price", 0.0);
                    String imageField = prod.optString("image", "");

                    // update UI on EDT
                    SwingUtilities.invokeLater(() -> {
                        nameLabel.setText(name);
                        priceLabel.setText(String.format("Unit price: $ %.2f", unit));
                        unitPriceHolder[0] = unit;
                        // update line total using latest unit price and current spinner value
                        int curQty = (Integer) spinner.getValue();
                        lineTotalLabel.setText(String.format("$ %.2f", unit * curQty));
                        recalcTotalAsync();
                        // load image asynchronously into label (kept simple)
                    });

                    if (imageField != null && !imageField.isEmpty()) {
                        String encoded = URLEncoder.encode(imageField, StandardCharsets.UTF_8);
                        String imageUrl = "http://localhost:8080/uploads/" + imageField;

                        try {
                            Image img = ImageIO.read(new URL(imageUrl));
                            if (img != null) {
                                Image scaled = img.getScaledInstance(110, 90, Image.SCALE_SMOOTH);
                                SwingUtilities.invokeLater(() -> {
                                    imgLabel.setText("");
                                    imgLabel.setIcon(new ImageIcon(scaled));
                                });
                            } else {
                                SwingUtilities.invokeLater(() -> imgLabel.setText("[no image]"));
                            }
                        } catch (Exception imgEx) {
                            SwingUtilities.invokeLater(() -> imgLabel.setText("[no image]"));
                        }
                    } else {
                        SwingUtilities.invokeLater(() -> imgLabel.setText("[no image]"));
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        nameLabel.setText("Product #" + item.opt("productId"));
                        priceLabel.setText("Unit price: $ 0.00");
                        imgLabel.setText("[no image]");
                    });
                }
                conn.disconnect();
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    nameLabel.setText("Product #" + item.opt("productId"));
                    priceLabel.setText("Unit price: $ 0.00");
                    imgLabel.setText("[no image]");
                });
            }
        }, "CartPage-prodFetch-" + item.opt("productId")).start();

        return card;
    }

    // Recalculate total in background (no blocking UI)
    private void recalcTotalAsync() {
        new Thread(() -> {
            double tot = 0.0;
            java.util.List<JSONObject> items = cartModel.getItems();
            for (JSONObject item : items) {
                try {
                    String pid = String.valueOf(item.opt("productId"));
                    URL url = new URL("http://localhost:8080/products/" + pid);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    if (AuthManager.Token != null && !AuthManager.Token.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                        conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                    }
                    int rc = conn.getResponseCode();
                    if (rc >= 200 && rc < 300) {
                        String body = readStream(conn.getInputStream());
                        JSONObject prod = new JSONObject(body);
                        double unitPrice = prod.optDouble("price", 0.0);
                        int qty = item.optInt("quantity", 1);
                        tot += unitPrice * qty;
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    // log and skip item
                    e.printStackTrace();
                }
            }
            final double finalTot = tot;
            SwingUtilities.invokeLater(() -> totalLabel.setText(String.format("Total: $ %.2f", finalTot)));
        }, "CartPage-recalcTotal").start();
    }

    // Async update quantity to backend with callbacks for success/failure
    private void updateCartQuantityAsync(JSONObject item, int newQty, Runnable onSuccess, Consumer<String> onFailure) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://localhost:8080/cart");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                }
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
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
                    if (onSuccess != null) onSuccess.run();
                } else {
                    if (onFailure != null) onFailure.accept("HTTP " + rc + ": " + resp);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                if (onFailure != null) onFailure.accept(ex.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }, "CartPage-updateQty").start();
    }

    // Overload used earlier in code for convenience (no callbacks)
    private void updateCartQuantityAsync(JSONObject item, int newQty) {
        updateCartQuantityAsync(item, newQty,
            () -> {
                // success: update model and recalc
                item.put("quantity", newQty);
                SwingUtilities.invokeLater(() -> recalcTotalAsync());
            },
            (err) -> {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(CartPage.this, "Failed to update quantity: " + err);
                    loadCartFromServer(); // re-sync
                });
            }
        );
    }

    // Async remove item from cart
    private void removeCartItemAsync(JSONObject item) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                int pid = item.optInt("productId", item.optInt("id", -1));
                URL url = new URL("http://localhost:8080/cart/" + pid);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Accept", "application/json");
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                }
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
                    cartModel.removeItem(item);
                    SwingUtilities.invokeLater(() -> {
                        rebuildItemsUI();
                        recalcTotalAsync();
                    });
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(CartPage.this, "Failed to remove item: " + rc + "\n" + resp));
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(CartPage.this, "Error removing item: " + ex.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }, "CartPage-removeItem").start();
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
        setBackground(UIManager.getColor("Panel.background"));

        // topPanel
        topPanel.setBackground(UIManager.getColor("Panel.background"));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        homeButton.setText("Home");
        homeButton.setFont(new java.awt.Font("Segoe UI", 0, 14));
        homeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
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
        itemsPanel.setBackground(UIManager.getColor("Panel.background"));
        itemsPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        itemsScrollPane.setViewportView(itemsPanel);

        // bottomPanel: total + checkout
        bottomPanel.setBackground(UIManager.getColor("Panel.background"));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        totalLabel.setFont(new java.awt.Font("Segoe UI", 1, 16));
        totalLabel.setText("Total: $ 0.00");

        checkoutButton.setText("Checkout");
        checkoutButton.setFont(new java.awt.Font("Segoe UI", 0, 14));
        checkoutButton.addActionListener(evt -> {
            int confirm = JOptionPane.showConfirmDialog(CartPage.this, "Proceed to checkout?", "Checkout", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            new Thread(() -> {
                try {
                    // Build payload from cartModel
                    JSONArray arr = new JSONArray();
                    java.util.List<JSONObject> snapshot =cartModel.getItems(); 
                    for (JSONObject it : snapshot) {
                        JSONObject o = new JSONObject();
                        o.put("productId", it.opt("productId"));
                        o.put("quantity", it.optInt("quantity", 1));
                        arr.put(o);
                    }

                    JSONObject body = new JSONObject();
                    body.put("items", arr);

                    URL url = new URL("http://localhost:8080/orders");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    if (AuthManager.Token != null && !AuthManager.Token.isEmpty())
                        conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                    conn.setDoOutput(true);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    int rc = conn.getResponseCode();
                    String resp;
                    try (InputStream is = (rc >= 200 && rc < 300) ? conn.getInputStream() : conn.getErrorStream()) {
                        resp = new BufferedReader(new InputStreamReader(is)).lines().reduce("", (a,b) -> a+b);
                    }
                    conn.disconnect();

                    if (rc >= 200 && rc < 300) {
                        JSONObject jsonResp = new JSONObject(resp);
                        long orderId = jsonResp.optLong("orderId", jsonResp.optLong("id", -1));
                        if (orderId == -1) throw new Exception("No orderId returned");

                        // Switch to BillingPanel on EDT
                        SwingUtilities.invokeLater(() -> parent.openBillingWithOrder(orderId));
                    } else {
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(CartPage.this, "Failed to create order: " + rc + "\n" + resp)
                        );
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(CartPage.this, "Error creating order: " + ex.getMessage())
                    );
                }
            }, "CartPage-checkout").start();
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
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
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

        this.revalidate();
        this.repaint();
    }

    // ------------------- End of class ------------------- 
}
