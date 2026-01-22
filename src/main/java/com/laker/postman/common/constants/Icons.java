package com.laker.postman.common.constants;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.util.Objects;

@UtilityClass
public class Icons {
    public static final ImageIcon LOGO = new ImageIcon(Objects.requireNonNull(Icons.class.getResource("/icons/icon.png")));
}