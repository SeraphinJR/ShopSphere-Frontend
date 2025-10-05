package pages;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import org.json.*;
import java.nio.charset.StandardCharsets;

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
        String[] productColumns = {"ID", "Name", "Price", "Stock", "Category", "Actions"};
        productsTableModel = new DefaultTableModel(productColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only actions column is editable
            }
        };
        
        // Users table model
        String[] userColumns = {"ID", "Username", "Email", "Role", "Actions"};
        usersTableModel = new DefaultTableModel(userColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // Only actions column is editable
            }
        };
        
        // Reviews table model
        String[] reviewColumns = {"ID", "Product", "User", "Rating", "Comment", "Actions"};
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
        table.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(4).setCellEditor(
            new ButtonEditor(new JCheckBox(), "Edit Role", e -> {
                int row = table.getSelectedRow();
                if (row != -1) {
                    String userId = table.getValueAt(row, 0).toString();
                    String currentRole = table.getValueAt(row, 3).toString();
                    updateUserRole(userId, currentRole);
                }
            })
        );
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add refresh button
        JButton refreshButton = new JButton("Refresh Users");
        refreshButton.addActionListener(e -> refreshUsersData());
        panel.add(refreshButton, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTable table = new JTable(productsTableModel);
        table.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(
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
        refreshUsersData();
        refreshProductsData();
        refreshOrdersData();
        refreshReviewsData();
        refreshStatistics();
        System.out.println("Data refresh completed.");
    }
    
    private void refreshUsersData() {
        if (usersPanel == null) {
            System.err.println("Users panel not initialized");
            return;
        }

        new Thread(() -> {
            try {
                URL url = URI.create("http://localhost:8080/admin/users").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                
                if (conn.getResponseCode() == 200) {
                    String response = readInputStream(conn.getInputStream());
                    JSONArray users = new JSONArray(response);
                    
                    SwingUtilities.invokeLater(() -> {
                        DefaultTableModel model = (DefaultTableModel) ((JTable) ((JScrollPane) usersPanel.getComponent(0)).getViewport().getView()).getModel();
                        model.setRowCount(0);
                        
                        for (int i = 0; i < users.length(); i++) {
                            JSONObject user = users.getJSONObject(i);
                            model.addRow(new Object[]{
                                user.getString("id"),
                                user.getString("username"),
                                user.getString("email"),
                                user.getString("role"),
                                "Edit"
                            });
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Error loading users: " + e.getMessage())
                );
            }
        }).start();
    }
    

    
    private void refreshStatistics() {
        if (statsPanel == null) {
            System.err.println("Stats panel not initialized");
            return;
        }

        new Thread(() -> {
            try {
                URL url = URI.create("http://localhost:8080/admin/statistics").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                
                if (conn.getResponseCode() == 200) {
                    String response = readInputStream(conn.getInputStream());
                    JSONObject stats = new JSONObject(response);
                    
                    SwingUtilities.invokeLater(() -> {
                        JPanel statsGrid = (JPanel) statsPanel.getComponent(0);
                        ((JLabel) statsGrid.getComponent(1)).setText(String.valueOf(stats.getInt("totalUsers")));
                        ((JLabel) statsGrid.getComponent(3)).setText(String.valueOf(stats.getInt("totalProducts")));
                        ((JLabel) statsGrid.getComponent(5)).setText(String.valueOf(stats.getInt("totalOrders")));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Error loading statistics: " + e.getMessage())
                );
            }
        }).start();
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
                                product.getString("id"),
                                product.getString("name"),
                                String.format("$%.2f", product.getDouble("price")),
                                product.getInt("stock"),
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
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Add form fields
        JTextField nameField = new JTextField(20);
        JTextField priceField = new JTextField(20);
        JTextField stockField = new JTextField(20);
        JTextField descriptionField = new JTextField(20);
        
        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        form.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1;
        form.add(priceField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Stock:"), gbc);
        gbc.gridx = 1;
        form.add(stockField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        form.add(descriptionField, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            try {
                // Validate inputs
                String name = nameField.getText().trim();
                double price = Double.parseDouble(priceField.getText().trim());
                int stock = Integer.parseInt(stockField.getText().trim());
                String description = descriptionField.getText().trim();
                
                if (name.isEmpty() || description.isEmpty()) {
                    throw new IllegalArgumentException("All fields are required");
                }
                
                // Create product object
                JSONObject product = new JSONObject();
                product.put("name", name);
                product.put("price", price);
                product.put("stock", stock);
                product.put("description", description);
                
                // Add product
                addProduct(product);
                dialog.dispose();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Price and stock must be valid numbers",
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
        
        dialog.add(form, BorderLayout.CENTER);
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
        String[] statuses = {"PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"};
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

                URL url = URI.create("http://localhost:8080/admin/orders/" + orderId + "/status").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                // Create JSON payload
                String jsonInput = "{\"status\":\"" + newStatus + "\"}";
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    JOptionPane.showMessageDialog(this, "Order status updated successfully!");
                    refreshOrdersData();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update order status", "Error", JOptionPane.ERROR_MESSAGE);
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
                        order.getString("id"),
                        order.getString("userId"),
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
        JButton refreshButton = new JButton("Refresh Reviews");
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
                        review.getString("id"),
                        review.getString("userId"),
                        review.getString("productId"),
                        review.getInt("rating"),
                        review.getString("comment"),
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

    private void updateUserRole(String userId, String newRole) {
        new Thread(() -> {
            try {
                URL url = URI.create("http://localhost:8080/api/admin/users/" + userId + "/role").toURL();
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
                
                if (conn.getResponseCode() == 200) {
                    SwingUtilities.invokeLater(this::refreshUsersData);
                } else {
                    throw new IOException("Server returned code: " + conn.getResponseCode());
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