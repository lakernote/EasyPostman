package com.laker.postman.common.constants;

import javax.swing.*;
import java.util.Objects;

public class Icons {
    private Icons() {
    }

    public static final ImageIcon LOGO = new ImageIcon(Objects.requireNonNull(Icons.class.getResource("/icons/icon.png")));
}
