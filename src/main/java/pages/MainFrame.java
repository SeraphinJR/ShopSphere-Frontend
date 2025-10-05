package pages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import model.CartModel;

/**
 * MainFrame - keeps single instances of pages, uses CardLayout.show(...)
 * and provides helpers to refresh home quietly.
 *
 * IMPORTANT:
 * - VendorDashboard is NOT instantiated at startup. It's created lazily when first requested.
 * - HomePage should be responsible for checking vendor permissions (via /auth/current)
 *   and only call parent.showPage("VENDOR") when allowed.
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

    // pages map keeps single instances (lazy create for some pages)
    private final Map<String, JPanel> pages = new HashMap<>();

    // navigation history (use fully-qualified type to avoid java.awt.List ambiguity)
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

        // create pages once and add to cardPanel (but DO NOT instantiate VENDOR yet)
        putPage("HOME", new HomePage(this, cartModel));
        putPage("CART", new CartPage(this, cartModel));
        putPage("BILLING", new BillingPanel(this, cartModel));
        putPage("ORDERS", new OrdersPanel(this));
        putPage("PROFILE", new ProfilePage(this, cartModel));
        // NOTE: do NOT create VendorDashboard here.

        setLayout(new BorderLayout());
        add(navBar, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);

        // show home as initial page and record it in history
        showPage("HOME");

        // keyboard shortcuts
        setupKeyBindings();
    }

    // Helper to add page instance to map + cardPanel
    private void putPage(String name, JPanel panel) {
        pages.put(name, panel);
        cardPanel.add(panel, name);
    }

    /**
     * Shows a page and appends to history.
     * Lazily creates vendor page if requested.
     */
    public void showPage(String pageName) {
        // lazy-create vendor page only when requested
        if ("VENDOR".equals(pageName) && !pages.containsKey("VENDOR")) {
            // create vendor dashboard lazily - don't do any server checks here,
            // HomePage should only call showPage("VENDOR") if the user is allowed.
            try {
                VendorDashboard vendor = new VendorDashboard(this);
                putPage("VENDOR", vendor);
            } catch (Throwable t) {
                // if creation fails, show error and don't add to history
                t.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to open Vendor Dashboard:\n" + t.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if (!pages.containsKey(pageName)) {
            System.err.println("Unknown page requested: " + pageName);
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
            case "BILLING":{
                pageLabel.setText("Billing");
                if ("BILLING".equals(pageName)) {
                    JPanel panel = pages.get("BILLING");
                    if (panel instanceof BillingPanel) {
                        ((BillingPanel) panel).reset();
                    }
                }
                break;}
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
     * HomePage must implement a public refreshProducts() method (see HomePage).
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
        // add other cases if necessary
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { Theme.applyDarkTheme(); } catch (Throwable ignored) {}
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
