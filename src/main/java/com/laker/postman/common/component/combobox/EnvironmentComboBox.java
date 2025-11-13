package com.laker.postman.common.component.combobox;

import com.laker.postman.model.Environment;
import com.laker.postman.model.EnvironmentItem;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.util.FontsUtil;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class EnvironmentComboBox extends JComboBox<EnvironmentItem> {
    private boolean isUpdating = false;
    @Setter
    private Consumer<Environment> onEnvironmentChange;

    public EnvironmentComboBox() {
        setRenderer(new EnvironmentItemRenderer());
        setPreferredSize(new Dimension(150, 28));
        setMaximumSize(new Dimension(150, 28));
        setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        setFocusable(false);
        addActionListener(e -> {
            if (isUpdating) return;
            EnvironmentItem item = (EnvironmentItem) getSelectedItem();
            if (item != null && item.getEnvironment() != null) {
                EnvironmentService.setActiveEnvironment(item.getEnvironment().getId());
                if (onEnvironmentChange != null) {
                    onEnvironmentChange.accept(item.getEnvironment());
                }
            }
        });
        reload();
    }

    public void reload() {
        isUpdating = true;
        removeAllItems();
        List<Environment> envs = EnvironmentService.getAllEnvironments();
        Environment activeEnv = null;
        for (Environment env : envs) {
            addItem(new EnvironmentItem(env));
            if (env.isActive()) {
                activeEnv = env;
            }
        }
        if (activeEnv != null) {
            for (int i = 0; i < getItemCount(); i++) {
                EnvironmentItem item = getItemAt(i);
                if (item.getEnvironment().getId().equals(activeEnv.getId())) {
                    setSelectedIndex(i);
                    break;
                }
            }
        } else if (getItemCount() > 0) {
            setSelectedIndex(0);
        }
        isUpdating = false;
    }

    /**
     * 选中指定环境
     */
    public void setSelectedEnvironment(Environment env) {
        if (env == null) return;
        isUpdating = true;
        for (int i = 0; i < getItemCount(); i++) {
            EnvironmentItem item = getItemAt(i);
            if (item.getEnvironment().getId().equals(env.getId())) {
                setSelectedIndex(i);
                break;
            }
        }
        isUpdating = false;
    }
}