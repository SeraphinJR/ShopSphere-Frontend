package pages;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Theme helper: applies a consistent dark look-and-feel to Swing UIs.
 */
public class Theme {
    public static void applyDarkTheme() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            // Make it extra dark: force near-black backgrounds and high-contrast white text
            UIManager.put("control", Color.decode("#000000"));
            UIManager.put("info", Color.decode("#000000"));
            UIManager.put("nimbusBase", Color.decode("#000000"));
            UIManager.put("nimbusAlertYellow", Color.decode("#000000"));
            UIManager.put("nimbusDisabledText", Color.decode("#7f7f7f"));
            UIManager.put("text", Color.decode("#FFFFFF"));
            UIManager.put("Label.foreground", Color.decode("#FFFFFF"));
            UIManager.put("Panel.background", Color.decode("#000000"));
            UIManager.put("Button.background", Color.decode("#000000"));
            UIManager.put("Button.foreground", Color.decode("#FFFFFF"));
            UIManager.put("ToolTip.background", Color.decode("#111111"));
            UIManager.put("TextField.background", Color.decode("#0A0A0A"));
            UIManager.put("TextField.foreground", Color.decode("#FFFFFF"));
            UIManager.put("PasswordField.background", Color.decode("#0A0A0A"));
            UIManager.put("PasswordField.foreground", Color.decode("#FFFFFF"));
            UIManager.put("Table.background", Color.decode("#000000"));
            UIManager.put("Table.foreground", Color.decode("#FFFFFF"));
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatDarkLaf: " + ex.getMessage());
        }
    }

    /**
     * Recursively style a component tree to use black background, white text
     * and a clear outlined box look for panels.
     * Note: we choose white borders for visibility on black background.
     */
    public static void styleComponentTree(Component root) {
        if (root == null)
            return;
        if (root instanceof JComponent) {
            JComponent jc = (JComponent) root;
            try {
                jc.setBackground(Color.BLACK);
            } catch (Exception ignored) {
            }
            try {
                jc.setForeground(Color.WHITE);
            } catch (Exception ignored) {
            }
            // For panels and other containers use a white line border for card-like
            // appearance
            if (jc instanceof JPanel) {
                Border existing = jc.getBorder();
                // avoid stomping required layout borders (like empty borders used for spacing)
                if (existing == null || existing instanceof javax.swing.border.LineBorder
                        || existing instanceof javax.swing.border.TitledBorder) {
                    jc.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
                } else {
                    // wrap existing border with compound to preserve spacing
                    jc.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 1),
                            existing));
                }
            }
        }

        if (root instanceof Container) {
            for (Component c : ((Container) root).getComponents()) {
                styleComponentTree(c);
            }
        }
    }
}
