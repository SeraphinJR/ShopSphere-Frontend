package pages;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import org.json.*;

/**
 * OrdersPanel - lists user's orders and each order's items.
 *
 * Visual tweaks:
 * - Order cards take only as much vertical space as their contents (no giant
 * fixed heights).
 * - Stronger typography: bolder headings, darker text for readability.
 * - Compact item rows so the list scrolls smoothly.
 *
 * Endpoints used:
 * - GET /orders/user -> JSONArray of orders
 * - GET /orders/{orderId}/items -> JSONArray of items for an order
 * - GET /products/{productId} -> to fetch product name if missing
 */
public class OrdersPanel extends JPanel {
    private final MainFrame parent;
    private final JPanel listPanel;
    private final JLabel statusLabel;

    public OrdersPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout(10, 10));
setBackground(new Color(245, 248, 250)); // light neutral bg

// 🔹 Top bar
JPanel top = new JPanel(new BorderLayout());
top.setBackground(new Color(30, 144, 255)); // Dodger blue bar
top.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

JLabel title = new JLabel("🛒 Your Orders");
title.setFont(new Font("Segoe UI", Font.BOLD, 22));
title.setForeground(Color.WHITE);
top.add(title, BorderLayout.WEST);

add(top, BorderLayout.NORTH);

// 🔹 Orders list area
listPanel = new JPanel();
listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
listPanel.setBackground(Color.WHITE);

JScrollPane scrollPane = new JScrollPane(listPanel);
scrollPane.setBorder(BorderFactory.createEmptyBorder());
scrollPane.getVerticalScrollBar().setUnitIncrement(16);
scrollPane.getViewport().setBackground(new Color(245, 248, 250));
add(scrollPane, BorderLayout.CENTER);

// 🔹 Bottom status bar
JPanel bottom = new JPanel(new BorderLayout());
bottom.setBackground(new Color(240, 240, 240));
bottom.setBorder(BorderFactory.createEmptyBorder(6, 15, 6, 15));

