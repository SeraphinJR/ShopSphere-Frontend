package pages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import model.CartModel;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private final CartModel cartModel = new CartModel();
    // navigation history
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1; // -1 means no entry yet

    public MainFrame() {
        setTitle("ShopSphere");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        BillingPanel billingPanel = new BillingPanel(this, cartModel);

        // Add pages
        cardPanel.add(new HomePage(this, cartModel), "HOME");
        cardPanel.add(new CartPage(this, cartModel), "CART");
        cardPanel.add(billingPanel, "BILLING");
        cardPanel.add(new OrdersPage(this, cartModel), "ORDERS");
        cardPanel.add(new ProfilePage(this, cartModel), "PROFILE");
        cardPanel.add(new VendorDashboard(this, cartModel), "VENDOR");

        add(cardPanel);

        // setup global keybindings for back/forward
        setupKeyBindings();
    }

    /**
     * Show a page and record it in navigation history.
     */
    public void showPage(String pageName) {
        // if we're not at the end of history, drop forward entries
        if (historyIndex < history.size() - 1) {
            // remove everything after historyIndex
            while (history.size() - 1 > historyIndex)
                history.remove(history.size() - 1);
        }
        history.add(pageName);
        historyIndex = history.size() - 1;
        displayPage(pageName);
    }

    // internal method to display a page without modifying history (used for
    // back/forward)
    private void displayPage(String pageName) {
        cardPanel.removeAll();

        switch (pageName) {
            case "BILLING":
                cardPanel.add(new BillingPanel(this, cartModel)); // new instance each time
                break;
            case "CART":
                cardPanel.add(new CartPage(this, cartModel));
                break;
            case "ORDERS":
                cardPanel.add(new OrdersPage(this, cartModel));
                break;
            case "PROFILE":
                cardPanel.add(new ProfilePage(this, cartModel));
                break;
            case "VENDOR":
                cardPanel.add(new VendorDashboard(this, cartModel));
                break;
            case "HOME":
            default:
                cardPanel.add(new HomePage(this, cartModel));
                break;
        }

        cardPanel.revalidate();
        cardPanel.repaint();
    }

    /**
     * Go back in history if possible.
     */
    public void goBack() {
        if (historyIndex > 0) {
            historyIndex--;
            String page = history.get(historyIndex);
            displayPage(page);
        } else {
            // optionally beep or ignore
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Go forward in history if possible.
     */
    public void goForward() {
        if (historyIndex < history.size() - 1) {
            historyIndex++;
            String page = history.get(historyIndex);
            displayPage(page);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void setupKeyBindings() {
        JRootPane root = this.getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        // Alt + Left => go back
        im.put(KeyStroke.getKeyStroke("alt LEFT"), "goBack");
        am.put("goBack", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goBack();
            }
        });

        // Alt + Right => go forward
        im.put(KeyStroke.getKeyStroke("alt RIGHT"), "goForward");
        am.put("goForward", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goForward();
            }
        });

        // Backspace as an additional back key
        im.put(KeyStroke.getKeyStroke("BACK_SPACE"), "goBack");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
