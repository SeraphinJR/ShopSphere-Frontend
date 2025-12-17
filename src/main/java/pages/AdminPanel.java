package pages;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import org.json.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminPanel extends JPanel {
    @SuppressWarnings("unused")
    private final MainFrame parent;
    private final JTabbedPane tabbedPane;
    private JPanel usersPanel;
    private JPanel productsPanel;
    private JPanel ordersPanel;
    private JPanel reviewsPanel;
    private JPanel statsPanel;
    
    private DefaultTableModel ordersTableModel;
    private DefaultTableModel productsTableModel;
    private DefaultTableModel usersTableModel;
    private DefaultTableModel reviewsTableModel;
    
    public AdminPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        
        System.out.println("Initializing Admin Panel...");
        
        // Create tabbed pane for different admin functions
        tabbedPane = new JTabbedPane();
        
        // Initialize table models first
        createTableModels();
        
        // Initialize panels
        ordersPanel = createOrdersPanel();
        productsPanel = createProductsPanel();
        usersPanel = createUsersPanel();
        reviewsPanel = createReviewsPanel();
        statsPanel = createStatsPanel();
        
        // Add tabs
        tabbedPane.addTab("Orders", ordersPanel);
        tabbedPane.addTab("Products", productsPanel);
        tabbedPane.addTab("Users", usersPanel);
        tabbedPane.addTab("Reviews", reviewsPanel);
        tabbedPane.addTab("Statistics", statsPanel);
        
        // Add to main panel
        add(tabbedPane, BorderLayout.CENTER);
        
        // Add a refresh button at the bottom
        JButton refreshButton = new JButton("Refresh All Data");
        refreshButton.addActionListener(e -> refreshData());
        add(refreshButton, BorderLayout.SOUTH);
        
        // Now that everything is initialized, load the data
        System.out.println("Admin Panel initialized, loading initial data...");
        SwingUtilities.invokeLater(this::refreshData);
    }
    
    private void createTableModels() {
        // Orders table model
        String[] orderColumns = {"Order ID", "Customer", "Date", "Total ($)", "Status", "Actions"};
        ordersTableModel = new DefaultTableModel(orderColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only actions column is editable
            }
        };
        
        // Products table model
        String[] productColumns = {"ID", "Name", "Price", "Original Price", "Category", "Description", 
            "Rating", "Reviews", "Stock", "Vendor ID", "Actions"};
        productsTableModel = new DefaultTableModel(productColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 10; // Only actions column is editable
            }
        };
        
        // Users table model
        String[] userColumns = {"ID", "First Name", "Last Name", "Email", "Role", "Actions"};
        usersTableModel = new DefaultTableModel(userColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // Only actions column is editable
            }
        };
        
        // Reviews table model
        String[] reviewColumns = {"ID", "Product ID", "User ID", "Rating", "Review", "Actions"};
        reviewsTableModel = new DefaultTableModel(reviewColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only actions column is editable
            }
        };
    }
    
    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTable table = new JTable(usersTableModel);
        // Set preferred width for role column
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(
            new ButtonEditor(new JCheckBox(), "Edit Role", e -> {
                int row = table.getSelectedRow();
                if (row != -1) {
                    String userId = table.getValueAt(row, 0).toString();
                    String currentRole = table.getValueAt(row, 4).toString();
                    updateUserRole(userId, currentRole);
                }
            })
        );
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add User");
        JButton refreshButton = new JButton("Refresh");
        addButton.addActionListener(e -> showAddUserDialog());
        refreshButton.addActionListener(e -> refreshUsersData());
        buttonPanel.add(addButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTable table = new JTable(productsTableModel);
        table.getColumnModel().getColumn(10).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(10).setCellEditor(
            new ButtonEditor(new JCheckBox(), "Delete", e -> {
                int row = table.getSelectedRow();
                if (row != -1) {
                    String productId = table.getValueAt(row, 0).toString();
                    deleteProduct(productId);
                }
            })
        );
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Product");
        JButton refreshButton = new JButton("Refresh");
        addButton.addActionListener(e -> showAddProductDialog());
        refreshButton.addActionListener(e -> refreshProductsData());
        buttonPanel.add(addButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    

    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create stats display
        JPanel statsGrid = new JPanel(new GridLayout(3, 2, 10, 10));
        statsGrid.add(new JLabel("Total Users:"));
        JLabel usersCountLabel = new JLabel("0");
        statsGrid.add(usersCountLabel);
        
        statsGrid.add(new JLabel("Total Products:"));
        JLabel productsCountLabel = new JLabel("0");
        statsGrid.add(productsCountLabel);
        
        statsGrid.add(new JLabel("Total Orders:"));
        JLabel ordersCountLabel = new JLabel("0");
        statsGrid.add(ordersCountLabel);
        
        panel.add(statsGrid, BorderLayout.NORTH);
        
        // Add refresh button
        JButton refreshButton = new JButton("Refresh Statistics");
        refreshButton.addActionListener(e -> refreshStatistics());
        panel.add(refreshButton, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Refreshes all data in the admin panel.
     * This method is meant to be called after initialization
     * or when a global refresh is needed.
     */
    public void refreshData() {
        System.out.println("Starting data refresh for all panels...");
        
        // Create a counter for completed data loads
        AtomicInteger completedLoads = new AtomicInteger(0);
        Runnable checkCompletion = () -> {
            if (completedLoads.incrementAndGet() == 4) { // After all 4 data loads complete
                refreshStatistics(); // Update statistics
                System.out.println("Data refresh completed.");
            }
        };

        // Start all data loads in parallel
        new Thread(() -> {
            refreshUsersData();
            SwingUtilities.invokeLater(checkCompletion);
        }).start();
        
        new Thread(() -> {
            refreshProductsData();
            SwingUtilities.invokeLater(checkCompletion);
        }).start();
        
        new Thread(() -> {
            refreshOrdersData();
            SwingUtilities.invokeLater(checkCompletion);
        }).start();
        
        new Thread(() -> {
            refreshReviewsData();
            SwingUtilities.invokeLater(checkCompletion);
        }).start();
    }
    
    private void refreshUsersData() {
        if (usersTableModel == null) {
            System.err.println("Users table model not initialized");
            return;
        }
        
        new Thread(() -> {
            try {
                URL url = URI.create("http://localhost:8080/admin/users").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                
                int responseCode = conn.getResponseCode();
                System.out.println("Users Response Code: " + responseCode);
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONArray usersArray = new JSONArray(response.toString());
                    System.out.println("Fetched " + usersArray.length() + " users");
                    
                    SwingUtilities.invokeLater(() -> {
                        usersTableModel.setRowCount(0);
                        for (int i = 0; i < usersArray.length(); i++) {
                            JSONObject user = usersArray.getJSONObject(i);
                            JSONArray roles = user.getJSONArray("role");
                            
                            // Convert roles array to comma-separated string
                            StringBuilder roleStr = new StringBuilder();
                            for (int j = 0; j < roles.length(); j++) {
                                if (j > 0) roleStr.append(",");
                                roleStr.append(roles.getString(j));
                            }
                            
                            usersTableModel.addRow(new Object[]{
                                String.valueOf(user.get("id")),
                                user.getString("firstName"),
                                user.getString("lastName"),
                                user.getString("email"),
                                roleStr.toString(),
                                "Edit Role"
                            });
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(null, "Error loading users: " + e.getMessage())
                );
            }
        }).start();
    }
    
    private void refreshStatistics() {
        if (statsPanel == null) {
            System.err.println("Stats panel not initialized");
            return;
        }

        // We'll calculate statistics from data we already have in our tables
        SwingUtilities.invokeLater(() -> {
            JPanel statsGrid = (JPanel) statsPanel.getComponent(0);
            
            // Count total users
            int totalUsers = usersTableModel.getRowCount();
            ((JLabel) statsGrid.getComponent(1)).setText(String.valueOf(totalUsers));
            
            // Count total products
            int totalProducts = productsTableModel.getRowCount();
            ((JLabel) statsGrid.getComponent(3)).setText(String.valueOf(totalProducts));
            
            // Count total orders
            int totalOrders = ordersTableModel.getRowCount();
            ((JLabel) statsGrid.getComponent(5)).setText(String.valueOf(totalOrders));
            
            System.out.println("Statistics updated from table data");
        });
    }
    

    
    private String readInputStream(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    // Custom button renderer for the users table
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
    
    private void refreshProductsData() {
        if (productsPanel == null) {
            System.err.println("Products panel not initialized");
            return;
        }

        new Thread(() -> {
            try {
                URL url = URI.create("http://localhost:8080/admin/products").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                
            if (conn.getResponseCode() == 200) {
                System.out.println("Successfully fetched products data");
                String response = readInputStream(conn.getInputStream());
                JSONArray products = new JSONArray(response);                    SwingUtilities.invokeLater(() -> {
                        JTable table = (JTable) ((JScrollPane) productsPanel.getComponent(0)).getViewport().getView();
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        model.setRowCount(0);
                        
                        for (int i = 0; i < products.length(); i++) {
                            JSONObject product = products.getJSONObject(i);
                            model.addRow(new Object[]{
                                String.valueOf(product.get("id")),
                                product.getString("name"),
                                String.format("$%.2f", product.getDouble("price")),
                                String.format("$%.2f", product.optDouble("originalPrice", 0.0)),
                                product.optString("category", "N/A"),
                                product.optString("description", ""),
                                String.format("%.1f", product.optDouble("rating", 0.0)),
                                product.optInt("reviewCount", 0),
                                product.optBoolean("inStock", false) ? "Yes" : "No",
                                String.valueOf(product.opt("vendorId")),
                                "Delete"
                            });
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage())
                );
            }
        }).start();
    }
    
    private void showAddProductDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Product", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);
        
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Add form fields
        JTextField nameField = new JTextField(30);
        JTextField priceField = new JTextField(30);
        JTextField originalPriceField = new JTextField(30);
        JTextField categoryField = new JTextField(30);
        JTextArea descriptionField = new JTextArea(4, 30);
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(descriptionField);
        
        JTextField imageField = new JTextField(30);
        JCheckBox inStockCheckbox = new JCheckBox("In Stock");
        inStockCheckbox.setSelected(true);
        
        JTextField featuresField = new JTextField(30);
        
        int gridy = 0;
        
        // Name
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        form.add(nameField, gbc);
        
        // Price
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1;
        form.add(priceField, gbc);
        
        // Original Price
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Original Price:"), gbc);
        gbc.gridx = 1;
        form.add(originalPriceField, gbc);
        
        // Category
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        form.add(categoryField, gbc);
        
        // Description
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        form.add(descScrollPane, gbc);
        
        // Image URL
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Image URL:"), gbc);
        gbc.gridx = 1;
        form.add(imageField, gbc);
        
        // Stock Status
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Stock Status:"), gbc);
        gbc.gridx = 1;
        form.add(inStockCheckbox, gbc);
        
        // Features
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Features (comma-separated):"), gbc);
        gbc.gridx = 1;
        form.add(featuresField, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            try {
                // Validate required inputs
                String name = nameField.getText().trim();
                String category = categoryField.getText().trim();
                String description = descriptionField.getText().trim();
                
                if (name.isEmpty() || category.isEmpty() || description.isEmpty()) {
                    throw new IllegalArgumentException("Name, category, and description are required");
                }
                
                // Validate numeric inputs
                double price = Double.parseDouble(priceField.getText().trim());
                double originalPrice = Double.parseDouble(originalPriceField.getText().trim());
                
                if (price < 0 || originalPrice < 0) {
                    throw new IllegalArgumentException("Prices cannot be negative");
                }
                
                // Create product object
                JSONObject product = new JSONObject();
                product.put("name", name);
                product.put("price", price);
                product.put("originalPrice", originalPrice);
                product.put("category", category);
                product.put("description", description);
                product.put("image", imageField.getText().trim());
                product.put("inStock", inStockCheckbox.isSelected());
                // Split features by comma and create a JSON array
                JSONArray features = new JSONArray();
                String[] featureList = featuresField.getText().trim().split(",");
                for (String feature : featureList) {
                    if (!feature.trim().isEmpty()) {
                        features.put(feature.trim());
                    }
                }
                product.put("features", features);
                
                // Add product
                addProduct(product);
                dialog.dispose();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Please enter valid numbers for prices",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog,
                    ex.getMessage(),
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Add numeric validation for price fields
        KeyAdapter numericValidator = new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!((c >= '0' && c <= '9') || c == '.' || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE)) {
                    e.consume();
                }
            }
        };
        priceField.addKeyListener(numericValidator);
        originalPriceField.addKeyListener(numericValidator);

        dialog.add(new JScrollPane(form), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private void addProduct(JSONObject product) {
        new Thread(() -> {
            try {
                URL url = URI.create("http://localhost:8080/admin/products").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                JSONArray products = new JSONArray();
                products.put(product);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(products.toString().getBytes(StandardCharsets.UTF_8));
                }
                
                if (conn.getResponseCode() == 201) {
                    SwingUtilities.invokeLater(this::refreshProductsData);
                } else {
                    throw new IOException("Server returned code: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Error adding product: " + e.getMessage())
                );
            }
        }).start();
    }
    
    private void deleteProduct(String productId) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this product?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try {
                    URL url = URI.create("http://localhost:8080/admin/products/" + productId).toURL();
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                    
                    if (conn.getResponseCode() == 200) {
                        SwingUtilities.invokeLater(this::refreshProductsData);
                    } else {
                        throw new IOException("Server returned code: " + conn.getResponseCode());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(this, "Error deleting product: " + e.getMessage())
                    );
                }
            }).start();
        }
    }

    // Custom button editor for the tables
    private class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        @SuppressWarnings("unused")
        private boolean isPushed;
        private ActionListener actionListener;
        
        public ButtonEditor(JCheckBox checkBox, String defaultLabel, ActionListener listener) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            this.label = defaultLabel;
            this.actionListener = listener;
            button.addActionListener(e -> {
                fireEditingStopped();
                actionListener.actionPerformed(e);
            });
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            isPushed = false;
            return label;
        }
        
        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTable table = new JTable(ordersTableModel);
        table.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(
            new ButtonEditor(new JCheckBox(), "Update Status", e -> {
                int row = table.getSelectedRow();
                if (row != -1) {
                    String orderId = table.getValueAt(row, 0).toString();
                    String currentStatus = table.getValueAt(row, 4).toString();
                    updateOrderStatus(orderId, currentStatus);
                }
            })
        );
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add refresh button
        JButton refreshButton = new JButton("Refresh Orders");
        refreshButton.addActionListener(e -> refreshOrdersData());
        panel.add(refreshButton, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void updateOrderStatus(String orderId, String currentStatus) {
        String[] statuses = {"PENDING", "PAYMENT_PENDING", "PAYMENT_COMPLETED"};
        String newStatus = (String) JOptionPane.showInputDialog(
            this,
            "Select new status for order: " + orderId,
            "Update Order Status",
            JOptionPane.QUESTION_MESSAGE,
            null,
            statuses,
            currentStatus
        );
        
        if (newStatus != null && !newStatus.equals(currentStatus)) {
            try {
                String token = AuthManager.Token;
                if (token == null) {
                    JOptionPane.showMessageDialog(this, "Please log in first", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                URL url = URI.create("http://localhost:8080/admin/orders/update/" + orderId).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                conn.setDoOutput(true);

                // Get current order details from the table
                JTable table = (JTable) ((JScrollPane) ordersPanel.getComponent(0)).getViewport().getView();
                int row = -1;
                for (int i = 0; i < table.getRowCount(); i++) {
                    if (orderId.equals(table.getValueAt(i, 0).toString())) {
                        row = i;
                        break;
                    }
                }
                
                if (row == -1) {
                    throw new IllegalStateException("Could not find order details in table");
                }
                
                // Create JSON payload with all required fields
                JSONObject jsonPayload = new JSONObject();
                jsonPayload.put("userId", Long.parseLong(table.getValueAt(row, 1).toString())); // User ID
                jsonPayload.put("orderDate", table.getValueAt(row, 2).toString()); // Order date
                jsonPayload.put("totalAmount", new BigDecimal(table.getValueAt(row, 3).toString())); // Total amount
                jsonPayload.put("status", newStatus); // New status
                
                System.out.println("Sending order update: " + jsonPayload.toString());
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonPayload.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                
                // Read response body regardless of success/failure
                String responseBody = "";
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader((responseCode >= 400) ? conn.getErrorStream() : conn.getInputStream()))) {
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    responseBody = response.toString();
                }
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    JOptionPane.showMessageDialog(this, "Order status updated successfully!");
                    refreshOrdersData();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update order status: " + responseBody, 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void refreshOrdersData() {
        if (ordersPanel == null) {
            System.err.println("Orders panel not initialized");
            return;
        }
        
        try {
            String token = AuthManager.Token;
            if (token == null) {
                JOptionPane.showMessageDialog(this, "Please log in first", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            URL url = URI.create("http://localhost:8080/admin/orders").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            
            System.out.println("Fetching orders with token: " + token);
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                // Parse JSON response and update table
                JSONArray ordersArray = new JSONArray(response.toString());
                DefaultTableModel model = (DefaultTableModel) ((JTable) ((JScrollPane) ordersPanel.getComponent(0)).getViewport().getView()).getModel();
                model.setRowCount(0);

                for (int i = 0; i < ordersArray.length(); i++) {
                    JSONObject order = ordersArray.getJSONObject(i);
                    model.addRow(new Object[]{
                        String.valueOf(order.get("id")),
                        String.valueOf(order.get("userId")),
                        order.getString("orderDate"),
                        order.getDouble("totalAmount"),
                        order.getString("status"),
                        "Update Status"
                    });
                }
            } else {
                JOptionPane.showMessageDialog(this, "Failed to fetch orders", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createReviewsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTable table = new JTable(reviewsTableModel);
        // Add Delete button
        table.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(
            new ButtonEditor(new JCheckBox(), "Delete", e -> {
                int row = table.getSelectedRow();
                if (row != -1) {
                    String reviewId = table.getValueAt(row, 0).toString();
                    deleteReview(reviewId);
                }
            })
        );
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshReviewsData());
        panel.add(refreshButton, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void refreshReviewsData() {
        if (reviewsPanel == null) {
            System.err.println("Reviews panel not initialized");
            return;
        }

        try {
            String token = AuthManager.Token;
            if (token == null) {
                JOptionPane.showMessageDialog(this, "Please log in first", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            URL url = URI.create("http://localhost:8080/admin/reviews").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                // Parse JSON response and update table
                JSONArray reviewsArray = new JSONArray(response.toString());
                DefaultTableModel model = (DefaultTableModel) ((JTable) ((JScrollPane) reviewsPanel.getComponent(0)).getViewport().getView()).getModel();
                model.setRowCount(0);

                for (int i = 0; i < reviewsArray.length(); i++) {
                    JSONObject review = reviewsArray.getJSONObject(i);
                    model.addRow(new Object[]{
                        String.valueOf(review.get("id")),
                        String.valueOf(review.get("productId")),
                        String.valueOf(review.get("userId")),
                        review.getInt("rating"),
                        review.getString("review"),
                        "Delete"
                    });
                }
            } else {
                JOptionPane.showMessageDialog(this, "Failed to fetch reviews", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteReview(String reviewId) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this review?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String token = AuthManager.Token;
                if (token == null) {
                    JOptionPane.showMessageDialog(this, "Please log in first", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                URL url = URI.create("http://localhost:8080/admin/reviews/" + reviewId).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    JOptionPane.showMessageDialog(this, "Review deleted successfully!");
                    refreshReviewsData();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete review", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAddUserDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add User", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Add form fields
        JTextField firstNameField = new JTextField(20);
        JTextField lastNameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JComboBox<String> roleBox = new JComboBox<>(new String[]{
            "CUSTOMER",
            "CUSTOMER,VENDOR",
            "CUSTOMER,VENDOR,ADMIN"
        });
        
        int gridy = 0;
        
        // First Name
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        form.add(firstNameField, gbc);
        
        // Last Name
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        form.add(lastNameField, gbc);
        
        // Email
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        form.add(emailField, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        form.add(passwordField, gbc);
        
        // Role
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        form.add(roleBox, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            try {
                // Validate inputs
                String firstName = firstNameField.getText().trim();
                String lastName = lastNameField.getText().trim();
                String email = emailField.getText().trim();
                String password = new String(passwordField.getPassword());
                String role = (String) roleBox.getSelectedItem();
                
                if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    throw new IllegalArgumentException("All fields are required");
                }
                
                // Create user object
                JSONObject user = new JSONObject();
                user.put("firstName", firstName);
                user.put("lastName", lastName);
                user.put("email", email);
                user.put("password", password);
                
                // Create role array based on selection
                JSONArray roles = new JSONArray();
                String[] selectedRoles = role.split(",");
                for (String r : selectedRoles) {
                    roles.put(r.trim());
                }
                user.put("role", roles);
                
                // Add user
                addUser(user);
                dialog.dispose();
                
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog,
                    ex.getMessage(),
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(new JScrollPane(form), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private void addUser(JSONObject user) {
        new Thread(() -> {
            try {
                URL url = URI.create("http://localhost:8080/admin/users").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(user.toString().getBytes(StandardCharsets.UTF_8));
                }
                
                if (conn.getResponseCode() == 201) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "User added successfully!");
                        refreshUsersData();
                    });
                } else {
                    throw new IOException("Server returned code: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Error adding user: " + e.getMessage())
                );
            }
        }).start();
    }
    
    private void showAddReviewDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Review", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Add form fields
        JTextField userIdField = new JTextField(20);
        JTextField productIdField = new JTextField(20);
        JSpinner ratingSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
        JTextArea reviewField = new JTextArea(5, 20);
        reviewField.setLineWrap(true);
        reviewField.setWrapStyleWord(true);
        JScrollPane reviewScroll = new JScrollPane(reviewField);
        
        int gridy = 0;
        
        // User ID
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("User ID:"), gbc);
        gbc.gridx = 1;
        form.add(userIdField, gbc);
        
        // Product ID
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Product ID:"), gbc);
        gbc.gridx = 1;
        form.add(productIdField, gbc);
        
        // Rating
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Rating (1-5):"), gbc);
        gbc.gridx = 1;
        form.add(ratingSpinner, gbc);
        
        // Review
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Review:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(reviewScroll, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            try {
                // Validate inputs
                String userId = userIdField.getText().trim();
                String productId = productIdField.getText().trim();
                int rating = (Integer) ratingSpinner.getValue();
                String reviewText = reviewField.getText().trim();
                
                if (userId.isEmpty() || productId.isEmpty() || reviewText.isEmpty()) {
                    throw new IllegalArgumentException("All fields are required");
                }
                
                // Create review object
                JSONObject review = new JSONObject();
                review.put("productId", Integer.parseInt(productId)); // Convert to integer
                review.put("rating", rating);
                review.put("review", reviewText); // Use 'review' field name
                
                // Add review for the specified user
                addReview(review, userId);
                dialog.dispose();
                
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog,
                    ex.getMessage(),
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(new JScrollPane(form), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private void showEditReviewDialog(String reviewId, String userId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Review", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Add form fields
        JTextField productIdField = new JTextField(20);
        JSpinner ratingSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
        JTextArea reviewField = new JTextArea(5, 20);
        reviewField.setLineWrap(true);
        reviewField.setWrapStyleWord(true);
        JScrollPane reviewScroll = new JScrollPane(reviewField);
        
        int gridy = 0;
        
        // Product ID
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Product ID:"), gbc);
        gbc.gridx = 1;
        form.add(productIdField, gbc);
        
        // Rating
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Rating (1-5):"), gbc);
        gbc.gridx = 1;
        form.add(ratingSpinner, gbc);
        
        // Review
        gbc.gridx = 0; gbc.gridy = gridy++;
        form.add(new JLabel("Review:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(reviewScroll, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            try {
                // Validate inputs
                String productId = productIdField.getText().trim();
                int rating = (Integer) ratingSpinner.getValue();
                String reviewText = reviewField.getText().trim();
                
                if (productId.isEmpty() || reviewText.isEmpty()) {
                    throw new IllegalArgumentException("All fields are required");
                }
                
                // Create review object
                JSONObject review = new JSONObject();
                review.put("productId", productId);
                review.put("rating", rating);
                review.put("text", reviewText); // Using 'text' instead of 'review' to match backend
                
                // Update review
                updateReview(reviewId, userId, review);
                dialog.dispose();
                
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog,
                    ex.getMessage(),
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(new JScrollPane(form), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void updateReview(String reviewId, String userId, JSONObject review) {
        new Thread(() -> {
            try {
                // First try user-specific update
                URL url = URI.create("http://localhost:8080/admin/reviews/users/" + userId).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                // Create review object matching backend format
                JSONObject reviewObj = new JSONObject();
                reviewObj.put("productId", review.getInt("productId")); // Note: productId as integer
                reviewObj.put("rating", review.getInt("rating"));
                reviewObj.put("review", review.getString("review")); // Use 'review' field name
                reviewObj.put("date", java.time.OffsetDateTime.now().toString()); // Add current timestamp
                if (reviewId != null) {
                    reviewObj.put("id", reviewId);
                }
                
                // Log the request payload
                System.out.println("Sending review update request: " + reviewObj.toString());
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(reviewObj.toString().getBytes(StandardCharsets.UTF_8));
                }
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Review updated successfully!");
                        refreshReviewsData();
                    });
                } else if (responseCode == 404) {
                    // If user-specific update fails, try direct review update
                    url = URI.create("http://localhost:8080/admin/reviews/" + reviewId + "/").toURL();
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                    conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    
                    try (OutputStream os = conn.getOutputStream()) {
                        // Create request array with single review
                        JSONArray reviewArray = new JSONArray();
                        reviewArray.put(review);
                        os.write(reviewArray.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    
                    if (conn.getResponseCode() == 200) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "Review updated successfully!");
                            refreshReviewsData();
                        });
                    } else {
                        throw new IOException("Server returned code: " + conn.getResponseCode());
                    }
                } else {
                    throw new IOException("Server returned code: " + responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Error updating review: " + e.getMessage())
                );
            }
        }).start();
    }

    private void addReview(JSONObject review, String userId) {
        new Thread(() -> {
            try {
                // Remove any trailing slash from userId
                String cleanUserId = userId.endsWith("/") ? userId.substring(0, userId.length() - 1) : userId;
                URL url = URI.create("http://localhost:8080/admin/reviews/users/" + cleanUserId).toURL();
                System.out.println("Making request to URL: " + url);
                
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                
                // Ensure we have a valid token
                if (AuthManager.Token == null) {
                    throw new IllegalStateException("No authentication token available");
                }
                
                // Set all required headers
                String authHeader = "Bearer " + AuthManager.Token;
                System.out.println("Using Authorization header: " + authHeader);
                conn.setRequestProperty("Authorization", authHeader);
                conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                System.out.println("Request headers:");
                conn.getRequestProperties().forEach((key, value) -> 
                    System.out.println(key + ": " + value));
                
                // Create review object matching backend format
                JSONObject reviewObj = new JSONObject();
                reviewObj.put("productId", review.getInt("productId")); // Note: productId as integer
                reviewObj.put("rating", review.getInt("rating"));
                reviewObj.put("review", review.getString("review")); // Use 'review' instead of 'text'
                reviewObj.put("date", java.time.OffsetDateTime.now().toString()); // Add current timestamp
                
                // Log the request payload
                System.out.println("Sending review request: " + reviewObj.toString());
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(reviewObj.toString().getBytes(StandardCharsets.UTF_8));
                }
                
                int responseCode = conn.getResponseCode();
                System.out.println("Response code: " + responseCode);
                
                // Read the response body for both success and error cases
                InputStream inputStream = (responseCode >= 400) 
                    ? conn.getErrorStream() 
                    : conn.getInputStream();
                    
                String responseBody = "";
                if (inputStream != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                        responseBody = response.toString();
                    }
                }
                System.out.println("Response body: " + responseBody);
                
                if (responseCode == 201) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Review added successfully!");
                        refreshReviewsData();
                    });
                } else {
                    throw new IOException("Server returned code: " + responseCode + ", Response: " + responseBody);
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Error adding review: " + e.getMessage())
                );
            }
        }).start();
    }
    
    private void updateUserRole(String userId, String currentRole) {
        // Show role selection dialog with combined roles
        String[] roles = {
            "CUSTOMER",
            "CUSTOMER,VENDOR",
            "CUSTOMER,VENDOR,ADMIN"
        };
        String newRole = (String) JOptionPane.showInputDialog(
            this,
            "Select new role for user " + userId,
            "Update User Role",
            JOptionPane.QUESTION_MESSAGE,
            null,
            roles,
            currentRole
        );

        if (newRole != null && !newRole.equals(currentRole)) {
            new Thread(() -> {
                try {
                    URL url = URI.create("http://localhost:8080/admin/users/" + userId + "/role").toURL();
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                    conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("role", newRole);
                    
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "User role updated successfully!");
                            refreshUsersData();
                        });
                    } else {
                        throw new IOException("Server returned code: " + responseCode);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(this, "Error updating user role: " + e.getMessage())
                    );
                }
            }).start();
        }
    }
}