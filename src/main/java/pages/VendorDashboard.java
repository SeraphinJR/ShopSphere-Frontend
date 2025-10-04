package pages;

import javax.swing.*;
import java.awt.*;
import model.CartModel;

public class VendorDashboard extends JPanel {
    public VendorDashboard(MainFrame parent, CartModel cartModel) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        JLabel lbl = new JLabel("Vendor Dashboard", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        add(lbl, BorderLayout.NORTH);

        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setText("Vendor tools and stats will appear here (placeholder).");
        add(new JScrollPane(ta), BorderLayout.CENTER);
    }
}
