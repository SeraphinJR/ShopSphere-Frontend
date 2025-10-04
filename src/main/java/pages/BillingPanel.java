package pages;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import model.CartModel;

/**
 * BillingPanel - shows line items, total, QR and handles order submission.
 *
 * Usage: new BillingPanel(parentMainFrame, cartModel)
 */
public class BillingPanel extends JPanel {
    private final MainFrame parent;
    private final CartModel cartModel;

    private JPanel itemsPanel;
    private JLabel totalLabel;
    private JButton proceedButton;
    private JPanel qrPanel;
    private JButton paymentCompletedBtn;

    public BillingPanel(MainFrame parent, CartModel cartModel) {
        this.parent = parent;
        this.cartModel = cartModel;
        initComponents();

        // Listen for cart changes to refresh billing view
        cartModel.addChangeListener(v -> SwingUtilities.invokeLater(() -> {
            rebuildItemsList();
            recalcTotalAsync();
        }));

        // initial build
        rebuildItemsList();
        recalcTotalAsync();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Top bar
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(245,245,245));
        JButton back = new JButton("Back to Cart");
        back.addActionListener(e -> parent.showPage("CART"));
        JLabel title = new JLabel("Checkout / Billing", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        top.add(back, BorderLayout.WEST);
        top.add(title, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // center: items list
        itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(itemsPanel);
        sp.getVerticalScrollBar().setUnitIncrement(20);
        add(sp, BorderLayout.CENTER);

        // bottom: total + buttons + qr
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        totalLabel = new JLabel("Total: $ 0.00");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        left.add(totalLabel);
        bottom.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        proceedButton = new JButton("Proceed to Pay");
        paymentCompletedBtn = new JButton("Payment Completed");
        paymentCompletedBtn.setEnabled(false); // enabled only after showing QR
        right.add(proceedButton);
        right.add(paymentCompletedBtn);
        bottom.add(right, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        // qr panel overlay (initially hidden)
        qrPanel = new JPanel(new BorderLayout());
        qrPanel.setBackground(Color.WHITE);
        qrPanel.setBorder(BorderFactory.createTitledBorder("Scan QR to pay"));
        qrPanel.setPreferredSize(new Dimension(260, 260));
        qrPanel.setVisible(false);
        add(qrPanel, BorderLayout.EAST);

        // actions
        proceedButton.addActionListener(e -> showQr());
        paymentCompletedBtn.addActionListener(e -> submitOrderAsync());
    }

    private void rebuildItemsList() {
        itemsPanel.removeAll();
        List<JSONObject> itemsSnapshot;
        synchronized (cartModel) {
            itemsSnapshot = List.copyOf(cartModel.getItems()); // snapshot to iterate safely
        }

        if (itemsSnapshot.isEmpty()) {
            JLabel empty = new JLabel("Your cart is empty.");
            empty.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
            itemsPanel.add(empty);
        } else {
            for (JSONObject item : itemsSnapshot) {
                JPanel row = makeBillingRow(item);
                itemsPanel.add(row);
                itemsPanel.add(Box.createRigidArea(new Dimension(0,8)));
            }
        }
        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    private JPanel makeBillingRow(JSONObject item) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY), BorderFactory.createEmptyBorder(8,8,8,8)
        ));

        // left: product name (we don't have product object here in model; show productId + quantity)
        String pid = item.optString("productId", item.optString("id", "n/a"));
        int qty = item.optInt("quantity", 1);
        JLabel left = new JLabel("Product ID: " + pid + "  × " + qty);
        left.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        row.add(left, BorderLayout.WEST);

        // right: price placeholder — will be updated in recalcTotalAsync if we can fetch product info
        JLabel right = new JLabel("Price: fetching...");
        row.add(right, BorderLayout.EAST);

