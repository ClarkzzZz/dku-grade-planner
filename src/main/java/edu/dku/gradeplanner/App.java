package edu.dku.gradeplanner;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Locale;

public final class App {
    private App() { }
    public static void main(String[] args) {
        configureEnglishLocale();
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { /* The default Swing look and feel is also supported. */ }
            new MainFrame().setVisible(true);
        });
    }

    /** Includes Swing's built-in confirmation buttons and file chooser labels. */
    static void configureEnglishLocale() {
        Locale.setDefault(Locale.ENGLISH);
        javax.swing.JComponent.setDefaultLocale(Locale.ENGLISH);
    }
}
