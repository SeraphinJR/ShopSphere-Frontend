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

    private JLabel photoLabel;
    private JTextField nameField;
    private JTextField emailField;
    private JButton uploadBtn;

    private BufferedImage profileImage = null;

    public ProfilePage(MainFrame parent, CartModel cartModel) {
        this.parent = parent;
        this.cartModel = cartModel;
        init();
        // apply theme styling
        try {
            Theme.styleComponentTree(this);
        } catch (Throwable ignored) {
        }
        fetchProfileAsync();
    }

    private void init() {
        // Top-level layout: vertical stack with photo centered at top
        setLayout(new BorderLayout(12, 12));
        setBackground(Color.WHITE);

        JLabel title = new JLabel("Profile", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(30, 30, 30));
        add(title, BorderLayout.NORTH);

        // Content panel - vertical
        JPanel content = new JPanel();
        content.setBackground(Color.WHITE);
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
        photoPanel.setBackground(Color.WHITE);
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
        details.setBackground(Color.WHITE);
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
        nameField.setBackground(Color.WHITE);
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
        emailField.setBackground(Color.WHITE);
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
        String[] endpoints = { "http://localhost:8080/auth/me", "http://localhost:8080/users/me" };
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
                // e.printStackTrace();
            }
        }
        return null;
    }

    private void applyProfile(JSONObject p) {
        String first = p.optString("firstName", p.optString("first", ""));
        String last = p.optString("lastName", p.optString("last", ""));
        String email = p.optString("email", p.optString("username", ""));
        // role is intentionally ignored in the UI (read-only view)
        String photoUrl = p.optString("photoUrl", p.optString("avatar", ""));

        nameField.setText((first + " " + last).trim());
        emailField.setText(email);
        // Role is intentionally not editable in the UI; keep server-side value if
        // needed.

        if (!photoUrl.isEmpty()) {
            // try to load image via HttpUtil to avoid URL deprecation issues
            SwingWorker<BufferedImage, Void> w = new SwingWorker<>() {
                @Override
                protected BufferedImage doInBackground() throws Exception {
                    try {
                        byte[] bytes = HttpUtil.getBytes(photoUrl, null);
                        if (bytes == null || bytes.length == 0)
                            return null;
                        try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(bytes)) {
                            return ImageIO.read(bis);
                        }
                    } catch (Exception ex) {
                        return null;
                    }
                }

                @Override
                protected void done() {
                    try {
                        profileImage = get();
                        photoLabel.repaint();
                    } catch (Exception ex) {
                    }
                }
            };
            w.execute();
        }
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
