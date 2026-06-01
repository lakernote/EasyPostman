package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.common.animation.ComponentSnapshotTransition;
import com.laker.postman.common.component.placeholder.RequestEditorPlaceholderPanel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BooleanSupplier;

/**
 * 请求编辑器延迟初始化控制器。
 * <p>
 * 启动恢复大量 Tab 时先显示轻量占位页，用户激活后再创建真实编辑器，避免主面板承担监听和过渡细节。
 */
@RequiredArgsConstructor
final class RequestEditorDeferredInitializationController {
    private final JPanel owner;
    private final BooleanSupplier placeholderActive;
    private final Runnable activationAction;
    private final ComponentSnapshotTransition transition;

    RequestEditorDeferredInitializationController(JPanel owner,
                                                  BooleanSupplier placeholderActive,
                                                  Runnable activationAction) {
        this(owner, placeholderActive, activationAction, new ComponentSnapshotTransition(owner));
    }

    void installPlaceholder() {
        owner.removeAll();
        JComponent placeholder = new RequestEditorPlaceholderPanel();
        attachActivationListeners(placeholder);
        owner.add(placeholder, BorderLayout.CENTER);
    }

    ComponentSnapshotTransition.CapturedSnapshot capturePlaceholderSnapshot() {
        if (!placeholderActive.getAsBoolean()) {
            return null;
        }
        Component currentContent = owner.getComponentCount() > 0 ? owner.getComponent(0) : null;
        if (!(currentContent instanceof JComponent placeholder) || !placeholder.isShowing()) {
            return null;
        }
        return transition.captureSnapshot(placeholder);
    }

    void startTransition(ComponentSnapshotTransition.CapturedSnapshot placeholderSnapshot) {
        if (placeholderSnapshot != null) {
            transition.start(placeholderSnapshot);
        }
    }

    private void attachActivationListeners(Component component) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                activateIfNeeded();
            }
        };
        FocusAdapter focusAdapter = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                activateIfNeeded();
            }
        };
        attachActivationListeners(component, mouseAdapter, focusAdapter);
    }

    private void attachActivationListeners(Component component, MouseAdapter mouseAdapter, FocusAdapter focusAdapter) {
        component.addMouseListener(mouseAdapter);
        component.addFocusListener(focusAdapter);
        if (component instanceof JComponent jComponent) {
            jComponent.setFocusable(true);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                attachActivationListeners(child, mouseAdapter, focusAdapter);
            }
        }
    }

    private void activateIfNeeded() {
        if (placeholderActive.getAsBoolean()) {
            activationAction.run();
        }
    }
}
