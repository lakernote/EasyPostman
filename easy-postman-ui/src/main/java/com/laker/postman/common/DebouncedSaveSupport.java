package com.laker.postman.common;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.Objects;

/**
 * Swing 场景下的防抖保存辅助类。
 * <p>
 * 所有 Timer 操作都收敛到 EDT，避免 UI 事件连续触发时频繁落盘。
 */
public class DebouncedSaveSupport {
    private final Timer timer;
    private final Runnable saveAction;

    public DebouncedSaveSupport(int delayMillis, Runnable saveAction) {
        this.saveAction = Objects.requireNonNull(saveAction, "saveAction");
        this.timer = new Timer(delayMillis, e -> this.saveAction.run());
        this.timer.setRepeats(false);
    }

    public void requestSave() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::requestSave);
            return;
        }
        timer.restart();
    }

    public void cancel() {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(this::cancel);
            } catch (Exception e) {
                timer.stop();
            }
            return;
        }
        timer.stop();
    }
}
