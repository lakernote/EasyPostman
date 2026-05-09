package com.laker.postman.common.component;

import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.lang.reflect.Field;
import java.util.List;

import static org.testng.Assert.assertTrue;

public class AutoCompleteEasyTextFieldTest extends AbstractSwingUiTest {

    @Test
    public void shouldKeepAutocompletePopupOnFieldScreen() throws Exception {
        Dimension primarySize = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle secondaryLikeBounds = new Rectangle(primarySize.width, 0, 1440, 900);
        Point fieldLocation = new Point(secondaryLikeBounds.x + 60, 80);
        TestAutoCompleteField field = new TestAutoCompleteField(fieldLocation, secondaryLikeBounds);
        field.setSize(360, 28);
        field.setSuggestions(List.of("Accept", "Accept-Charset", "Accept-Encoding"));
        field.setAutoCompleteEnabled(true);

        SwingUtilities.invokeAndWait(field::showAllSuggestions);

        JWindow popup = popupOf(field);
        try {
            assertTrue(popup.getLocationOnScreen().x >= secondaryLikeBounds.x,
                    "Autocomplete popup must stay on the same screen as the edited field");
        } finally {
            SwingUtilities.invokeAndWait(() -> field.setAutoCompleteEnabled(false));
            popup.dispose();
        }
    }

    private JWindow popupOf(AutoCompleteEasyTextField field) throws Exception {
        Field popupField = AutoCompleteEasyTextField.class.getDeclaredField("popup");
        popupField.setAccessible(true);
        return (JWindow) popupField.get(field);
    }

    private static class TestAutoCompleteField extends AutoCompleteEasyTextField {
        private final Point screenLocation;
        private final GraphicsConfiguration graphicsConfiguration;

        private TestAutoCompleteField(Point screenLocation, Rectangle screenBounds) {
            super(1);
            this.screenLocation = screenLocation;
            this.graphicsConfiguration = new BoundsOnlyGraphicsConfiguration(getGraphicsConfiguration(), screenBounds);
        }

        @Override
        public boolean isShowing() {
            return true;
        }

        @Override
        public Point getLocationOnScreen() {
            return new Point(screenLocation);
        }

        @Override
        public GraphicsConfiguration getGraphicsConfiguration() {
            return graphicsConfiguration != null ? graphicsConfiguration : super.getGraphicsConfiguration();
        }
    }

    private static class BoundsOnlyGraphicsConfiguration extends GraphicsConfiguration {
        private final GraphicsConfiguration delegate;
        private final Rectangle bounds;

        private BoundsOnlyGraphicsConfiguration(GraphicsConfiguration delegate, Rectangle bounds) {
            this.delegate = delegate;
            this.bounds = bounds;
        }

        @Override
        public GraphicsDevice getDevice() {
            return delegate.getDevice();
        }

        @Override
        public ColorModel getColorModel() {
            return delegate.getColorModel();
        }

        @Override
        public ColorModel getColorModel(int transparency) {
            return delegate.getColorModel(transparency);
        }

        @Override
        public AffineTransform getDefaultTransform() {
            return delegate.getDefaultTransform();
        }

        @Override
        public AffineTransform getNormalizingTransform() {
            return delegate.getNormalizingTransform();
        }

        @Override
        public Rectangle getBounds() {
            return new Rectangle(bounds);
        }
    }
}
