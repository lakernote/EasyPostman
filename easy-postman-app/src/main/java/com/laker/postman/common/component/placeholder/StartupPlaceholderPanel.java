package com.laker.postman.common.component.placeholder;

import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import java.awt.*;

public class StartupPlaceholderPanel extends JPanel {

    public StartupPlaceholderPanel() {
        super(new GridBagLayout());
        setOpaque(true);
        setBackground(ModernColors.getBackgroundColor());
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        add(new GlassStartupHintCard());
    }
}
