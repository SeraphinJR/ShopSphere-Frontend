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
        BillingPanel billingPanel = new BillingPanel(this, cartModel);


        // Add pages
        cardPanel.add(new HomePage(this,cartModel), "HOME");
        cardPanel.add(new CartPage(this,cartModel), "CART");
        cardPanel.add(billingPanel, "BILLING");

        add(cardPanel);
    }

    public void showPage(String pageName) {
    cardPanel.removeAll();

    switch(pageName) {
        case "BILLING":
            cardPanel.add(new BillingPanel(this,cartModel)); // new instance each time
            break;
        case "CART":
            cardPanel.add(new CartPage(this,cartModel));
            break;
        case "HOME":
            cardPanel.add(new HomePage(this,cartModel));
            break;
    }

    cardPanel.revalidate();
    cardPanel.repaint();
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
