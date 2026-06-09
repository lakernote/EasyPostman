package com.laker.postman.panel.topmenu;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;
import lombok.experimental.UtilityClass;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;

/**
 * EasyPostman 关于对话框。
 */
@UtilityClass
class TopMenuAboutDialog {

    void show(Component parent) {
        JEditorPane editorPane = createAboutEditorPane(createAboutHtml(parent), parent);
        JOptionPane.showMessageDialog(
                parent,
                editorPane,
                I18nUtil.getMessage(MessageKeys.MENU_ABOUT_EASYPOSTMAN),
                JOptionPane.PLAIN_MESSAGE
        );
    }

    private String createAboutHtml(Component parent) {
        String iconUrl = parent.getClass().getResource("/icons/icon.png") + "";
        return "<html>"
                + "<head>"
                + "<div style='border-radius:16px; border:1px solid " + getThemeBorderColor() + "; padding:20px 28px; min-width:340px; max-width:420px;'>"
                + "<div style='text-align:center;'>"
                + "<img src='" + iconUrl + "' width='56' height='56' style='margin-bottom:10px;'/>"
                + "</div>"
                + "<div style='font-size:16px; font-weight:bold; color:" + getThemeTextColor() + "; text-align:center; margin-bottom:6px;'>EasyPostman</div>"
                + "<div style='font-size:12px; color:" + getThemeSecondaryTextColor() + "; text-align:center; margin-bottom:12px;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_VERSION, SystemUtil.getCurrentVersion()) + "</div>"
                + "<div style='font-size:10px; color:" + getThemeHintTextColor() + "; margin-bottom:2px;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_AUTHOR) + "</div>"
                + "<div style='font-size:10px; color:" + getThemeHintTextColor() + "; margin-bottom:2px;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_LICENSE) + "</div>"
                + "<div style='font-size:10px; color:" + getThemeHintTextColor() + "; margin-bottom:8px;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_WECHAT) + "</div>"
                + "<hr style='border:none; border-top:1px solid " + getThemeBorderColor() + "; margin:10px 0;'>"
                + "<div style='font-size:9px; margin-bottom:2px;'>"
                + "<a href='https://laker.blog.csdn.net' style='color:" + getThemeLinkColor() + "; text-decoration:none;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_BLOG) + "</a>"
                + "</div>"
                + "<div style='font-size:9px; margin-bottom:2px;'>"
                + "<a href='https://github.com/lakernote' style='color:" + getThemeLinkColor() + "; text-decoration:none;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_GITHUB) + "</a>"
                + "</div>"
                + "<div style='font-size:9px;'>"
                + "<a href='https://gitee.com/lakernote' style='color:" + getThemeLinkColor() + "; text-decoration:none;'>"
                + I18nUtil.getMessage(MessageKeys.ABOUT_GITEE) + "</a>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    private JEditorPane createAboutEditorPane(String html, Component parent) {
        JEditorPane editorPane = new JEditorPane("text/html", html);
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(parent,
                            I18nUtil.getMessage(MessageKeys.ERROR_OPEN_LINK_FAILED, e.getURL()),
                            I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        editorPane.setPreferredSize(new Dimension(310, 350));
        return editorPane;
    }

    private String getThemeBorderColor() {
        return toHex(ModernColors.getDividerBorderColor());
    }

    private String getThemeTextColor() {
        return toHex(ModernColors.getTextPrimary());
    }

    private String getThemeSecondaryTextColor() {
        return toHex(ModernColors.getTextSecondary());
    }

    private String getThemeHintTextColor() {
        return toHex(ModernColors.getTextHint());
    }

    private String getThemeLinkColor() {
        return toHex(ModernColors.getPrimary());
    }

    private String toHex(Color color) {
        return ModernColors.toHtmlColor(color);
    }
}
