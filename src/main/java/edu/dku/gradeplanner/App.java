package edu.dku.gradeplanner;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class App {
    private App() { }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { /* The default Swing look and feel is also supported. */ }
            new MainFrame().setVisible(true);
        });
    }
}
