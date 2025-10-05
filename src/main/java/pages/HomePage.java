package pages;

import javax.swing.*;
import java.awt.*;

// networking handled via HttpUtil
import org.json.JSONArray;
import org.json.JSONObject;
import javax.imageio.ImageIO;
import java.awt.Image;
import model.CartModel;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * HomePage - ShopSphere
 * NetBeans GUI Builder style JFrame with initComponents().
 */
public class HomePage extends JPanel {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(HomePage.class.getName());

    // Variables declaration - do not modify
    private javax.swing.JButton cartButton;
    private javax.swing.JPopupMenu mainMenu;
    private javax.swing.JButton searchButton;
    private javax.swing.JTextField searchField;
    private javax.swing.JButton clearButton;
    private javax.swing.JPanel productPanel;
    private javax.swing.JScrollPane productScrollPane;
    private javax.swing.JPanel topPanel;
    // End of variables declaration

    /**
     * Creates new form HomePage
     */
    private MainFrame parent;
    private final CartModel cartModel;

    public HomePage(MainFrame parent, CartModel cartModel) {
        this.cartModel = cartModel;
        this.parent = parent;
        initComponents();
        // apply global theme styling to this page
        try {
            Theme.styleComponentTree(this);
        } catch (Throwable ignored) {
        }
        // Assuming you have a JScrollPane named scrollPaneProducts
        productScrollPane.getVerticalScrollBar().setUnitIncrement(20); // default is ~1-5, increase to speed up
        productScrollPane.getHorizontalScrollBar().setUnitIncrement(20); // optional if horizontal scroll

        // enforce size and center
        setSize(1000, 600);

        // populate some sample products (replace with real data)
        refreshProducts();

        // show frame
        setVisible(true);
    }

    /**
     * Create some sample product cards.
     * Replace this with real data from your backend.
     */
    // Add imports at top

    // inside HomePage class, replace populateProducts() with:

