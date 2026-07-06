package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.common.component.AppToolWindowChrome;
import com.laker.postman.common.component.ToolWindowStripeMetrics;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.collections.CollectionRequestExecutionScopeResolver;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

public final class RequestSideAssistantPanel extends JPanel {
    private static final int TOOLBAR_WIDTH = ToolWindowStripeMetrics.STRIPE_THICKNESS;
    private static final int DEFAULT_DRAWER_WIDTH = AppToolWindowChrome.DEFAULT_SIDE_WIDTH;
    private static final int MIN_DRAWER_WIDTH = 260;
    private static final int MAX_DRAWER_WIDTH = 640;
    private static final int RESIZE_HANDLE_WIDTH = AppToolWindowChrome.DIVIDER_SIZE;
    private static final int TOOL_ICON_SIZE = 19;
    private static final int TOOL_BUTTON_ARC = 7;
    private static final int TOOLBAR_TOP_PADDING = 4;
    private static final int TOOLBAR_BOTTOM_PADDING = 4;
    private static final int TOOL_BUTTON_GAP = 4;
    private static final int TOOL_BUTTON_VERTICAL_PADDING = 2;
    private static final int TOOL_BUTTON_CONTENT_SHIFT_LEFT = 4;
    private static final int TOOL_BUTTON_SELECTED_BACKGROUND_PADDING = 1;

    private final Supplier<HttpRequestItem> requestSupplier;
    private final CardLayout drawerLayout = new CardLayout();
    private final JPanel drawerPanel = new JPanel(drawerLayout);
    private final JComponent drawerChrome;
    private final JLayeredPane drawerStack;
    private final RequestVariablesInRequestPanel variablesPanel;
    private final RequestCodeSnippetPanel codeSnippetPanel;
    private final AssistantToolLabel variablesToolLabel;
    private final AssistantToolLabel codeToolLabel;
    private final JComponent resizeHandle;
    private Tool activeTool;
    private int drawerWidth = DEFAULT_DRAWER_WIDTH;

    public RequestSideAssistantPanel(Supplier<HttpRequestItem> requestSupplier) {
        this.requestSupplier = requestSupplier;
        variablesPanel = new RequestVariablesInRequestPanel(this::hideDrawer, this::refreshNow);
        codeSnippetPanel = new RequestCodeSnippetPanel(this::hideDrawer, this::refreshNow);
        setLayout(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyBackground(this);

        variablesToolLabel = createToolLabel(
                "icons/request-variables.svg",
                I18nUtil.getMessage(MessageKeys.REQUEST_ASSISTANT_VARIABLES_TITLE),
                Tool.VARIABLES
        );
        codeToolLabel = createToolLabel(
                "icons/code.svg",
                I18nUtil.getMessage(MessageKeys.REQUEST_ASSISTANT_CODE_SNIPPET_TITLE),
                Tool.CODE
        );
        resizeHandle = createResizeHandle();

        JPanel toolbar = createToolbar();
        drawerPanel.add(variablesPanel, Tool.VARIABLES.name());
        drawerPanel.add(codeSnippetPanel, Tool.CODE.name());
        ToolWindowSurfaceStyle.applyCard(drawerPanel);

        drawerChrome = AppToolWindowChrome.wrapToolWindow(drawerPanel, new Insets(4, 0, 4, 6));
        drawerChrome.setPreferredSize(new Dimension(drawerWidth, 1));
        drawerChrome.setMinimumSize(new Dimension(MIN_DRAWER_WIDTH, 0));
        drawerStack = createDrawerStack();
        drawerStack.setVisible(false);

        JPanel toolWindowArea = new JPanel(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyBackground(toolWindowArea);
        toolWindowArea.add(drawerStack, BorderLayout.CENTER);
        toolWindowArea.add(toolbar, BorderLayout.EAST);

        add(toolWindowArea, BorderLayout.CENTER);
        setPreferredSize(new Dimension(TOOLBAR_WIDTH, 1));
        setMinimumSize(new Dimension(TOOLBAR_WIDTH, 0));
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        ToolWindowSurfaceStyle.applyBackground(toolbar);
        toolbar.setPreferredSize(new Dimension(TOOLBAR_WIDTH, 1));
        toolbar.setMinimumSize(new Dimension(TOOLBAR_WIDTH, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(TOOLBAR_TOP_PADDING, 0, TOOLBAR_BOTTOM_PADDING, 0));
        toolbar.add(variablesToolLabel);
        toolbar.add(Box.createVerticalStrut(TOOL_BUTTON_GAP));
        toolbar.add(codeToolLabel);
        toolbar.add(Box.createVerticalGlue());
        return toolbar;
    }

    private JComponent createResizeHandle() {
        JPanel handle = new JPanel(new BorderLayout());
        handle.setOpaque(false);
        handle.setPreferredSize(new Dimension(RESIZE_HANDLE_WIDTH, 1));
        handle.setMinimumSize(new Dimension(RESIZE_HANDLE_WIDTH, 0));
        handle.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));

        MouseAdapter adapter = new MouseAdapter() {
            private int startX;
            private int startWidth;

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                startX = e.getXOnScreen();
                startWidth = drawerWidth;
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                int delta = e.getXOnScreen() - startX;
                setDrawerWidth(startWidth - delta);
            }
        };
        handle.addMouseListener(adapter);
        handle.addMouseMotionListener(adapter);
        return handle;
    }

