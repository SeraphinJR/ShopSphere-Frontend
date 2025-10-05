package pages;

import javax.swing.*;
import java.awt.*;

public class ContactPage extends JPanel {
    public ContactPage(MainFrame parent) {
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        try {
            Theme.styleComponentTree(this);
        } catch (Throwable ignored) {
        }

        JLabel title = new JLabel("ShopSphere", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setBorder(BorderFactory.createEmptyBorder(20, 12, 12, 12));

        JPanel center = new JPanel();
        center.setBackground(Color.BLACK);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(12, 40, 40, 40));

        JLabel phone = new JLabel("Phone: 7907374511");
        phone.setForeground(Color.WHITE);
        phone.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        phone.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel email = new JLabel("Email: shopshere2005@yahoo.com");
        email.setForeground(Color.WHITE);
        email.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        email.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel avail = new JLabel("Available: 24x7");
        avail.setForeground(Color.WHITE);
        avail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        avail.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(phone);
        center.add(Box.createVerticalStrut(8));
        center.add(email);
        center.add(Box.createVerticalStrut(8));
        center.add(avail);

        add(title, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }
}
