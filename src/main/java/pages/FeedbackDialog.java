package pages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Modal feedback dialog - mock only, does not save data.
 */
public class FeedbackDialog extends JDialog {
    public FeedbackDialog(JFrame owner) {
        super(owner, "Feedback (optional)", true);
        initUI();
        try {
            Theme.styleComponentTree(this.getContentPane());
        } catch (Throwable ignored) {
        }
        setSize(480, 300);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.BLACK);
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel h = new JLabel("We'd love your feedback", SwingConstants.CENTER);
        h.setForeground(Color.WHITE);
        h.setFont(new Font("Segoe UI", Font.BOLD, 18));
        p.add(h, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setBackground(Color.BLACK);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextArea comment = new JTextArea(4, 40);
        comment.setLineWrap(true);
        comment.setWrapStyleWord(true);
        comment.setBackground(Color.DARK_GRAY);
        comment.setForeground(Color.WHITE);
        comment.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comment.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        JPanel stars = new JPanel(new FlowLayout(FlowLayout.CENTER));
        stars.setBackground(Color.BLACK);
        ButtonGroup bg = new ButtonGroup();
        for (int i = 1; i <= 5; i++) {
            JToggleButton b = new JToggleButton("★");
            b.setForeground(Color.WHITE);
            b.setBackground(Color.BLACK);
            b.setBorder(BorderFactory.createLineBorder(Color.WHITE));
            b.setFocusPainted(false);
            b.setActionCommand(String.valueOf(i));
            bg.add(b);
            stars.add(b);
        }

        center.add(stars);
        center.add(Box.createVerticalStrut(8));
        center.add(new JScrollPane(comment));

        p.add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(Color.BLACK);
        JButton skip = new JButton("Skip");
        JButton submit = new JButton("Submit");
        skip.addActionListener(e -> {
            dispose();
        });
        submit.addActionListener(e -> {
            // mock: accept and show thank you
            JOptionPane.showMessageDialog(this, "Thanks for your feedback!", "Thank you",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });
        actions.add(skip);
        actions.add(submit);
        p.add(actions, BorderLayout.SOUTH);

        setContentPane(p);
    }
}