    private JLayeredPane createDrawerStack() {
        return new JLayeredPane() {
            {
                add(drawerChrome, JLayeredPane.DEFAULT_LAYER);
                add(resizeHandle, JLayeredPane.PALETTE_LAYER);
            }

            @Override
            public void doLayout() {
                int width = getWidth();
                int height = getHeight();
                drawerChrome.setBounds(0, 0, width, height);
                resizeHandle.setBounds(0, 0, RESIZE_HANDLE_WIDTH, height);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(drawerWidth, 1);
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(MIN_DRAWER_WIDTH, 0);
            }
        };
    }

    private AssistantToolLabel createToolLabel(String iconPath, String tooltip, Tool tool) {
        AssistantToolLabel label = new AssistantToolLabel(iconPath);
        label.setToolTipText(tooltip);
        label.getAccessibleContext().setAccessibleName(tooltip);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    toggleTool(tool);
                }
            }
        });
        return label;
    }

    private void toggleTool(Tool tool) {
        if (activeTool == tool && drawerStack.isVisible()) {
            hideDrawer();
            return;
        }
        showTool(tool);
    }

    private void showTool(Tool tool) {
        activeTool = tool;
        drawerLayout.show(drawerPanel, tool.name());
        drawerStack.setVisible(true);
        applyExpandedSize();
        variablesToolLabel.setToolSelected(tool == Tool.VARIABLES);
        codeToolLabel.setToolSelected(tool == Tool.CODE);
        refreshNow();
        revalidate();
        repaint();
    }

    private void hideDrawer() {
        drawerStack.setVisible(false);
        activeTool = null;
        variablesToolLabel.setToolSelected(false);
        codeToolLabel.setToolSelected(false);
        setPreferredSize(new Dimension(TOOLBAR_WIDTH, 1));
        revalidate();
        repaint();
    }

    private void setDrawerWidth(int width) {
        drawerWidth = Math.max(MIN_DRAWER_WIDTH, Math.min(MAX_DRAWER_WIDTH, width));
        if (drawerStack.isVisible()) {
            applyExpandedSize();
            revalidate();
            repaint();
        }
    }

    private void applyExpandedSize() {
        drawerChrome.setPreferredSize(new Dimension(drawerWidth, 1));
        drawerStack.setPreferredSize(new Dimension(drawerWidth, 1));
        setPreferredSize(new Dimension(drawerWidth + TOOLBAR_WIDTH, 1));
    }

    private void refreshNow() {
        HttpRequestItem item = requestSupplier.get();
        CollectionRequestExecutionScopeResolver.syncCurrentScopeOrEmpty(item == null ? null : item.getId());
        variablesPanel.updateRequest(item);
        codeSnippetPanel.updateRequest(item);
    }

    private enum Tool {
        VARIABLES,
        CODE
    }

    private static final class AssistantToolLabel extends JLabel {
        private final Icon defaultIcon;
        private final Icon selectedIcon;
        private boolean selected;

        private AssistantToolLabel(String iconPath) {
            defaultIcon = IconUtil.createThemed(iconPath, TOOL_ICON_SIZE, TOOL_ICON_SIZE);
            selectedIcon = IconUtil.createOnPrimary(iconPath, TOOL_ICON_SIZE, TOOL_ICON_SIZE);
            setIcon(defaultIcon);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setFocusable(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(
                    TOOL_BUTTON_VERTICAL_PADDING,
                    0,
                    TOOL_BUTTON_VERTICAL_PADDING,
                    TOOL_BUTTON_CONTENT_SHIFT_LEFT * 2
            ));
            Dimension size = new Dimension(TOOLBAR_WIDTH, TOOLBAR_WIDTH);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        private void setToolSelected(boolean selected) {
            if (this.selected == selected) {
                return;
            }
            this.selected = selected;
            setIcon(selected ? selectedIcon : defaultIcon);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (selected) {
                Graphics2D copy = (Graphics2D) g.create();
                copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                copy.setColor(ModernColors.getPrimary());
                Rectangle bounds = selectedBackgroundBounds();
                copy.fillRoundRect(
                        bounds.x,
                        bounds.y,
                        bounds.width,
                        bounds.height,
                        TOOL_BUTTON_ARC,
                        TOOL_BUTTON_ARC
                );
                copy.dispose();
            }
            super.paintComponent(g);
        }

        private Rectangle selectedBackgroundBounds() {
            Insets insets = getInsets();
            int x = Math.max(0, insets.left - TOOL_BUTTON_SELECTED_BACKGROUND_PADDING);
            int y = insets.top;
            int width = Math.max(0, getWidth()
                    - insets.left
                    - insets.right
                    + TOOL_BUTTON_SELECTED_BACKGROUND_PADDING * 2);
            int height = Math.max(0, getHeight() - insets.top - insets.bottom);
            return new Rectangle(x, y, Math.min(width, getWidth() - x), height);
        }
    }
}