statusLabel = new JLabel(" ");
statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
statusLabel.setForeground(new Color(60, 60, 60));
bottom.add(statusLabel, BorderLayout.WEST);
add(bottom, BorderLayout.SOUTH);


        // Actions

        // initial load
        loadOrdersAsync();
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    private void loadOrdersAsync() {
        System.out.println("OrdersPanel: loadOrdersAsync called!");
    setStatus("Loading orders...");
    new Thread(() -> {
        System.out.println("start...");
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://localhost:8080/orders/");
            HttpURLConnection vconn = (HttpURLConnection) url.openConnection();
            
            vconn.setRequestMethod("GET");
            vconn.setRequestProperty("Accept", "application/json");
            if (AuthManager.Token != null && !AuthManager.Token.isEmpty()) {
                vconn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                vconn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
            }

            int rc = vconn.getResponseCode();
            System.out.println("Orders HTTP code: " + rc);


            InputStream is = (rc >= 200 && rc < 300) ? vconn.getInputStream() : vconn.getErrorStream();
            String body = readStream(is);
            System.out.println("Body: " + body);
            vconn.disconnect();

            final int statusCode = rc;
            final String respBody = body == null ? "" : body.trim();
            System.out.println("[Orders] rc=" + statusCode + " body=" + (respBody.length() > 200 ? respBody.substring(0,200) + "..." : respBody));

            if (statusCode >= 200 && statusCode < 300) {
                // Try to parse either an array or a wrapped object containing an array
                JSONArray ordersArray = null;
                try {
                    // if body starts with [ -> parse as array
                    if (respBody.startsWith("[")) {
                        ordersArray = new JSONArray(respBody);
                    } else {
                        JSONObject o = new JSONObject(respBody);
                        if (o.has("orders") && o.get("orders") instanceof JSONArray) {
                            ordersArray = o.getJSONArray("orders");
                        } else if (o.has("data") && o.get("data") instanceof JSONArray) {
                            ordersArray = o.getJSONArray("data");
                        } else if (o.has("items") && o.get("items") instanceof JSONArray) {
                            ordersArray = o.getJSONArray("items");
                        } else {
                            // maybe the response is a single object representing one order -> wrap it
                            // but safer: create empty and show a warning
                            ordersArray = new JSONArray();
                            System.err.println("[Orders] Unexpected JSON shape: " + o.toString());
                        }
                    }
                } catch (Exception pe) {
                    // parsing error
                    SwingUtilities.invokeLater(() -> {
                        System.err.println("rc:"+rc+" body: "+respBody);
                        JOptionPane.showMessageDialog(this,
                            "Failed to parse orders JSON.\nResponse code: " + statusCode + "\nBody: " + respBody,
                            "Parse error", JOptionPane.ERROR_MESSAGE);
                    });
                    setStatus("Failed to parse orders JSON");
                    return;
                }

                final JSONArray orders = ordersArray;
                SwingUtilities.invokeLater(() -> {
                    listPanel.removeAll();
                    if (orders.length() == 0) {
                        JLabel empty = new JLabel("You have no orders yet.heh");
                        empty.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
                        empty.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                        empty.setForeground(new Color(80, 80, 80));
                        listPanel.add(empty);
                    } else {
                        for (int i = 0; i < orders.length(); i++) {
                            JSONObject order = orders.getJSONObject(i);
                            JPanel card = makeOrderCard(order);
                            card.setAlignmentX(Component.LEFT_ALIGNMENT);
                            listPanel.add(card);
                            listPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                        }
                    }
                    listPanel.revalidate();
                    listPanel.repaint();
                });
                setStatus("Loaded " + orders.length() + " orders.");
            } else {
                // non-2xx
                setStatus("Failed to load orders: " + statusCode);
                final String diag = "Failed to load orders: HTTP " + statusCode + "\nResponse: " + respBody;
                System.err.println("[Orders] " + diag);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, diag, "Load error", JOptionPane.ERROR_MESSAGE));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Error loading orders");
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error loading orders: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
        } finally {
            if (conn != null) conn.disconnect();
        }
    }).start();
}


    /**
     * Create the visual card for a single order. The items area will be populated
     * asynchronously by fetching /orders/{orderId}/items.
     */
    private JPanel makeOrderCard(JSONObject order) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        // Let the card size be determined by contents (don't force a large fixed
        // height)
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Header
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        String id = order.opt("id") == null ? "?" : String.valueOf(order.opt("id"));
        String created = order.optString("orderDate", "");       // was createdAt
        String status = order.optString("status", "N/A");
        double total = order.optDouble("totalAmount", 0.0);     // was total


        JLabel left = new JLabel(String.format("Order #%s", id));
        left.setFont(new Font("Segoe UI", Font.BOLD, 15));
        left.setForeground(new Color(34, 34, 34));

        JLabel mid = new JLabel(created.isEmpty() ? "" : (" • " + created));
        mid.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        mid.setForeground(new Color(100, 100, 100));

        JPanel leftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftWrap.setOpaque(false);
        leftWrap.add(left);
        leftWrap.add(mid);
        header.add(leftWrap, BorderLayout.WEST);

        JLabel right = new JLabel(
                String.format("<html><b>%s</b> &nbsp; — &nbsp; Total: <b>$ %.2f</b></html>", status, total));
        right.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        right.setForeground(new Color(45, 45, 45));
        header.add(right, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);

        // Body placeholder
        JPanel itemsContainer = new JPanel();
        itemsContainer.setLayout(new BoxLayout(itemsContainer, BoxLayout.Y_AXIS));
        itemsContainer.setOpaque(false);
        itemsContainer.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));

        JLabel loading = new JLabel("Loading items...");
        loading.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        loading.setForeground(new Color(90, 90, 90));
        loading.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        itemsContainer.add(loading);

        card.add(itemsContainer, BorderLayout.CENTER);

        // Kick off async load of items for this order
        long orderId = order.optLong("id", -1);
        if (orderId > 0) {
            loadOrderItemsAsync(orderId, itemsContainer);
        } else {
            itemsContainer.removeAll();
            JLabel none = new JLabel("No items available.");
            none.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            itemsContainer.add(none);
        }

        return card;
    }

    /**
     * Loads items for a given order and updates itemsContainer in-place.
     * For each item, displays product name (fetched if missing), qty, unit price
     * and line total.
     */
    private void loadOrderItemsAsync(long orderId, JPanel itemsContainer) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/orders/" + orderId + "/items");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                if (AuthManager.Token != null && !AuthManager.Token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + AuthManager.Token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                }

                int rc = conn.getResponseCode();
                String body = rc >= 200 && rc < 300 ? readStream(conn.getInputStream())
                        : readStream(conn.getErrorStream());
                conn.disconnect();

                if (rc >= 200 && rc < 300) {
                    JSONArray items = new JSONArray(body);
                    SwingUtilities.invokeLater(() -> {
                        itemsContainer.removeAll();
                        if (items.length() == 0) {
                            JLabel empty = new JLabel("No items in this order.");
                            empty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                            itemsContainer.add(empty);
                        } else {
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject it = items.getJSONObject(i);
                                JPanel row = makeOrderItemRow(it);
                                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                                itemsContainer.add(row);
                                if (i < items.length() - 1) {
                                    itemsContainer.add(Box.createRigidArea(new Dimension(0, 6)));
                                    // subtle separator
                                    JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
                                    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                                    itemsContainer.add(sep);
                                    itemsContainer.add(Box.createRigidArea(new Dimension(0, 6)));
                                }
                            }
                        }
                        itemsContainer.revalidate();
                        itemsContainer.repaint();
                    });

                    // Fetch product names asynchronously for any items missing names
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject it = items.getJSONObject(i);
                        String name = it.optString("productName", it.optString("name", ""));
                        long productId = it.optLong("productId", -1);
                        final int index = i;
                        if ((name == null || name.isEmpty()) && productId > 0) {
                            new Thread(() -> {
                                try {
                                    String fetchedName = fetchProductName(productId);
                                    if (fetchedName != null) {
                                        SwingUtilities.invokeLater(() -> {
                                            // find the corresponding row in itemsContainer and update its name label
                                            int compIndex = index * 4; // row, maybe rigid/sep etc. using conservative
                                                                       // indexing is fragile; we'll search rows instead
                                            // safer approach: iterate components and update first JLabel WEST we find
                                            // in row
                                            Component[] comps = itemsContainer.getComponents();
                                            int foundRows = 0;
                                            for (Component c : comps) {
                                                if (c instanceof JPanel) {
                                                    JPanel rowPanel = (JPanel) c;
                                                    Component west = ((BorderLayout) rowPanel.getLayout())
                                                            .getLayoutComponent(BorderLayout.WEST);
                                                    if (west instanceof JLabel) {
                                                        ((JLabel) west).setText(fetchedName);
                                                    }
                                                    foundRows++;
                                                    if (foundRows > index)
                                                        break;
                                                }
                                            }
                                        });
                                    }
                                } catch (Exception ignored) {
                                }
                            }).start();
                        }
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        itemsContainer.removeAll();
                        JLabel err = new JLabel("Failed to load items: " + rc);
                        err.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                        itemsContainer.add(err);
                        itemsContainer.revalidate();
                        itemsContainer.repaint();
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    itemsContainer.removeAll();
                    JLabel err = new JLabel("Error loading items: " + ex.getMessage());
                    err.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    itemsContainer.add(err);
                    itemsContainer.revalidate();
                    itemsContainer.repaint();
                });
            }
        }).start();
    }

    /**
     * Create a compact row for an order item JSON object:
     * { id, orderId, productId, quantity, price }
     *
     * Row layout: [ name (WEST) ] [ qty × $price = $lineTotal (EAST) ]
     */
    private JPanel makeOrderItemRow(JSONObject it) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        String productName = it.optString("productName", it.optString("name", ""));
        long productId = it.optLong("productId", it.optLong("id", -1));
        int qty = it.optInt("quantity", 1);
        double price = it.optDouble("price", 0.0);

        JLabel nameLabel = new JLabel(productName.isEmpty() ? ("Product #" + productId) : productName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(new Color(0, 134, 34));
        // allow wrapping by restricting preferred width but keep height minimal
        nameLabel.setPreferredSize(new Dimension(360, 18));
        nameLabel.setMinimumSize(new Dimension(120, 18));

        double lineTotal = price * qty;
        JLabel lineLabel = new JLabel(String.format("%d × $ %.2f  =  $ %.2f", qty, price, lineTotal));
        lineLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lineLabel.setForeground(new Color(250, 50, 50));
        lineLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(nameLabel, BorderLayout.WEST);
        row.add(lineLabel, BorderLayout.EAST);

        return row;
    }

    /**
     * Fetch product name by productId. Returns null on failure.
     */
    private String fetchProductName(long productId) {
        try {
            URL purl = new URL("http://localhost:8080/products/" + productId);
            HttpURLConnection pconn = (HttpURLConnection) purl.openConnection();
            pconn.setRequestMethod("GET");
            pconn.setRequestProperty("Accept", "application/json");
            int prc = pconn.getResponseCode();
            if (prc >= 200 && prc < 300) {
                String pb = readStream(pconn.getInputStream());
                pconn.disconnect();
                JSONObject prod = new JSONObject(pb);
                return prod.optString("name", "Product #" + productId);
            } else {
                pconn.disconnect();
                return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private static String readStream(InputStream is) throws IOException {
        if (is == null)
            return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line);
            return sb.toString();
        }
    }
}
