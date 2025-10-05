package pages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;
import java.nio.charset.StandardCharsets;


/**
 * VendorDashboard - improved version with safe UI updates and Home refresh after changes.
 */
public class VendorDashboard extends JPanel {
    private final MainFrame parent;
    private final JPanel productsPanel;
    private final JButton refreshBtn;
    private final JButton addBtn;
    private final JLabel statusLabel;
    private final JScrollPane scroll;
    private final AtomicBoolean loading = new AtomicBoolean(false);

    public VendorDashboard(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout(8,8));
        setBackground(Color.WHITE);

        // Top bar
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(245,245,245));
        top.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
        JLabel title = new JLabel("Vendor Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        top.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        addBtn = new JButton("Add Product");
        refreshBtn = new JButton("Refresh");
        controls.add(addBtn);
        controls.add(refreshBtn);
        top.add(controls, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // center: products area (flow layout so cards wrap)
        productsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        productsPanel.setBackground(Color.WHITE);
        productsPanel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        scroll = new JScrollPane(productsPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);

        // bottom status
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(new Color(245,245,245));
        bottom.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
        statusLabel = new JLabel(" ");
        bottom.add(statusLabel, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);

        // actions
        refreshBtn.addActionListener(e -> loadProductsAsync());
        addBtn.addActionListener(e -> openAddProductDialog());

        // initial load
        loadProductsAsync();
    }

    private void setStatus(String txt) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(txt));
    }

    private void setControlsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            refreshBtn.setEnabled(enabled);
            addBtn.setEnabled(enabled);
        });
    }

    // ---------- Load products (safe, non-concurrent) ----------
    private void loadProductsAsync() {
        if (!loading.compareAndSet(false, true)) return; // already loading
        setControlsEnabled(false);
        setStatus("Loading products...");
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://localhost:8080/vendor/products");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                if (AuthManager.Token != null && !AuthManager.Token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                }
                int rc = conn.getResponseCode();
                String body = rc >= 200 && rc < 300 ? readStream(conn.getInputStream()) : readStream(conn.getErrorStream());

                if (rc >= 200 && rc < 300) {
                    JSONArray arr = new JSONArray(body);
                    SwingUtilities.invokeLater(() -> {
                        productsPanel.removeAll();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject p = arr.getJSONObject(i);
                            JPanel card = makeProductCard(p);
                            productsPanel.add(card);
                        }
                        // crucial: force viewport/layout to recompute to avoid overlap
                        productsPanel.revalidate();
                        productsPanel.repaint();
                        scroll.getViewport().invalidate();
                        scroll.revalidate();
                        scroll.repaint();
                    });
                    setStatus("Loaded " + new JSONArray(body).length() + " products.");
                } else {
                    setStatus("Failed to load products: " + rc);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to load products: " + rc + "\n" + body));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                setStatus("Error loading products");
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
                loading.set(false);
                setControlsEnabled(true);
            }
        }).start();
    }

    // ---------- Product card ----------
    private JPanel makeProductCard(JSONObject p) {
        JPanel card = new JPanel(new BorderLayout(8,6));
        card.setPreferredSize(new Dimension(260, 140));
        card.setMaximumSize(new Dimension(260, 140));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200,200,200)),
                BorderFactory.createEmptyBorder(8,8,8,8)
        ));
        card.setBackground(Color.WHITE);

        long id = p.optLong("id", -1);
        String idText = id > 0 ? String.valueOf(id) : p.optString("id", "?");
        String name = p.optString("name", "Unnamed");
        double price = p.optDouble("price", 0.0);
        String imageField = p.optString("image", null);
        boolean inStock = p.optBoolean("inStock", true);

        // left: thumbnail
        JLabel thumb = new JLabel("Img", SwingConstants.CENTER);
        thumb.setPreferredSize(new Dimension(100, 90));
        thumb.setOpaque(true);
        thumb.setBackground(new Color(245,245,245));
        thumb.setBorder(BorderFactory.createLineBorder(new Color(220,220,220)));
        thumb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        loadImageAsync(thumb, imageField, 100, 90);

        // center: name + meta
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        JLabel nameLbl = new JLabel("<html><b>" + escapeHtml(name) + "</b></html>");
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel idLbl = new JLabel("ID: " + idText + (inStock ? "" : "  (Out of stock)"));
        idLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        idLbl.setForeground(Color.DARK_GRAY);
        JLabel priceLbl = new JLabel(String.format("$ %.2f", price));
        priceLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        priceLbl.setForeground(new Color(0,120,0));
        center.add(nameLbl);
        center.add(Box.createVerticalStrut(6));
        center.add(idLbl);
        center.add(Box.createVerticalGlue());
        center.add(priceLbl);

        // right: actions
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        JButton edit = new JButton("Edit");
        edit.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JButton del = new JButton("Delete");
        del.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(edit);
        right.add(Box.createVerticalStrut(6));
        right.add(del);

        // wire actions
        edit.addActionListener(e -> openEditProductDialog(p));
        del.addActionListener(e -> {
            int conf = JOptionPane.showConfirmDialog(this, "Delete product \"" + name + "\"?", "Confirm delete", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) deleteProductAsync(id);
        });

        card.add(thumb, BorderLayout.WEST);
        card.add(center, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);

        return card;
    }

    // ---------- Image loader ----------
    private void loadImageAsync(JLabel target, String imageField, int w, int h) {
        if (imageField == null || imageField.isEmpty()) {
            target.setText("[no image]");
            return;
        }
        String encoded = URLEncoder.encode(imageField, StandardCharsets.UTF_8);
        String imageUrl = "http://localhost:8080/uploads/" + encoded;

        target.setText("[loading]");
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(imageUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                int rc = conn.getResponseCode();
                if (rc >=200 && rc < 300) {
                    BufferedImage img = ImageIO.read(conn.getInputStream());
                    if (img != null) {
                        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                        SwingUtilities.invokeLater(() -> {
                            target.setText("");
                            target.setIcon(new ImageIcon(scaled));
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> target.setText("[no image]"));
                    }
                } else {
                    SwingUtilities.invokeLater(() -> target.setText("[no image]"));
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> target.setText("[no image]"));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // ---------- Add / Edit / Delete ----------
    private void openAddProductDialog() {
        ProductFormPanel form = new ProductFormPanel(null);
        int ok = JOptionPane.showConfirmDialog(this, form, "Add Product", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            String validationError = form.validateInputs();
            if (validationError != null) {
                JOptionPane.showMessageDialog(this, validationError, "Validation error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JSONObject body = form.toJson();
            addProductAsync(body);
        }
    }

    private void openEditProductDialog(JSONObject product) {
        ProductFormPanel form = new ProductFormPanel(product);
        int ok = JOptionPane.showConfirmDialog(this, form, "Edit Product", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            String validationError = form.validateInputs();
            if (validationError != null) {
                JOptionPane.showMessageDialog(this, validationError, "Validation error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JSONObject body = form.toJson();
            // Merge edited fields into the original product JSON so server validation passes
            JSONObject merged = new JSONObject(product.toString()); // copy original
            for (Iterator<String> it = body.keys(); it.hasNext();) {
                String k = it.next();
                merged.put(k, body.get(k));
            }
            long productId = product.optLong("id", -1);
            if (productId > 0) editProductAsync(productId, merged);
        }
    }

    private void addProductAsync(JSONObject body) {
        addBtn.setEnabled(false);
        setStatus("Adding product...");
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://localhost:8080/vendor/add");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                if (AuthManager.Token != null && !AuthManager.Token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                }
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("utf-8"));
                }
                int rc = conn.getResponseCode();
                String resp = rc >= 200 && rc < 300 ? readStream(conn.getInputStream()) : readStream(conn.getErrorStream());
                System.out.println("[ADD] rc=" + rc + " resp=" + resp);
                if (rc >= 200 && rc < 300) {
                    setStatus("Product added.");
                    // Refresh home (so storefront reflects new product) and then the dashboard
                    SwingUtilities.invokeLater(() -> {
                        parent.refreshHomeIfPresent();
                        parent.showPage("HOME");
                        parent.showPage("VENDOR");
                    });
                } else {
                    setStatus("Add failed: " + rc);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to add product: " + rc + "\n" + resp));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                setStatus("Error adding product");
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
                SwingUtilities.invokeLater(() -> addBtn.setEnabled(true));
                // ensure we reload dashboard data
                loadProductsAsync();
            }
        }).start();
    }

    private void editProductAsync(long productId, JSONObject body) {
        setStatus("Updating product...");
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://localhost:8080/vendor/products/" + productId);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                if (AuthManager.Token != null && !AuthManager.Token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                }
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("utf-8"));
                }
                int rc = conn.getResponseCode();
                String resp = rc >= 200 && rc < 300 ? readStream(conn.getInputStream()) : readStream(conn.getErrorStream());
                System.out.println("[EDIT] rc=" + rc + " resp=" + resp);
                if (rc >= 200 && rc < 300) {
                    setStatus("Product updated.");
                    parent.refreshHomeIfPresent();
                    // refresh home and then vendor view to show updated product
                    SwingUtilities.invokeLater(() -> {
                        parent.showPage("HOME");
                        parent.showPage("VENDOR");
                    });
                } else {
                    setStatus("Update failed: " + rc);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to update product: " + rc + "\n" + resp));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                setStatus("Error updating product");
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
                // reload products after server call completes
                loadProductsAsync();
            }
        }).start();
    }

    private void deleteProductAsync(long productId) {
        setStatus("Deleting product...");
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://localhost:8080/vendor/delete/" + productId);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                if (AuthManager.Token != null && !AuthManager.Token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                }
                int rc = conn.getResponseCode();
                String resp = rc >= 200 && rc < 300 ? readStream(conn.getInputStream()) : readStream(conn.getErrorStream());
                System.out.println("[DELETE] rc=" + rc + " resp=" + resp);
                if (rc >= 200 && rc < 300) {
                    setStatus("Product deleted.");
                    parent.refreshHomeIfPresent();
                    // reflect change on storefront
                    SwingUtilities.invokeLater(() -> {
                        parent.showPage("HOME");
                        parent.showPage("VENDOR");
                    });
                } else {
                    setStatus("Delete failed: " + rc);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to delete: " + rc + "\n" + resp));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                setStatus("Error deleting product");
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
                loadProductsAsync();
            }
        }).start();
    }

    // ---------- Small product form panel used for Add/Edit ----------
    private static class ProductFormPanel extends JPanel {
        private final JTextField nameField = new JTextField(30);
        private final JTextField priceField = new JTextField(10);
        private final JTextField originalPriceField = new JTextField(10);
        private final JTextField categoryField = new JTextField(20);
        private final JTextField imageField = new JTextField(30);
        private final JCheckBox inStockBox = new JCheckBox("In stock", true);
        private final JTextField ratingField = new JTextField(6);
        private final JTextField reviewCountField = new JTextField(6);
        private final JTextArea descArea = new JTextArea(4, 30);
        private final JTextField featuresField = new JTextField(30); // comma separated
        private final JButton uploadImageBtn = new JButton("Upload Image");
        
        ProductFormPanel(JSONObject product) {
    setLayout(new BorderLayout(6,6));
    JPanel fields = new JPanel(new GridBagLayout());
    fields.setOpaque(false);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(4,4,4,4);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    int y = 0;
    addField(fields, gbc, y++, "Name", nameField);
    addField(fields, gbc, y++, "Price", priceField);
    addField(fields, gbc, y++, "Original Price", originalPriceField);
    addField(fields, gbc, y++, "Category", categoryField);
    
    // Image field + upload button
    gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0;
    fields.add(new JLabel("Image (url or filename):"), gbc);
    gbc.gridx = 1; gbc.weightx = 1;
    JPanel imgPanel = new JPanel(new BorderLayout(4,0));
    imgPanel.add(imageField, BorderLayout.CENTER);
    imgPanel.add(uploadImageBtn, BorderLayout.EAST);
    fields.add(imgPanel, gbc);
    y++;

    // ... rest of fields
    addField(fields, gbc, y++, "Rating", ratingField);
    addField(fields, gbc, y++, "Review Count", reviewCountField);
    addField(fields, gbc, y++, "Features (comma-separated)", featuresField);

    gbc.gridx = 0; gbc.gridy = y++; gbc.gridwidth = 2;
    descArea.setLineWrap(true);
    descArea.setWrapStyleWord(true);
    fields.add(new JLabel("Description"), gbc);
    gbc.gridy = y++;
    gbc.weightx = 1; gbc.weighty = 0.2;
    fields.add(new JScrollPane(descArea,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), gbc);

    gbc.gridy = y++; gbc.gridwidth = 2; gbc.weighty = 0;
    fields.add(inStockBox, gbc);

    add(fields, BorderLayout.CENTER);

    if (product != null) populate(product);

    // Wire upload button
    uploadImageBtn.addActionListener(e -> onUploadImage());
}   
        
        private void onUploadImage() {
    JFileChooser chooser = new JFileChooser();
    int ret = chooser.showOpenDialog(this);
    if (ret != JFileChooser.APPROVE_OPTION) return;
    File file = chooser.getSelectedFile();
    uploadImageBtn.setEnabled(false);

    new Thread(() -> {
        try {
            String uploadedFileName = multipartUpload("http://localhost:8080/vendor/upload", file);
            if (uploadedFileName != null && !uploadedFileName.isEmpty()) {
                SwingUtilities.invokeLater(() -> imageField.setText(uploadedFileName));
                JOptionPane.showMessageDialog(this, "Image uploaded successfully");
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to upload image"));
            }
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()));
        } finally {
            SwingUtilities.invokeLater(() -> uploadImageBtn.setEnabled(true));
        }
    }).start();
}

        private String multipartUpload(String urlStr, File file) throws IOException {
    String boundary = "----VendorDashboardBoundary" + System.currentTimeMillis();
    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    if (AuthManager.Token != null && !AuthManager.Token.isEmpty())
        conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);

    try (OutputStream out = conn.getOutputStream();
         PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true)) {

        // File part
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"\r\n");
        writer.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(file.getName())).append("\r\n\r\n");
        writer.flush();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) out.write(buffer, 0, read);
            out.flush();
        }
        writer.append("\r\n").flush();
        writer.append("--").append(boundary).append("--").append("\r\n").flush();
    }

    int rc = conn.getResponseCode();
    String resp = readStream(rc >= 200 && rc < 300 ? conn.getInputStream() : conn.getErrorStream());
    conn.disconnect();

    // assume backend returns JSON with { "filename": "uploads/xyz.jpg" }
    try {
        JSONObject obj = new JSONObject(resp);
        return obj.optString("filename", null);
    } catch (Exception e) {
        return null;
    }
}
        
        private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
            gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0;
            panel.add(new JLabel(label + ":"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            panel.add(field, gbc);
        }

        private void populate(JSONObject p) {
            nameField.setText(p.optString("name",""));
            priceField.setText(p.has("price") ? String.valueOf(p.optDouble("price", 0.0)) : "");
            originalPriceField.setText(p.has("originalPrice") ? String.valueOf(p.optDouble("originalPrice", p.optDouble("original_price", 0.0))) : "");
            categoryField.setText(p.optString("category",""));
            imageField.setText(p.optString("image",""));
            ratingField.setText(p.has("rating") ? String.valueOf(p.optDouble("rating",0.0)) : "");
            reviewCountField.setText(p.has("reviewCount") ? String.valueOf(p.optInt("reviewCount",0)) : "");
            descArea.setText(p.optString("description",""));
            JSONArray feats = p.optJSONArray("features");
            if (feats != null) {
                StringBuilder sb = new StringBuilder();
                for (int i=0;i<feats.length();i++){
                    if (i>0) sb.append(", ");
                    sb.append(feats.optString(i,""));
                }
                featuresField.setText(sb.toString());
            }
            inStockBox.setSelected(p.optBoolean("inStock", true));
        }

        /** Return null if OK, otherwise an error message */
        String validateInputs() {
            String name = nameField.getText().trim();
            if (name.isEmpty()) return "Product name is required.";
            String priceText = priceField.getText().trim();
            if (priceText.isEmpty()) return "Price is required.";
            try {
                Double.parseDouble(priceText);
            } catch (NumberFormatException ex) {
                return "Price must be a number.";
            }
            String ratingText = ratingField.getText().trim();
            if (!ratingText.isEmpty()) {
                try { Double.parseDouble(ratingText); } catch (NumberFormatException e) { return "Rating must be numeric."; }
            }
            String rcText = reviewCountField.getText().trim();
            if (!rcText.isEmpty()) {
                try { Integer.parseInt(rcText); } catch (NumberFormatException e) { return "Review count must be integer."; }
            }
            return null;
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            o.put("name", nameField.getText().trim());
            try { o.put("price", Double.parseDouble(priceField.getText().trim())); } catch (Exception ex) {}
            try {
                String op = originalPriceField.getText().trim();
                if (!op.isEmpty()) o.put("originalPrice", Double.parseDouble(op));
            } catch (Exception ex) {}
            o.put("category", categoryField.getText().trim());
            o.put("image", imageField.getText().trim());
            try {
                String rf = ratingField.getText().trim();
                if (!rf.isEmpty()) o.put("rating", Double.parseDouble(rf));
            } catch (Exception ex) {}
            try {
                String rc = reviewCountField.getText().trim();
                if (!rc.isEmpty()) o.put("reviewCount", Integer.parseInt(rc));
            } catch (Exception ex) {}
            o.put("description", descArea.getText().trim());
            o.put("inStock", inStockBox.isSelected());
            String feats = featuresField.getText().trim();
            if (!feats.isEmpty()) {
                JSONArray fa = new JSONArray();
                for (String s : feats.split("\\s*,\\s*")) if (!s.isEmpty()) fa.put(s);
                o.put("features", fa);
            }
            return o;
        }
    }

    

    
    

    
    
    // ---------- utilities ----------
    private static String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString().trim();
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    // ---------- small WrapLayout (flow layout that wraps) ----------
    private static class WrapLayout extends FlowLayout {
        public WrapLayout() { super(); }
        public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }
        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension d = layoutSize(target, false);
            d.width -= (getHgap() + 1);
            return d;
        }
        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + hgap*2);
                int x = 0, y = insets.top + vgap;
                int rowHeight = 0;
                int nmembers = target.getComponentCount();
                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (x == 0 || (x + d.width) <= maxWidth) {
                        if (x > 0) x += hgap;
                        x += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    } else {
                        x = d.width;
                        y += vgap + rowHeight;
                        rowHeight = d.height;
                    }
                }
                y += rowHeight;
                y += insets.bottom;
                return new Dimension(targetWidth, y);
            }
        }
    }
}
