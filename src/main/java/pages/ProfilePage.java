package pages;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
// ...existing code...
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import model.CartModel;



/**
 * Profile page: shows name, email, role (dropdown), and profile photo
 * placeholder.
 * Fetches current user from backend (/auth/me or /users/me) using AuthManager
 * token.
 */
public class ProfilePage extends JPanel {
    private MainFrame parent;
    private CartModel cartModel;
    private JButton upgradeBtn;
    private String currentEmail;
    
    private JLabel photoLabel;
    private JTextField nameField;
    private JTextField emailField;
    private JButton uploadBtn;

    private BufferedImage profileImage = null;

    public ProfilePage(MainFrame parent, CartModel cartModel) {
        this.parent = parent;
        this.cartModel = cartModel;
        init();
        fetchProfileAsync();
    }

    private void init() {
        // Top-level layout: vertical stack with photo centered at top
        setLayout(new BorderLayout(12, 12));
        setBackground(UIManager.getColor("Panel.background"));

        JLabel title = new JLabel("Profile", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(30, 30, 30));
        add(title, BorderLayout.NORTH);

        // Content panel - vertical
        JPanel content = new JPanel();
        content.setBackground(UIManager.getColor("Panel.background"));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Photo area (centered)
        photoLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int w = getWidth();
                int h = getHeight();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(w, h) - 4;
                int x = (w - size) / 2;
                int y = (h - size) / 2;
                // draw oval background
                g2.setColor(new Color(230, 230, 230));
                g2.fillOval(x, y, size, size);
                // draw image if present
                if (profileImage != null) {
                    BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g3 = scaled.createGraphics();
                    g3.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g3.drawImage(profileImage, 0, 0, size, size, null);
                    g3.dispose();
                    g2.setClip(new java.awt.geom.Ellipse2D.Float(x, y, size, size));
                    g2.drawImage(scaled, x, y, null);
                } else {
                    g2.setColor(new Color(150, 150, 150));
                    String s = "Photo";
                    FontMetrics fm = g2.getFontMetrics();
                    int sw = fm.stringWidth(s);
                    g2.drawString(s, (w - sw) / 2, h / 2 + fm.getAscent() / 2);
                }
                g2.dispose();
            }
        };
        photoLabel.setPreferredSize(new Dimension(180, 180));
        photoLabel.setMaximumSize(new Dimension(180, 180));
        photoLabel.setOpaque(false);

        uploadBtn = new JButton("Upload Photo");
        uploadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        uploadBtn.addActionListener(e -> onUploadPhoto());

        JPanel photoPanel = new JPanel();
        photoPanel.setBackground(UIManager.getColor("Panel.background"));
        photoPanel.setLayout(new BoxLayout(photoPanel, BoxLayout.Y_AXIS));
        photoPanel.add(Box.createVerticalStrut(8));
        photoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        photoPanel.add(photoLabel);
        photoPanel.add(Box.createVerticalStrut(8));
        uploadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        photoPanel.add(uploadBtn);
        photoPanel.add(Box.createVerticalStrut(12));

        content.add(photoPanel);

        // Details area - compact, formal layout (labels left, fields right)
        JPanel details = new JPanel(new GridBagLayout());
        details.setBackground(UIManager.getColor("Panel.background"));
        details.setAlignmentX(Component.CENTER_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nameLabel.setForeground(new Color(40, 40, 40));
        gbc.gridx = 0;
        gbc.gridy = 0;
        details.add(nameLabel, gbc);

        nameField = new JTextField(22);
        nameField.setEditable(false);
        nameField.setBackground(UIManager.getColor("Panel.background"));
        nameField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        details.add(nameField, gbc);

        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emailLabel.setForeground(new Color(40, 40, 40));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        details.add(emailLabel, gbc);

        emailField = new JTextField(22);
        emailField.setEditable(false);
        emailField.setBackground(UIManager.getColor("Panel.background"));
        emailField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        details.add(emailField, gbc);

        // Keep name and email visually close by reducing vertical gaps
        content.add(details);

        // put content into center
        add(content, BorderLayout.CENTER);
        upgradeBtn = new JButton("Upgrade to Vendor?");
        upgradeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        upgradeBtn.addActionListener(e -> onUpgradeVendor());
        content.add(Box.createVerticalStrut(12));
        content.add(upgradeBtn);
        content.add(Box.createVerticalStrut(12));

        add(content, BorderLayout.CENTER);
    }

    private void onUpgradeVendor() {
    if (currentEmail == null || currentEmail.isEmpty()) return;

    int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to upgrade your account to Vendor?",
            "Confirm Upgrade", JOptionPane.YES_NO_OPTION);
    if (confirm != JOptionPane.YES_OPTION) return;

    upgradeBtn.setEnabled(false);

    SwingWorker<Void, Void> worker = new SwingWorker<>() {
        private String msg = "Unknown response";
        private boolean upgradeSuccess = false;

        @Override
        protected Void doInBackground() {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://localhost:8080/auth/register/vendor");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty())
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                String[] nameParts = nameField.getText().split(" ", 2);
                payload.put("firstName", nameParts.length > 0 ? nameParts[0] : "");
                payload.put("lastName", nameParts.length > 1 ? nameParts[1] : "");
                payload.put("email", currentEmail);
                payload.put("password","placeholder");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes("utf-8"));
                }

                int rc = conn.getResponseCode();
                InputStream is = (rc >= 200 && rc < 300) ? conn.getInputStream() : conn.getErrorStream();

                if (is != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject resp = new JSONObject(sb.toString());
                    if (resp.has("message")) msg = resp.getString("message");
                    else if (resp.has("msg")) msg = resp.getString("msg");
                    else msg = sb.toString();

                    upgradeSuccess = rc >= 200 && rc < 300;
                }

            } catch (Exception ex) {
                msg = ex.getMessage();
            } finally {
                if (conn != null) conn.disconnect();
            }
            return null;
        }

        @Override
        protected void done() {
            upgradeBtn.setEnabled(true);
            JOptionPane.showMessageDialog(ProfilePage.this, msg);

            if (upgradeSuccess) {
                // Logout
                try {
                    HttpURLConnection logoutConn = (HttpURLConnection) new URL("http://localhost:8080/auth/logout").openConnection();
                    logoutConn.setRequestMethod("GET");
                    String token = AuthManager.Token;
                    if (token != null && !token.isEmpty())
                        logoutConn.setRequestProperty("Authorization", "Bearer " + token);
                    logoutConn.getResponseCode(); // just trigger the request
                    logoutConn.disconnect();
                } catch (Exception ignored) {}

                // Open login page and close main frame
                SwingUtilities.invokeLater(() -> {
                    Login loginPage = new Login(); // your login frame
                    loginPage.setVisible(true);
                    parent.dispose(); // close main frame
                });
            }
        }
    };
    worker.execute();
}



    
    private void fetchProfileAsync() {
        uploadBtn.setEnabled(false);
        SwingWorker<JSONObject, Void> worker = new SwingWorker<>() {
            @Override
            protected JSONObject doInBackground() throws Exception {
                JSONObject profile = fetchProfile();
                return profile;
            }

            @Override
            protected void done() {
                try {
                    JSONObject profile = get();
                    if (profile != null)
                        applyProfile(profile);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    uploadBtn.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private JSONObject fetchProfile() {
        String[] endpoints = { "http://localhost:8080/auth/current" };
        for (String ep : endpoints) {
            try {
                URL url = new URL(ep);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                String token = AuthManager.Token;
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("Refresh-Token", AuthManager.Refresh);
                }
                int rc = conn.getResponseCode();
                InputStreamReader isr = null;
                if (rc >= 200 && rc < 300)
                    isr = new InputStreamReader(conn.getInputStream());
                else if (conn.getErrorStream() != null)
                    isr = new InputStreamReader(conn.getErrorStream());
                if (isr != null) {
                    BufferedReader in = new BufferedReader(isr);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null)
                        sb.append(line);
                    in.close();
                    try {
                        return new JSONObject(sb.toString());
                    } catch (Exception ex) {
                        System.out.println("Failed to parse profile JSON from " + ep + ": " + sb);
                    }
                }
            } catch (Exception e) {
                // try next endpoint
                 e.printStackTrace();
            }
        }
        return null;
    }

    
    
    private void applyProfile(JSONObject p) {
    if (!"success".equalsIgnoreCase(p.optString("status"))) {
        JOptionPane.showMessageDialog(this, "Failed to load profile: " + p.optString("message"));
        return;
    }

    String first = p.optString("firstName", "");
    String last = p.optString("lastName", "");
    String email = p.optString("email", "");
    currentEmail = email;

    nameField.setText((first + " " + last).trim());
    emailField.setText(email);

    // Load photo if available
    String photoUrl = p.optString("photoUrl", p.optString("avatar", ""));
    if (!photoUrl.isEmpty()) loadProfileImage(photoUrl);
}

private void loadProfileImage(String url) {
    SwingWorker<BufferedImage, Void> w = new SwingWorker<>() {
        @Override
        protected BufferedImage doInBackground() throws Exception {
            try { return ImageIO.read(new URL(url)); } catch (Exception ex) { return null; }
        }
        @Override
        protected void done() {
            try { profileImage = get(); photoLabel.repaint(); } catch (Exception ignored) {}
        }
    };
    w.execute();
}


    // onSaveProfile removed - profile is read-only in this view

    private void onUploadPhoto() {
        JFileChooser chooser = new JFileChooser();
        int ret = chooser.showOpenDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION)
            return;
        File f = chooser.getSelectedFile();
        uploadBtn.setEnabled(false);
        SwingWorker<Boolean, Void> w = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    // try multipart upload to /users/me/photo then /auth/me/photo
                    String[] endpoints = { "http://localhost:8080/users/me/photo",
                            "http://localhost:8080/auth/me/photo" };
                    for (String ep : endpoints) {
                        try {
                            boolean ok = multipartUpload(ep, f);
                            if (ok)
                                return true;
                        } catch (Exception ex) {
                            // continue
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return false;
            }

            @Override
            protected void done() {
                uploadBtn.setEnabled(true);
                try {
                    boolean ok = get();
                    if (ok) {
                        profileImage = ImageIO.read(f);
                        photoLabel.repaint();
                        JOptionPane.showMessageDialog(ProfilePage.this, "Photo uploaded");
                    } else {
                        JOptionPane.showMessageDialog(ProfilePage.this, "Failed to upload photo to server");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProfilePage.this, "Error: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private boolean multipartUpload(String urlStr, File file) throws IOException {
        String boundary = "----ShopSphereBoundary" + System.currentTimeMillis();
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        String token = AuthManager.Token;
        if (token != null && !token.isEmpty())
            conn.setRequestProperty("Authorization", "Bearer " + token);

        try (OutputStream out = conn.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true)) {

            // file part
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n");
            writer.append("Content-Type: image/jpeg\r\n\r\n");
            writer.flush();
            // stream file
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) != -1)
                    out.write(buffer, 0, read);
                out.flush();
            }
            writer.append("\r\n");
            writer.flush();

            // end
            writer.append("--").append(boundary).append("--").append("\r\n");
            writer.flush();
        }

        int rc = conn.getResponseCode();
        return rc >= 200 && rc < 300;
    }
}