        // attach the price label to the JSON item for later update? We'll do a quick fetch per-row in background.
        // Kick off a small background job to fetch product price/name if available
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/products/" + pid);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                int rc = conn.getResponseCode();
                if (rc >= 200 && rc < 300) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    JSONObject prod = new JSONObject(sb.toString());
                    String name = prod.optString("name", "Product " + pid);
                    double price = prod.optDouble("price", 0.0);
                    SwingUtilities.invokeLater(() -> {
                        left.setText(name + " (ID " + pid + ") × " + qty);
                        right.setText(String.format("$ %.2f each  —  Line: $ %.2f", price, price * qty));
                    });
                } else {
                    SwingUtilities.invokeLater(() -> right.setText("Price: N/A"));
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> right.setText("Price: error"));
            }
        }).start();

        return row;
    }

    /** Compute total by looping cart items and fetching each product price (async). */
    private void recalcTotalAsync() {
        new Thread(() -> {
            double tot = 0.0;
            List<JSONObject> snapshot;
            synchronized (cartModel) { snapshot = List.copyOf(cartModel.getItems()); }

            for (JSONObject item : snapshot) {
                String pid = item.optString("productId", item.optString("id", null));
                int qty = item.optInt("quantity", 1);
                if (pid == null) continue;
                try {
                    URL url = new URL("http://localhost:8080/products/" + pid);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    int rc = conn.getResponseCode();
                    if (rc >= 200 && rc < 300) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();
                        JSONObject prod = new JSONObject(sb.toString());
                        double price = prod.optDouble("price", 0.0);
                        tot += price * qty;
                    }
                } catch (Exception ignored) {}
            }
            final double totalFinal = tot;
            SwingUtilities.invokeLater(() -> totalLabel.setText(String.format("Total: $ %.2f", totalFinal)));
        }).start();
    }

    /** Simulates showing a QR — here we create a simple placeholder BufferedImage. */
    private void showQr() {
        // create a simple QR placeholder image
        BufferedImage img = new BufferedImage(240, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0,0,240,240);
        g.setColor(Color.BLACK);
        g.drawRect(0,0,239,239);
        g.setFont(new Font("Monospaced", Font.BOLD, 32));
        g.drawString("QR", 94, 120);
        g.dispose();

        qrPanel.removeAll();
        JLabel pic = new JLabel(new ImageIcon(img));
        qrPanel.add(pic, BorderLayout.CENTER);
        qrPanel.setVisible(true);
        paymentCompletedBtn.setEnabled(true);
        revalidate();
        repaint();
    }

    private void clearCart(){
        try{
            URL url=new URL("http://localhost:8080/cart");
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("content-type","application/json");
            conn.setRequestProperty("Authorization", "Bearer "+AuthManager.Token);
            conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
            conn.setDoOutput(true);
            
            int rc=conn.getResponseCode();
            if (rc>=200&&rc<300){
                System.out.print("Cleared");
            }
            else{
                System.out.print("Error:"+rc);
            }
            
            conn.disconnect();
        }catch(Exception e){}
        
    }
    
    /** Called when user confirms payment is completed. Posts /orders with items. */
    private void submitOrderAsync() {
        // disable button to avoid double clicks
        paymentCompletedBtn.setEnabled(false);
        proceedButton.setEnabled(false);

        new Thread(() -> {
            try {
                // build payload from cartModel items
                JSONArray arr = new JSONArray();
                List<JSONObject> snapshot;
                synchronized (cartModel) { snapshot = List.copyOf(cartModel.getItems()); }

                for (JSONObject it : snapshot) {
                    // we expect cart-item JSON to have productId and quantity
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
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty()) conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(arr.toString().getBytes("utf-8"));
                }

                int rc = conn.getResponseCode();
                String resp;
                try (InputStream is = (rc >= 200 && rc < 300) ? conn.getInputStream() : conn.getErrorStream()) {
                    resp = new BufferedReader(new InputStreamReader(is)).lines().reduce("", (a,b) -> a + b);
                }
                conn.disconnect();

                if (rc >= 200 && rc < 300) {
                    // success: clear cart and go to HOME
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(BillingPanel.this, "Payment successful! Order placed.");
                        clearCart();
                        cartModel.clear(); // empties the cart and notifies listeners
                        parent.showPage("HOME");
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(BillingPanel.this, "Order failed: " + rc + "\n" + resp);
                        paymentCompletedBtn.setEnabled(true);
                        proceedButton.setEnabled(true);
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(BillingPanel.this, "Error placing order: " + ex.getMessage());
                    paymentCompletedBtn.setEnabled(true);
                    proceedButton.setEnabled(true);
                });
            }
        }).start();
    }
}
