package pages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import model.CartModel;

/**
 * MainFrame - keeps single instances of pages, uses CardLayout.show(...)
 * and provides helpers to refresh home quietly.
 */
public class MainFrame extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final JToolBar navBar;
    private final JButton backBtn;
    private final JButton forwardBtn;
    private final JButton homeBtn;
    private final JLabel pageLabel;
    private final CartModel cartModel = new CartModel();

    // pages map keeps single instances
    private final Map<String, JPanel> pages = new HashMap<>();

    // navigation history
    private final java.util.List<String> history = new ArrayList<>();
    private int historyIndex = -1;

    public MainFrame() {
        setTitle("ShopSphere");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // nav bar
        navBar = new JToolBar();
        navBar.setFloatable(false);
        backBtn = new JButton("◀");
        forwardBtn = new JButton("▶");
        homeBtn = new JButton("🏠");
        pageLabel = new JLabel("Home");
        backBtn.setToolTipText("Back");
        forwardBtn.setToolTipText("Forward");
        homeBtn.setToolTipText("Home");
        backBtn.addActionListener(e -> goBack());
        forwardBtn.addActionListener(e -> goForward());
        homeBtn.addActionListener(e -> showPage("HOME"));
        navBar.add(backBtn);
        navBar.add(forwardBtn);
        navBar.addSeparator();
        navBar.add(homeBtn);
        navBar.addSeparator(new Dimension(12, 0));
        navBar.add(pageLabel);

        // create pages once and add to cardPanel
        pages.put("HOME", new HomePage(this, cartModel));
        pages.put("CART", new CartPage(this, cartModel));
        pages.put("BILLING", new BillingPanel(this, cartModel));
        pages.put("ORDERS", new OrdersPanel(this));
        pages.put("PROFILE", new ProfilePage(this, cartModel));
        pages.put("VENDOR", new VendorDashboard(this));

        for (Map.Entry<String, JPanel> e : pages.entrySet()) {
            cardPanel.add(e.getValue(), e.getKey());
        }

        setLayout(new BorderLayout());
        add(navBar, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);

        // show home as initial page and record it in history
        showPage("HOME");

        // keyboard shortcuts
        setupKeyBindings();
    }

    /**
     * Shows a page and appends to history.
     */
    public void showPage(String pageName) {
        if (!pages.containsKey(pageName)) {
            System.err.println("Unknown page: " + pageName);
            return;
        }

        // trim forward history if needed
        if (historyIndex < history.size() - 1) {
            while (history.size() - 1 > historyIndex) history.remove(history.size() - 1);
        }
        history.add(pageName);
        historyIndex = history.size() - 1;
        displayPage(pageName);
        updateNavButtons();
    }

    /**
     * Display page without modifying history (used by back/forward).
     */
    private void displayPage(String pageName) {
        if (!pages.containsKey(pageName)) return;
        cardLayout.show(cardPanel, pageName);
        setPageTitle(pageName);
        updateNavButtons();
    }

    private void setPageTitle(String pageName) {
        switch (pageName) {
            case "CART": pageLabel.setText("Cart"); break;
            case "ORDERS": pageLabel.setText("Orders"); break;
            case "PROFILE": pageLabel.setText("Profile"); break;
            case "VENDOR": pageLabel.setText("Vendor Dashboard"); break;
            case "BILLING": pageLabel.setText("Billing"); break;
            case "HOME":
            default: pageLabel.setText("Home"); break;
        }
    }

    private void updateNavButtons() {
        backBtn.setEnabled(historyIndex > 0);
        forwardBtn.setEnabled(historyIndex < history.size() - 1);
    }

    public void goBack() {
        if (historyIndex > 0) {
            historyIndex--;
            displayPage(history.get(historyIndex));
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void goForward() {
        if (historyIndex < history.size() - 1) {
            historyIndex++;
            displayPage(history.get(historyIndex));
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void setupKeyBindings() {
        JRootPane root = this.getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke("alt LEFT"), "goBack");
        am.put("goBack", new AbstractAction() { public void actionPerformed(ActionEvent e) { goBack(); }});
        im.put(KeyStroke.getKeyStroke("alt RIGHT"), "goForward");
        am.put("goForward", new AbstractAction() { public void actionPerformed(ActionEvent e) { goForward(); }});

        im.put(KeyStroke.getKeyStroke("BACK_SPACE"), "goBack");

        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, menuMask), "goBack");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, menuMask), "goForward");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, menuMask), "goBack");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, menuMask), "goForward");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK), "goBack");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK), "goForward");
    }

    /**
     * If Home page exists, call its refreshProducts() method to update quietly.
     * HomePage must implement a public refreshProducts() method (see HomePage below).
     */
    public void refreshHomeIfPresent() {
        JPanel home = pages.get("HOME");
        if (home instanceof HomePage) {
            ((HomePage) home).refreshProducts();
        }
    }

    /**
     * Refresh an arbitrary page by name if it exposes a refresh method.
     * For now only "HOME" is supported but you can extend this.
     */
    public void refreshPage(String pageName) {
        JPanel p = pages.get(pageName);
        if (p == null) return;
        if (p instanceof HomePage) ((HomePage) p).refreshProducts();
        // add other cases if necessary (e.g., ordersPanel.refreshOrders())
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // if you have Theme.applyDarkTheme(); keep it
            try { Theme.applyDarkTheme(); } catch (Throwable ignored) {}
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
