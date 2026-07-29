package com.laker.postman.startup;

import com.laker.postman.frame.MainFrame;
import lombok.experimental.UtilityClass;

import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Adapts platform-level activation requests to the concrete Swing main window.
 */
@UtilityClass
class AppSingleInstanceController {
    private static final AtomicReference<MainFrame> READY_MAIN_FRAME = new AtomicReference<>();
    private static final AtomicBoolean ACTIVATION_PENDING = new AtomicBoolean();

    void requestActivation() {
        MainFrame mainFrame = READY_MAIN_FRAME.get();
        if (mainFrame != null) {
            activate(mainFrame);
            return;
        }

        ACTIVATION_PENDING.set(true);
        mainFrame = READY_MAIN_FRAME.get();
        if (mainFrame != null && ACTIVATION_PENDING.compareAndSet(true, false)) {
            activate(mainFrame);
        }
    }

    void registerReadyMainFrame(MainFrame mainFrame) {
        if (mainFrame == null) {
            return;
        }
        READY_MAIN_FRAME.set(mainFrame);
        if (ACTIVATION_PENDING.compareAndSet(true, false)) {
            activate(mainFrame);
        }
    }

    private void activate(MainFrame mainFrame) {
        Runnable activation = () -> {
            mainFrame.setVisible(true);
            int state = mainFrame.getExtendedState();
            if ((state & Frame.ICONIFIED) != 0) {
                mainFrame.setExtendedState(state & ~Frame.ICONIFIED);
            }
            mainFrame.toFront();
            mainFrame.requestFocus();
            mainFrame.requestFocusInWindow();
        };

        if (SwingUtilities.isEventDispatchThread()) {
            activation.run();
        } else {
            SwingUtilities.invokeLater(activation);
        }
    }
}
