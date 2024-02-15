package bguspl.set;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * Handles windows events (e.g. closing the game window with the X button).
 */
public class WindowManager implements WindowListener {

    @Override
    public void windowOpened(WindowEvent e) {
        // Auto-generated method stub
    }

    @Override
    public void windowClosing(WindowEvent e) {
        try {
            Main.xButtonPressed();
        } catch (InterruptedException ignored) {}
    }

    @Override
    public void windowClosed(WindowEvent e) {
        // Auto-generated method stub
    }

    @Override
    public void windowIconified(WindowEvent e) {
        // Auto-generated method stub
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        // Auto-generated method stub
    }

    @Override
    public void windowActivated(WindowEvent e) {
        // Auto-generated method stub
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        // Auto-generated method stub
    }
}
