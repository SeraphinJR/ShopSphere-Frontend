package pages;

import javax.swing.*;
import java.awt.*;
import model.CartModel;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private final CartModel cartModel=new CartModel();

    public MainFrame() {
        setTitle("ShopSphere");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // Add pages
        cardPanel.add(new HomePage(this,cartModel), "HOME");
        cardPanel.add(new CartPage(this,cartModel), "CART");

        add(cardPanel);
    }

    public void showPage(String pageName) {
        cardLayout.show(cardPanel, pageName);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
