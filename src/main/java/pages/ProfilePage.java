package pages;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private JComboBox<String> roleCombo;
    private JButton saveBtn;
    private JButton uploadBtn;

    private BufferedImage profileImage = null;

    public ProfilePage(MainFrame parent, CartModel cartModel) {
        this.parent = parent;
        this.cartModel = cartModel;
        init();
        fetchProfileAsync();
    }

    private void init() {
        setLayout(new BorderLayout(12, 12));
        setBackground(Color.WHITE);

        JLabel title = new JLabel("Profile", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setBackground(Color.WHITE);
        center.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        // Photo placeholder
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
                    // scale and clip to oval
                    BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g3 = scaled.createGraphics();
                    g3.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g3.drawImage(profileImage, 0, 0, size, size, null);
                    g3.dispose();
                    g2.setClip(new java.awt.geom.Ellipse2D.Float(x, y, size, size));
                    g2.drawImage(scaled, x, y, null);
                } else {
                    // placeholder icon/text
                    g2.setColor(new Color(150, 150, 150));
                    String s = "Photo";
                    FontMetrics fm = g2.getFontMetrics();
                    int sw = fm.stringWidth(s);
                    g2.drawString(s, (w - sw) / 2, h / 2 + fm.getAscent() / 2);
                }
                g2.dispose();
            }
        };
        photoLabel.setPreferredSize(new Dimension(160, 160));
        photoLabel.setOpaque(false);

        uploadBtn = new JButton("Upload Photo");
        uploadBtn.addActionListener(e -> onUploadPhoto());

        JPanel photoCol = new JPanel(new BorderLayout(6, 6));
        photoCol.setBackground(Color.WHITE);
        photoCol.add(photoLabel, BorderLayout.CENTER);
        photoCol.add(uploadBtn, BorderLayout.SOUTH);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 4;
        center.add(photoCol, gbc);

        // Name
        gbc.gridheight = 1;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        center.add(new JLabel("Name:"), gbc);

        nameField = new JTextField(24);
        nameField.setEditable(false);
        gbc.gridx = 2;
        center.add(nameField, gbc);

        // Email
        gbc.gridx = 1;
        gbc.gridy = 1;
        center.add(new JLabel("Email:"), gbc);

        emailField = new JTextField(24);
        emailField.setEditable(false);
        gbc.gridx = 2;
        center.add(emailField, gbc);

        // Role dropdown
        gbc.gridx = 1;
        gbc.gridy = 2;
        center.add(new JLabel("Role:"), gbc);

        roleCombo = new JComboBox<>(new String[] { "CUSTOMER", "VENDOR" });
        roleCombo.setSelectedIndex(0);
        gbc.gridx = 2;
        center.add(roleCombo, gbc);

        // Save button
        saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> onSaveProfile());
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        center.add(saveBtn, gbc);

        add(center, BorderLayout.CENTER);
    }

    private void fetchProfileAsync() {
        saveBtn.setEnabled(false);
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
                    saveBtn.setEnabled(true);
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
        String role = p.optString("role", p.optString("userType", "CUSTOMER"));
        String photoUrl = p.optString("photoUrl", p.optString("avatar", ""));

        nameField.setText((first + " " + last).trim());
        emailField.setText(email);
        if (role != null) {
            if (role.equalsIgnoreCase("vendor") || role.equalsIgnoreCase("VENDOR"))
                roleCombo.setSelectedItem("VENDOR");
            else
                roleCombo.setSelectedItem("CUSTOMER");
        }

        if (!photoUrl.isEmpty()) {
            // try to load image
            SwingWorker<BufferedImage, Void> w = new SwingWorker<>() {
                @Override
                protected BufferedImage doInBackground() throws Exception {
                    try {
                        URL u = new URL(photoUrl);
                        return ImageIO.read(u);
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

    private void onSaveProfile() {
        saveBtn.setEnabled(false);
        SwingWorker<Boolean, Void> w = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                String newRole = (String) roleCombo.getSelectedItem();
                JSONObject body = new JSONObject();
                body.put("role", newRole);
                // try PUT to /users/me then /auth/me
                String[] endpoints = { "http://localhost:8080/users/me", "http://localhost:8080/auth/me" };
                for (String ep : endpoints) {
                    try {
                        URL url = new URL(ep);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("PUT");
                        conn.setRequestProperty("Content-Type", "application/json");
                        String token = AuthManager.Token;
                        if (token != null && !token.isEmpty())
                            conn.setRequestProperty("Authorization", "Bearer " + token);
                        conn.setDoOutput(true);
                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(body.toString().getBytes("utf-8"));
                        }
                        int rc = conn.getResponseCode();
                        if (rc >= 200 && rc < 300)
                            return true;
                    } catch (Exception ex) {
                        // try next
                    }
                }
                return false;
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok)
                        JOptionPane.showMessageDialog(ProfilePage.this, "Profile updated");
                    else
                        JOptionPane.showMessageDialog(ProfilePage.this, "Failed to update profile on server");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProfilePage.this, "Error: " + ex.getMessage());
                } finally {
                    saveBtn.setEnabled(true);
                }
            }
        };
        w.execute();
    }

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
