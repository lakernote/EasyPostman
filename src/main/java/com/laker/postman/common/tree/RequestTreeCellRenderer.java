package com.laker.postman.common.tree;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.http.HttpUtil;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * 自定义树节点渲染器，用于美化 JTree 的节点显示
 */
public class RequestTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();
        if ("-请求集合-".equals(userObject)) {
            setIcon(IconFontSwing.buildIcon(FontAwesome.FOLDER, 18, new Color(120, 120, 120)));
        } else if (userObject instanceof Object[] obj) {
            if ("group".equals(obj[0])) {
                // 橙色实心文件夹，模拟Postman分组
                setIcon(IconFontSwing.buildIcon(FontAwesome.FOLDER_O, 14, new Color(255, 140, 0)));
                setText((String) obj[1]);
            } else if ("request".equals(obj[0])) {
                // 直接用彩色文本显示 method + name，method 彩色，name 默认色
                HttpRequestItem item = (HttpRequestItem) obj[1];
                String method = item.getMethod();
                String name = item.getName();
                String methodColor = HttpUtil.getMethodColor(method);
                setIcon(null);
                setText("<html><span style='color:" + methodColor + ";font-weight:bold;font-size:8px'>" + (method == null ? "" : method) + "</span> <span style='font-size:9px'>" + name + "</span></html>");
            }
        }
        if (sel) setBackgroundSelectionColor(new Color(255, 230, 180));
        else setBackgroundNonSelectionColor(getBackground());
        return this;
    }

}