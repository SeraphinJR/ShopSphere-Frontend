package pages;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.UIManager;
import java.awt.Color;

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
}
