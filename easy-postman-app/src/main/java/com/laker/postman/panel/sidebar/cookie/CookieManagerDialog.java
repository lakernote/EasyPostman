package com.laker.postman.panel.sidebar.cookie;

import com.laker.postman.panel.collections.right.request.sub.CookieTablePanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * Cookie 管理器对话框
 * 全局 Cookie 管理界面，显示和管理所有域名的 Cookie
 */
public class CookieManagerDialog extends JDialog {

    public CookieManagerDialog(Window owner) {
        super(owner, I18nUtil.getMessage(MessageKeys.COOKIES_MANAGER_TITLE), ModalityType.MODELESS);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 使用现有的 CookieTablePanel
        CookieTablePanel cookiePanel = new CookieTablePanel();
        add(cookiePanel, BorderLayout.CENTER);

        // 设置对话框属性
        setSize(900, 500);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
}