    /**
     * Fetch full product list off the EDT and update UI on the EDT.
     * Replaces your previous populateProducts().
     */
    public void refreshProducts() {
        // optional: show temporary UI state (disable search button, show spinner ...)
        new Thread(() -> {
            int attempts = 0;
            while (attempts < 2) {
                try {
                    attempts++;
                    String body = HttpUtil.getString("http://localhost:8080/products",
                            java.util.Map.of("Accept", "application/json"));
                    org.json.JSONArray products = new org.json.JSONArray(body);
                    SwingUtilities.invokeLater(() -> displayProducts(products));
                    return; // success
                } catch (Exception ex) {
                    // on timeout or other transient failure, retry once
                    logger.log(java.util.logging.Level.WARNING, "Failed to fetch products (attempt " + attempts + ")",
                            ex);
                    if (attempts >= 2) {
                        final String msg = ex.getMessage() == null ? "Failed to fetch products" : ex.getMessage();
                        SwingUtilities.invokeLater(
                                () -> JOptionPane.showMessageDialog(this, "Error fetching products: " + msg));
                    } else {
                        try {
                            Thread.sleep(400); // brief backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        }).start();
    }

    // Updated createProductCard method to optionally load images from URL
    private JPanel createProductCard(String id, String name, String price, String imagePath, boolean isUrl) {
        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(260, 260));
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(260, 160));

        new Thread(() -> {
            try {
                byte[] bytes;
                if (isUrl) {
                    bytes = HttpUtil.getBytes(imagePath, null);
                } else {
                    bytes = HttpUtil.getBytes("http://localhost:8080/uploads/" + imagePath, null);
                }
                if (bytes != null && bytes.length > 0) {
                    java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(bytes);
                    Image img = ImageIO.read(bis);
                    if (img != null) {
                        Image scaled = img.getScaledInstance(240, 150, Image.SCALE_SMOOTH);
                        SwingUtilities.invokeLater(() -> imageLabel.setIcon(new ImageIcon(scaled)));
                        return;
                    }
                }
                SwingUtilities.invokeLater(() -> imageLabel.setText("<no image>"));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> imageLabel.setText("<image error>"));
            }
        }).start();

        JPanel info = new JPanel(new BorderLayout());
        info.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel priceLabel = new JLabel(price);
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        priceLabel.setForeground(new Color(0, 128, 0));

        JPanel topInfo = new JPanel(new BorderLayout());
        topInfo.add(nameLabel, BorderLayout.WEST);
        topInfo.add(priceLabel, BorderLayout.EAST);

        JButton addBtn = new JButton("Add to cart");
        addBtn.setPreferredSize(new Dimension(120, 28));
        addBtn.addActionListener(e -> {
            new Thread(() -> {
                try {
                    org.json.JSONObject requestBody = new org.json.JSONObject();
                    requestBody.put("productId", id);
                    requestBody.put("quantity", 1);
                    java.util.Map<String, String> headers = java.util.Map.of("Content-Type", "application/json",
                            "Authorization", "Bearer " + AuthManager.Token, "Refresh-Token", AuthManager.Refresh);
                    HttpUtil.postString("http://localhost:8080/cart", requestBody.toString(), headers);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(HomePage.this, name + " added to cart.");
                        org.json.JSONObject prod = new org.json.JSONObject();
                        prod.put("productId", id);
                        prod.put("quantity", 1);
                        cartModel.addItem(prod);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()));
                }
            }).start();
        });

        info.add(topInfo, BorderLayout.NORTH);
        info.add(addBtn, BorderLayout.SOUTH);

        card.add(imageLabel, BorderLayout.NORTH);
        card.add(info, BorderLayout.CENTER);
        // after card construction, before return
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // open detail frame on click (use product id as string)
                String pid = String.valueOf(id);
                SwingUtilities.invokeLater(() -> {
                    ProductDetailFrame f = new ProductDetailFrame(pid);
                    f.setVisible(true);
                });
            }
        });

        return card;
    }

    private void searchProducts(java.awt.event.ActionEvent evt) {
        String query = searchField.getText().trim();
        try {
            String urlString = "http://localhost:8080/products";
            if (!query.isEmpty()) {
                urlString += "?search=" + java.net.URLEncoder.encode(query, "UTF-8");
            }
            String body = HttpUtil.getString(urlString, java.util.Map.of("Accept", "application/json"));
            JSONArray productsArray = new JSONArray(body);
            displayProducts(productsArray);
        } catch (Exception e) {
            logger.log(java.util.logging.Level.WARNING, "Product search failed", e);
            JOptionPane.showMessageDialog(this, "Failed to load products: " + e.getMessage());
        }
    }

    private void clearSearch(java.awt.event.ActionEvent evt) {
        searchField.setText(""); // clear text
        try {
            String body = HttpUtil.getString("http://localhost:8080/products",
                    java.util.Map.of("Accept", "application/json"));
            JSONArray productsArray = new JSONArray(body);
            displayProducts(productsArray);
        } catch (Exception e) {
            logger.log(java.util.logging.Level.WARNING, "Failed to clear search/load products", e);
            JOptionPane.showMessageDialog(this, "Failed to load products: " + e.getMessage());
        }
    }

    private void displayProducts(JSONArray productsArray) {
        productPanel.removeAll(); // Clear old products
        for (int i = 0; i < productsArray.length(); i++) {
            JSONObject prod = productsArray.getJSONObject(i);
            String id = String.valueOf(prod.getInt("id"));
            String name = prod.getString("name");
            String price = "$" + prod.getDouble("price");
            String imagePath = prod.getString("image"); // could be URL or local path
            boolean isUrl = imagePath.startsWith("http");

            JPanel card = createProductCard(id, name, price, imagePath, isUrl);
            productPanel.add(card);
        }
        productPanel.revalidate();
        productPanel.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * NetBeans GUI Builder style generated code - DO NOT modify the guarded blocks.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        topPanel = new javax.swing.JPanel();
        searchField = new javax.swing.JTextField();
        searchButton = new javax.swing.JButton();
        clearButton = new javax.swing.JButton();
        cartButton = new javax.swing.JButton();
        productScrollPane = new javax.swing.JScrollPane();
        productPanel = new javax.swing.JPanel();

        setBackground(new java.awt.Color(250, 250, 250));
        setPreferredSize(new java.awt.Dimension(1000, 600));

        // Top panel (search + cart)
        topPanel.setBackground(new java.awt.Color(245, 245, 245));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        searchField.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        searchField.setToolTipText("Search products...");
        searchField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onSearch(evt);
            }
        });

        searchButton.setText("Search");
        searchButton.setFont(new java.awt.Font("Segoe UI", 0, 14));
        searchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchProducts(evt); // call your real search function
            }
        });

        clearButton.setText("Clear");
        clearButton.setFont(new java.awt.Font("Segoe UI", 0, 14));
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSearch(evt);
            }
        });

        // use hamburger symbol for compact menu button
        cartButton.setText("\u2630"); // ☰
        cartButton.setToolTipText("Open menu");
        cartButton.setFont(new java.awt.Font("Segoe UI", 1, 18));
        // create popup menu for Cart / Orders / Profile
        mainMenu = new JPopupMenu();
        JMenuItem miCart = new JMenuItem("Cart");
        JMenuItem miOrders = new JMenuItem("Orders");

        JMenuItem miProfile = new JMenuItem("Profile");
        JMenuItem miVendor = new JMenuItem("Vendor Dashboard");

        miCart.addActionListener(e -> parent.showPage("CART"));
        miOrders.addActionListener(e -> parent.showPage("ORDERS"));
        miProfile.addActionListener(e -> parent.showPage("PROFILE"));
        miVendor.addActionListener(e -> parent.showPage("VENDOR"));

        mainMenu.add(miCart);
        mainMenu.add(miOrders);
        mainMenu.add(miProfile);
        mainMenu.addSeparator();
        // Only include Vendor menu item if the client believes the user is a vendor
        if (AuthManager.IsVendor) {
            mainMenu.add(miVendor);
            mainMenu.addSeparator();
        }
        JMenuItem miLogout = new JMenuItem("Logout");
        miLogout.addActionListener(e -> {
            // clear tokens
            AuthManager.Token = "";
            AuthManager.Refresh = "";
            // close main frame and open login
            SwingUtilities.invokeLater(() -> {
                // dispose parent frame if it's a frame
                Window w = SwingUtilities.getWindowAncestor(HomePage.this);
                if (w != null)
                    w.dispose();
                Login login = new Login();
                login.setVisible(true);
            });
        });
        mainMenu.add(miLogout);

        cartButton.addActionListener(e -> {
            // show the popup menu aligned to the button
            mainMenu.show(cartButton, 0, cartButton.getHeight());
        });

        searchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchProducts(evt);
            }
        });

        // Layout for topPanel: simple GroupLayout generated-style
        javax.swing.GroupLayout topPanelLayout = new javax.swing.GroupLayout(topPanel);
        topPanel.setLayout(topPanelLayout);
        topPanelLayout.setHorizontalGroup(
                topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(topPanelLayout.createSequentialGroup()
                                .addComponent(searchField, javax.swing.GroupLayout.DEFAULT_SIZE, 760, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(searchButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(clearButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(cartButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)));
        topPanelLayout.setVerticalGroup(
                topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, topPanelLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 34,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(searchButton, javax.swing.GroupLayout.PREFERRED_SIZE, 34,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(clearButton, javax.swing.GroupLayout.PREFERRED_SIZE, 34,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cartButton, javax.swing.GroupLayout.PREFERRED_SIZE, 34,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))));
        topPanel.add(clearButton);

        // Product panel inside scroll pane
        productPanel.setLayout(new java.awt.GridLayout(0, 3, 16, 16)); // 3 columns, variable rows
        productPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        productPanel.setBackground(new java.awt.Color(255, 255, 255));

        productScrollPane.setViewportView(productPanel);

        // Main layout for the frame (GroupLayout)
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(topPanel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(productScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 984, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(topPanel, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(productScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 520,
                                        Short.MAX_VALUE)));

        this.revalidate();
        this.repaint();
    } // </editor-fold>

    // --- Event handlers --------------------------------------------------
    private void onSearch(java.awt.event.ActionEvent evt) {
        String query = searchField.getText().trim();
        // TODO: filter productPanel contents based on query by product name
        JOptionPane.showMessageDialog(this, "Search: " + query);
    }

    // navigation helpers used by menu lambdas

    // ---------------------------------------------------------------------

    /**
     * Main method for standalone testing.
     */

}
