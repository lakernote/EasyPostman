package com.laker.postman.panel.collections.tree.dialog;

import com.laker.postman.common.constants.ModernColors;
import org.testng.annotations.Test;

import java.awt.FlowLayout;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;

public class AddRequestDialogLayoutTest {

    @Test
    public void addRequestDialogShouldReserveHeightForProtocolCardsAndFooter() {
        assertEquals(AddRequestDialog.DIALOG_HEIGHT, 280,
                "Add request dialog should stay compact while leaving room for protocol cards above the fixed footer");
    }

    @Test
    public void protocolCardsShouldUseCompactCustomSelectionChrome() {
        AddRequestDialog.ProtocolOptionButton button = new AddRequestDialog.ProtocolOptionButton("HTTP", null);

        assertEquals(button.getPreferredSize(), new Dimension(96, 54));
        assertFalse(button.isContentAreaFilled(),
                "Protocol cards should not use FlatLaf's solid toggle-button selected fill");
        assertFalse(button.isBorderPainted(),
                "Protocol cards paint their own light border and selected indicator");
        assertEquals(button.getVerticalTextPosition(), SwingConstants.BOTTOM);
        assertEquals(button.getHorizontalTextPosition(), SwingConstants.CENTER);
    }

    @Test
    public void protocolCardsShouldBeCenteredWithinDialogContent() {
        FlowLayout layout = AddRequestDialog.createProtocolButtonsLayout();

        assertEquals(layout.getAlignment(), FlowLayout.CENTER,
                "Protocol cards should be centered under the full-width request name field");
        assertEquals(layout.getHgap(), 10);
        assertEquals(layout.getVgap(), 0);
    }

    @Test
    public void selectedProtocolCardShouldNotPaintBottomPrimaryBar() {
        AddRequestDialog.ProtocolOptionButton button = new AddRequestDialog.ProtocolOptionButton("HTTP", null);
        button.setSelected(true);
        button.setSize(button.getPreferredSize());

        BufferedImage image = new BufferedImage(button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_ARGB);
        button.paint(image.getGraphics());

        Color bottomCenter = new Color(image.getRGB(button.getWidth() / 2, button.getHeight() - 2), true);
        assertNotEquals(bottomCenter.getRGB(), ModernColors.getPrimary().getRGB(),
                "Selected protocol cards should rely on fill and border, not a thick bottom primary bar");
    }
}
