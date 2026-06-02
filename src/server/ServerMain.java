package server;

import javax.swing.SwingUtilities;

public class ServerMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerFrame frame = new ServerFrame();
            frame.setVisible(true);
        });
    }
}