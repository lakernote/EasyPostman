package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.component.EasyTextField;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.util.HttpUrlUtil;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.Supplier;

/**
 * URL input with Postman-style path variable hints.
 * <p>
 * EasyTextField already handles {{variables}}. This class only adds visual
 * treatment and tooltip content for path tokens such as :id in the URL path.
 */
@Slf4j
class RequestUrlField extends EasyTextField {
    private Supplier<List<HttpParam>> pathVariablesSupplier = List::of;

    RequestUrlField(String text, int columns, String placeholderText) {
        super(text, columns, placeholderText);
    }

    void setPathVariablesSupplier(Supplier<List<HttpParam>> pathVariablesSupplier) {
        this.pathVariablesSupplier = pathVariablesSupplier == null ? List::of : pathVariablesSupplier;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintPathVariableBadges(g);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        HttpUrlUtil.PathVariableSegment segment = pathVariableAt(event);
        if (segment != null) {
            return buildPathVariableTooltip(segment.name());
        }
        return super.getToolTipText(event);
    }

    private void paintPathVariableBadges(Graphics g) {
        String text = getText();
        List<HttpUrlUtil.PathVariableSegment> segments = HttpUrlUtil.extractPathVariableSegments(text);
        if (segments.isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics metrics = getFontMetrics(getFont());
            int height = metrics.getHeight();
            for (HttpUrlUtil.PathVariableSegment segment : segments) {
                Rectangle2D startRect = modelToView2D(segment.startIndex());
                if (startRect == null) {
                    continue;
                }
                String tokenText = text.substring(segment.startIndex(), segment.endIndex());
                int x = (int) Math.floor(startRect.getX());
                int y = (int) Math.floor(startRect.getY());
                int width = metrics.stringWidth(tokenText);
                if (x + width < 0 || x > getWidth()) {
                    continue;
                }

                g2.setColor(pathVariableBackground());
                g2.fillRoundRect(x, y, width, height, 8, 8);
                g2.setColor(ModernColors.getDefinedVariableBadgeBorder());
                g2.drawRoundRect(x, y, width, height, 8, 8);
            }
        } catch (Exception e) {
            log.debug("Failed to paint URL path variable badges", e);
        } finally {
            g2.dispose();
        }
    }

    private HttpUrlUtil.PathVariableSegment pathVariableAt(MouseEvent event) {
        if (event == null) {
            return null;
        }
        List<HttpUrlUtil.PathVariableSegment> segments = HttpUrlUtil.extractPathVariableSegments(getText());
        if (segments.isEmpty()) {
            return null;
        }

        try {
            int offset = viewToModel2D(event.getPoint());
            for (HttpUrlUtil.PathVariableSegment segment : segments) {
                if (offset >= segment.startIndex() && offset < segment.endIndex()
                        && isMouseWithinSegment(event, segment)) {
                    return segment;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to resolve URL path variable tooltip target", e);
        }
        return null;
    }

    private boolean isMouseWithinSegment(MouseEvent event, HttpUrlUtil.PathVariableSegment segment) throws Exception {
        Rectangle2D startRect = modelToView2D(segment.startIndex());
        if (startRect == null) {
            return false;
        }
        FontMetrics metrics = getFontMetrics(getFont());
        String tokenText = getText().substring(segment.startIndex(), segment.endIndex());
        int x = (int) Math.floor(startRect.getX());
        int width = metrics.stringWidth(tokenText);
        return event.getX() >= x && event.getX() <= x + width;
    }

    private String buildPathVariableTooltip(String name) {
        return RequestBodyVariableTooltipBuilder.pathVariableTooltip(name, findPathVariableValue(name));
    }

    private String findPathVariableValue(String name) {
        for (HttpParam param : pathVariables()) {
            if (param != null && name.equals(param.getKey())) {
                return param.getValue();
            }
        }
        return "";
    }

    private List<HttpParam> pathVariables() {
        try {
            List<HttpParam> pathVariables = pathVariablesSupplier.get();
            return pathVariables == null ? List.of() : pathVariables;
        } catch (Exception e) {
            log.debug("Failed to read URL path variables", e);
            return List.of();
        }
    }

    private static Color pathVariableBackground() {
        int alpha = ModernColors.isDarkTheme() ? 70 : 45;
        return ModernColors.withAlpha(ModernColors.getPrimary(), alpha);
    }
}
