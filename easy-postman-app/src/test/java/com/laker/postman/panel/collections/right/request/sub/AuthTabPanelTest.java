package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.model.AuthType;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;

import static org.testng.Assert.assertEquals;

public class AuthTabPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldUseCurrentVisibleCredentialsWhenSwitchingFromBasicToDigest() throws Exception {
        String[] username = new String[1];
        String[] password = new String[1];

        SwingUtilities.invokeAndWait(() -> {
            AuthTabPanel panel = new AuthTabPanel();
            panel.setAuthType(AuthType.BASIC.getConstant());
            panel.setUsername("loaded-user");
            panel.setPassword("loaded-pass");

            textField(panel, "usernameField").setText("edited-user");
            textField(panel, "passwordField").setText("edited-pass");

            panel.setAuthType(AuthType.DIGEST.getConstant());

            username[0] = panel.getUsername();
            password[0] = panel.getPassword();
        });

        assertEquals(username[0], "edited-user");
        assertEquals(password[0], "edited-pass");
    }

    @Test
    public void shouldUseCurrentVisibleCredentialsWhenSwitchingFromDigestToBasic() throws Exception {
        String[] username = new String[1];
        String[] password = new String[1];

        SwingUtilities.invokeAndWait(() -> {
            AuthTabPanel panel = new AuthTabPanel();
            panel.setAuthType(AuthType.DIGEST.getConstant());
            panel.setUsername("loaded-user");
            panel.setPassword("loaded-pass");

            textField(panel, "digestUsernameField").setText("edited-user");
            textField(panel, "digestPasswordField").setText("edited-pass");

            panel.setAuthType(AuthType.BASIC.getConstant());

            username[0] = panel.getUsername();
            password[0] = panel.getPassword();
        });

        assertEquals(username[0], "edited-user");
        assertEquals(password[0], "edited-pass");
    }

    private static JTextField textField(AuthTabPanel panel, String fieldName) {
        try {
            Field field = AuthTabPanel.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (JTextField) field.get(panel);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
