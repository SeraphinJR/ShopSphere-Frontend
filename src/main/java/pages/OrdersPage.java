package pages;

import javax.swing.*;
import java.awt.*;
import model.CartModel;

public class OrdersPage extends JPanel {
    private MainFrame parent;
    private CartModel cartModel;

    public OrdersPage(MainFrame parent, CartModel cartModel) {
        this.parent = parent;
        this.cartModel = cartModel;
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        JLabel lbl = new JLabel("Orders", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        add(lbl, BorderLayout.NORTH);

        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setText("No orders yet. This is a placeholder page.");
        add(new JScrollPane(ta), BorderLayout.CENTER);
    }
}
