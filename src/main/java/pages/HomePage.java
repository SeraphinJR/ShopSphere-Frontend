package pages;

import javax.swing.*;
import java.awt.*;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.net.URI;
import model.CartModel;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/**
 * HomePage - ShopSphere
 * NetBeans GUI Builder style JFrame with initComponents().
 */
public class HomePage extends JPanel {

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
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://localhost:8080/products");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            String body = sb.toString();
            if (responseCode >= 200 && responseCode < 300) {
                JSONArray products = new JSONArray(body);
                SwingUtilities.invokeLater(() -> displayProducts(products));
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Failed to load products: " + responseCode + "\n" + body));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Error fetching products: " + ex.getMessage()));
        } finally {
            if (conn != null) conn.disconnect();
            // optional: re-enable controls here
        }
    }).start();
}


    // Updated createProductCard method to optionally load images from URL
    // Replace your old createProductCard with this one
    private JPanel createProductCard(String id, String name, String price, String imagePath, boolean isUrl, double rating, int reviewCount) {
        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(260, 260));
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        card.setBackground(UIManager.getColor("Panel.background"));

        // --- Image area (top) ---
        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(260, 160));
        imageLabel.setOpaque(false);

        try {
            Image img = null;
            if (imagePath != null && !imagePath.isEmpty()) {
                if (isUrl) {
                    img = ImageIO.read(URI.create(imagePath).toURL());
                } else {
                    String imageUrl = "http://localhost:8080/uploads/" + URLEncoder.encode(imagePath, StandardCharsets.UTF_8.toString());
                    img = ImageIO.read(URI.create(imageUrl).toURL());
                }
            }
            if (img != null) {
                Image scaled = img.getScaledInstance(240, 150, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaled));
            } else {
                imageLabel.setText("<no image>");
            }
        } catch (Exception ex) {
            imageLabel.setText("<image error>");
        }

        // --- Middle info area (name, price, rating) ---
        JPanel info = new JPanel();
        info.setLayout(new BorderLayout());
        info.setOpaque(false);
        info.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // Name + price row
        JLabel nameLabel = new JLabel("<html><b>" + name + "</b></html>");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setOpaque(false);

        JLabel priceLabel = new JLabel(price);
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        priceLabel.setForeground(new Color(0, 128, 0));
        priceLabel.setOpaque(false);

        JPanel topInfo = new JPanel(new BorderLayout());
        topInfo.setOpaque(false);
        topInfo.add(nameLabel, BorderLayout.WEST);
        topInfo.add(priceLabel, BorderLayout.EAST);

        // Rating row (star, numeric rating, review count)
        JPanel ratingRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        ratingRow.setOpaque(false);

        // star icon (unicode) — colored
        JLabel starLabel = new JLabel("\u2605"); // ★
        starLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        starLabel.setForeground(new Color(212, 175, 55)); // gold-ish

        // numeric rating
        String ratingText = String.format("%.1f", rating);
        JLabel ratingValue = new JLabel(ratingText);
        ratingValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // review count
        JLabel reviewCountLabel = new JLabel("(" + reviewCount + (reviewCount == 1 ? " review)" : " reviews)"));
        reviewCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        reviewCountLabel.setForeground(UIManager.getColor("Label.foreground"));

        ratingRow.add(starLabel);
        ratingRow.add(ratingValue);
        ratingRow.add(reviewCountLabel);

        // put topInfo and ratingRow in a vertical box
        JPanel mid = new JPanel();
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setOpaque(false);
        mid.add(topInfo);
        mid.add(Box.createVerticalStrut(6));
        mid.add(ratingRow);

        info.add(mid, BorderLayout.CENTER);

        // --- bottom: add to cart button ---
        JButton addBtn = new JButton("Add to cart");
        addBtn.setPreferredSize(new Dimension(120, 28));
        addBtn.addActionListener(e -> {
            // keep existing behaviour (unchanged)
            try {
                URL url = URI.create("http://localhost:8080/cart").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                conn.setDoOutput(true);

                JSONObject requestBody = new JSONObject();
                requestBody.put("productId", id);
                requestBody.put("quantity", "1");

                try (java.io.OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject prod = new JSONObject();
                    prod.put("productId", id);
                    prod.put("quantity", 1);
                    JOptionPane.showMessageDialog(HomePage.this, name + " added to cart.");
                    cartModel.addItem(prod);
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errResp = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null)
                        errResp.append(line);
                    br.close();

                    JOptionPane.showMessageDialog(this, "Failed to add to cart: " + errResp);
                }

                conn.disconnect();

            } catch (Exception err) {
                err.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: " + err.getMessage());
            }
        });

        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnWrap.setOpaque(false);
        btnWrap.add(addBtn);
        bottomRow.add(btnWrap, BorderLayout.EAST);

        info.add(bottomRow, BorderLayout.SOUTH);

        // assemble card
        card.add(imageLabel, BorderLayout.NORTH);
        card.add(info, BorderLayout.CENTER);

        // clickable: open product detail
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
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
            // Construct URL with query param
            String urlString = "http://localhost:8080/products";
            if (!query.isEmpty()) {
                urlString += "?search=" + java.net.URLEncoder.encode(query, "UTF-8");
            }

            URL url = URI.create(urlString).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parse JSON array and update products panel
                JSONArray productsArray = new JSONArray(response.toString());
                displayProducts(productsArray);
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearSearch(java.awt.event.ActionEvent evt) {
        searchField.setText(""); // clear text
        try {
            // reload all products (no query param)
            URL url = new URL("http://localhost:8080/products");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONArray productsArray = new JSONArray(response.toString());
                displayProducts(productsArray);
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayProducts(JSONArray productsArray) {
        productPanel.removeAll(); // Clear old products
        for (int i = 0; i < productsArray.length(); i++) {
            JSONObject prod = productsArray.getJSONObject(i);
            // defensive reads (use opt... so bad/missing values don't blow up)
            String id = String.valueOf(prod.opt("id"));
            String name = prod.optString("name", "Unnamed");
            String price = "$" + String.format("%.2f", prod.optDouble("price", 0.0));
            String imagePath = prod.optString("image", "");
            boolean isUrl = imagePath.startsWith("http");
            double rating = prod.has("rating") ? prod.optDouble("rating", 0.0) : 0.0;
            int reviewCount = prod.has("reviewCount") ? prod.optInt("reviewCount", 0) : prod.optInt("review_count", 0);

            JPanel card = createProductCard(id, name, price, imagePath, isUrl, rating, reviewCount);
            productPanel.add(card);
        }
        productPanel.revalidate();
        productPanel.repaint();
    }


    private void checkVendorAndOpen() {
        checkRoleAndOpenPage("VENDOR", "Please sign in first. Go to your Profile to upgrade to Vendor.");
    }

    private void checkAdminAndOpen() {
        checkRoleAndOpenPage("ADMIN", "Please sign in with an admin account.");
    }

    private void checkRoleAndOpenPage(String targetPage, String notSignedInMessage) {
        // quick not-signed-in shortcut
        if (AuthManager.Token == null || AuthManager.Token.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                notSignedInMessage,
                "Not signed in",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // show wait cursor
        Cursor old = getCursor();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new Thread(() -> {
            HttpURLConnection conn = null;
            String errorMsg = null;
            final boolean[] allowed = {false};
            try {
                URL url = URI.create("http://localhost:8080/auth/current/").toURL();
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                int rc = conn.getResponseCode();
                InputStream is = (rc >= 200 && rc < 300) ? conn.getInputStream() : conn.getErrorStream();
                String body = readStream(is);

                // Defensive parse: handle { role: ... } or { data: { role: ... } } or roles array
                JSONObject root = new JSONObject(body);

                Object roleObj = null;
                if (root.has("role")) roleObj = root.get("role");
                else if (root.has("roles")) roleObj = root.get("roles");
                else if (root.has("data") && root.get("data") instanceof JSONObject) {
                    JSONObject data = root.getJSONObject("data");
                    if (data.has("role")) roleObj = data.get("role");
                    else if (data.has("roles")) roleObj = data.get("roles");
                }

                // normalize to uppercase set
                Set<String> roles = new HashSet<>();
                if (roleObj instanceof JSONArray) {
                    JSONArray a = (JSONArray) roleObj;
                    for (int i = 0; i < a.length(); i++) roles.add(String.valueOf(a.get(i)).toUpperCase());
                } else if (roleObj instanceof String) {
                    String s = ((String) roleObj).trim();
                    if (s.contains(",")) {
                        for (String part : s.split("\\s*,\\s*")) roles.add(part.toUpperCase());
                    } else {
                        roles.add(s.toUpperCase());
                    }
                } else if (roleObj != null) {
                    String s = String.valueOf(roleObj);
                    for (String part : s.split("\\s*,\\s*")) roles.add(part.toUpperCase());
                }

                // Process the role information from the response
                Set<String> userRoles = new HashSet<>();
                if (roleObj instanceof JSONArray) {
                    JSONArray rolesArray = (JSONArray) roleObj;
                    for (int i = 0; i < rolesArray.length(); i++) {
                        userRoles.add(String.valueOf(rolesArray.get(i)).toUpperCase());
                    }
                } else if (roleObj instanceof String) {
                    String roleStr = ((String) roleObj).trim();
                    if (roleStr.contains(",")) {
                        for (String part : roleStr.split("\\s*,\\s*")) {
                            userRoles.add(part.toUpperCase());
                        }
                    } else {
                        userRoles.add(roleStr.toUpperCase());
                    }
                }

                // Check permissions based on the target page
                final boolean isAdmin = userRoles.contains("ADMIN") || userRoles.contains("ROLE_ADMIN");
                final boolean isVendor = userRoles.contains("VENDOR") || userRoles.contains("ROLE_VENDOR");
                allowed[0] = isAdmin || ("VENDOR".equals(targetPage) && isVendor);

            } catch (Exception ex) {
                ex.printStackTrace();
                errorMsg = ex.getMessage();
            } finally {
                if (conn != null) conn.disconnect();
            }


            final String err = errorMsg;
            SwingUtilities.invokeLater(() -> {
                setCursor(old);
                if (allowed[0]) {
                    parent.showPage(targetPage);
                } else {
                    String msg = "ADMIN".equals(targetPage) ? "Admin access required." : "Upgrade to vendor through your profile.";
                    if (err != null && !err.isEmpty()) {
                        msg += "\n\n(Notice: couldn't verify role: " + err + ")";
                    }
                    JOptionPane.showMessageDialog(HomePage.this, msg, "Upgrade required", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        }).start();
    }

    // small helper (if you already have one in the class, you can omit this duplicate)
    private static String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
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

        setBackground(UIManager.getColor("Panel.background"));
        setPreferredSize(new java.awt.Dimension(1000, 600));

        // Top panel (search + cart)
        topPanel.setBackground(UIManager.getColor("Panel.background"));
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
        JMenuItem miAdmin = new JMenuItem("Admin Panel");

        miCart.addActionListener(e -> parent.showPage("CART"));
        miOrders.addActionListener(e -> parent.showPage("ORDERS"));
        miProfile.addActionListener(e -> parent.showPage("PROFILE"));
        miVendor.addActionListener(e -> checkVendorAndOpen());
        miAdmin.addActionListener(e -> checkAdminAndOpen());


        mainMenu.add(miCart);
        mainMenu.add(miOrders);
        mainMenu.add(miProfile);
        mainMenu.addSeparator();
        mainMenu.add(miVendor);
        mainMenu.add(miAdmin);
        mainMenu.addSeparator();
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
            // Check roles before showing menu
            checkRolesAndUpdateMenu(miVendor, miAdmin);
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
        productPanel.setBackground(UIManager.getColor("Panel.background"));

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

    private void checkRolesAndUpdateMenu(JMenuItem vendorItem, JMenuItem adminItem) {
        if (AuthManager.Token == null || AuthManager.Token.trim().isEmpty()) {
            vendorItem.setVisible(false);
            adminItem.setVisible(false);
            return;
        }

        try {
            URL url = URI.create("http://localhost:8080/auth/current/").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
            conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            int rc = conn.getResponseCode();
            if (rc >= 200 && rc < 300) {
                String body = readStream(conn.getInputStream());
                JSONObject root = new JSONObject(body);

                Object roleObj = null;
                if (root.has("role")) roleObj = root.get("role");
                else if (root.has("roles")) roleObj = root.get("roles");
                else if (root.has("data") && root.get("data") instanceof JSONObject) {
                    JSONObject data = root.getJSONObject("data");
                    if (data.has("role")) roleObj = data.get("role");
                    else if (data.has("roles")) roleObj = data.get("roles");
                }

                Set<String> roles = new HashSet<>();
                if (roleObj instanceof JSONArray) {
                    JSONArray a = (JSONArray) roleObj;
                    for (int i = 0; i < a.length(); i++) {
                        roles.add(String.valueOf(a.get(i)).toUpperCase());
                    }
                } else if (roleObj instanceof String) {
                    String s = ((String) roleObj).trim().toUpperCase();
                    if (s.contains(",")) {
                        for (String part : s.split("\\s*,\\s*")) {
                            roles.add(part);
                        }
                    } else {
                        roles.add(s);
                    }
                }

                boolean isAdmin = roles.contains("ADMIN") || roles.contains("ROLE_ADMIN");
                boolean isVendor = roles.contains("VENDOR") || roles.contains("ROLE_VENDOR");

                vendorItem.setVisible(isAdmin || isVendor);
                adminItem.setVisible(isAdmin);
            } else {
                // On error, hide both items
                vendorItem.setVisible(false);
                adminItem.setVisible(false);
            }

            conn.disconnect();
        } catch (Exception ex) {
            // On error, hide both items
            vendorItem.setVisible(false);
            adminItem.setVisible(false);
        }
    }



    // ---------------------------------------------------------------------

    /**
     * Main method for standalone testing.
     */

}
