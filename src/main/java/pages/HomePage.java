package pages;

import javax.swing.*;
import java.awt.*;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

    // Variables declaration - do not modify
    private javax.swing.JButton cartButton;
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
    public HomePage(MainFrame parent,CartModel cartModel) {
        this.cartModel=cartModel;
        this.parent=parent;
        initComponents();
        // Assuming you have a JScrollPane named scrollPaneProducts
        productScrollPane.getVerticalScrollBar().setUnitIncrement(20); // default is ~1-5, increase to speed up
        productScrollPane.getHorizontalScrollBar().setUnitIncrement(20); // optional if horizontal scroll


        // enforce size and center
        setSize(1000, 600);

        // populate some sample products (replace with real data)
        populateProducts();

        // show frame
        setVisible(true);
    }

    /**
     * Create some sample product cards.
     * Replace this with real data from your backend.
     */
    // Add imports at top

// inside HomePage class, replace populateProducts() with:

private void populateProducts() {
    try {
        URL url = new URL("http://localhost:8080/products");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            JSONArray products = new JSONArray(response.toString());

            // clear panel
            productPanel.removeAll();

            for (int i = 0; i < products.length(); i++) {
                JSONObject p = products.getJSONObject(i);
                String id = p.get("id").toString();
                String name = p.getString("name");
                String price = "$" + p.getDouble("price");
                String imageFile = p.getString("image"); // e.g., "image1.jpg"
                
                // If your images are served via HTTP, prepend the URL
                String imageUrl = "http://localhost:8080/images/" + imageFile;

                JPanel card = createProductCard(id, name, price, imageUrl, true);
                productPanel.add(card);
            }

            productPanel.revalidate();
            productPanel.repaint();

        } else {
            JOptionPane.showMessageDialog(this, "Failed to load products: " + responseCode);
        }

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error fetching products: " + e.getMessage());
    }
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

    try {
        Image img;
        if (isUrl) {
            img = ImageIO.read(new URL(imagePath));
        } else {
            java.net.URL imgUrl = getClass().getResource(imagePath);
            img = ImageIO.read(imgUrl);
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
        try{
            URL url=new URL("http://localhost:8080/cart");
            HttpURLConnection conn=(HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type","application/json");
            conn.setRequestProperty("Authorization","Bearer "+AuthManager.Token);
            conn.setRequestProperty("Refresh-Token",AuthManager.Refresh);
            conn.setDoOutput(true);
            
            JSONObject requestBody=new JSONObject();
            requestBody.put("productId",id);
            requestBody.put("quantity","1");
            
            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                JSONObject prod=new JSONObject();
                prod.put("productId",id);
                prod.put("quantity",1);
                JOptionPane.showMessageDialog(HomePage.this, name + " added to cart.");
                cartModel.addItem(prod);
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder errResp = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) errResp.append(line);
                br.close();

                JOptionPane.showMessageDialog(this, "Failed to add to cart: " + errResp);
            }

            conn.disconnect();
        
        }catch(Exception err){
            err.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + err.getMessage());

        }
    });

    info.add(topInfo, BorderLayout.NORTH);
    info.add(addBtn, BorderLayout.SOUTH);

    card.add(imageLabel, BorderLayout.NORTH);
    card.add(info, BorderLayout.CENTER);
    // after card construction, before return
    card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    card.addMouseListener(new MouseAdapter() {
        @Override public void mouseClicked(MouseEvent e) {
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
        // Construct URL with query param
        String urlString = "http://localhost:8080/products";
        if (!query.isEmpty()) {
            urlString += "?search=" + java.net.URLEncoder.encode(query, "UTF-8");
        }

        URL url = new URL(urlString);
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
            System.out.println("Failed to load products. Response code: " + responseCode);
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
            System.out.println("Failed to load products. Response code: " + responseCode);
        }
    } catch (Exception e) {
        e.printStackTrace();
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

        JPanel card = createProductCard(id, name, price, imagePath, true);
        productPanel.add(card);
    }
    productPanel.revalidate();
    productPanel.repaint();
}


    
    /**
     * This method is called from within the constructor to initialize the form.
     * NetBeans GUI Builder style generated code - DO NOT modify the guarded blocks.
     */
    @SuppressWarnings("unchecked")
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
                searchProducts(evt);  // call your real search function
            }
        });
        
        clearButton.setText("Clear");
        clearButton.setFont(new java.awt.Font("Segoe UI", 0, 14));
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSearch(evt);
            }
        });

        cartButton.setText("Cart");
        cartButton.setFont(new java.awt.Font("Segoe UI", 0, 14));
        cartButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onCart(evt);
            }
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
                .addComponent(searchButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12,12,12)
                .addComponent(clearButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(cartButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        topPanelLayout.setVerticalGroup(
            topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, topPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchButton, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearButton, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cartButton, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        topPanel.add(clearButton);

        // Product panel inside scroll pane
        productPanel.setLayout(new java.awt.GridLayout(0, 3, 16, 16)); // 3 columns, variable rows
        productPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        productPanel.setBackground(new java.awt.Color(255,255,255));

        productScrollPane.setViewportView(productPanel);

        // Main layout for the frame (GroupLayout)
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(topPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(productScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 984, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(topPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(productScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 520, Short.MAX_VALUE))
        );

        this.revalidate();
        this.repaint();
    } // </editor-fold>

    // --- Event handlers --------------------------------------------------
    private void onSearch(java.awt.event.ActionEvent evt) {
        String query = searchField.getText().trim();
        // TODO: filter productPanel contents based on query by product name
        JOptionPane.showMessageDialog(this, "Search: " + query);
    }

    private void onCart(java.awt.event.ActionEvent evt) {
        // TODO: open cart window or navigate to cart page
        parent.showPage("CART");
    }
    
    

    // ---------------------------------------------------------------------

    /**
     * Main method for standalone testing.
     */
    
}
