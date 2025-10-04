package pages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * IntroPage shows an animated title and tagline then presents Login/Register
 * buttons.
 */
public class IntroPage extends JFrame {
    private JPanel centerPanel;
    private float scale = 0.3f;
    private float alpha = 0f;
    private Timer animTimer;
    private long animStart;
    private final int ANIM_DURATION = 900; // ms

    public IntroPage() {
        setUndecorated(true);
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        centerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int h = getHeight();
                // background
                g2.setColor(new Color(250, 250, 250));
                g2.fillRect(0, 0, w, h);

                // draw title with scale and alpha
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(new Color(20, 33, 61));
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 48f * scale));
                String title = "ShopSphere";
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(title);
                g2.drawString(title, (w - tw) / 2, h / 2 - 20);

                // tagline
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f * scale));
                String tag = "Smart. Simple. Seamless Shopping.";
                FontMetrics fm2 = g2.getFontMetrics();
                int tagw = fm2.stringWidth(tag);
                g2.drawString(tag, (w - tagw) / 2, h / 2 + 20);

                g2.dispose();
            }
        };
        centerPanel.setLayout(null);
        centerPanel.setBackground(new Color(250, 250, 250));

        add(centerPanel, BorderLayout.CENTER);

        // bottom buttons
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        JButton loginBtn = new JButton("Login");
        JButton regBtn = new JButton("Register");
        loginBtn.setPreferredSize(new Dimension(120, 40));
        regBtn.setPreferredSize(new Dimension(120, 40));
        bottom.add(loginBtn);
        bottom.add(regBtn);
        add(bottom, BorderLayout.SOUTH);

        // actions
        loginBtn.addActionListener(e -> {
            Login l = new Login();
            l.setVisible(true);
            dispose();
        });
        regBtn.addActionListener(e -> {
            Register r = new Register();
            r.setVisible(true);
            dispose();
        });

        // animate: smooth scale with overshoot and settle + fade-in
        animStart = System.currentTimeMillis();
        animTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long now = System.currentTimeMillis();
                float t = Math.min(1f, (now - animStart) / (float) ANIM_DURATION);
                // easeOutBack for scale (overshoot)
                float s = easeOutBack(t);
                // map s in [0,1] to scale from 0.3 -> 1.0 with overshoot factor
                float overshoot = 1.08f; // slight overshoot
                scale = 0.3f + s * (overshoot - 0.3f);
                // alpha fade-in faster
                alpha = Math.min(1f, t * 1.2f);
                centerPanel.repaint();
                if (t >= 1f) {
                    // settle scale back to exact 1.0 with small second animation
                    animTimer.stop();
                    // run settle animation to bring scale to 1.0 smoothly
                    Timer settle = new Timer(16, null);
                    final long settleStart = System.currentTimeMillis();
                    final int settleDur = 250;
                    settle.addActionListener(ev -> {
                        float tt = Math.min(1f, (System.currentTimeMillis() - settleStart) / (float) settleDur);
                        // linear interpolate from current scale to 1.0
                        scale = scale + (1.0f - scale) * tt;
                        centerPanel.repaint();
                        if (tt >= 1f)
                            settle.stop();
                    });
                    settle.start();
                }
            }
        });
        animTimer.setInitialDelay(200);
        animTimer.start();

        setVisible(true);
    }

    // easing function: easeOutBack (overshoot)
    private float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(x - 1f, 3) + c1 * (float) Math.pow(x - 1f, 2);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IntroPage());
    }
}
