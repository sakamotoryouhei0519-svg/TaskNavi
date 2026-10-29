package org.tasknavi.ui;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemedComboBoxTest {

    @Test
    void listRowStaysOpaqueAfterTheCurrentValueIsPainted() throws Exception {
        AtomicBoolean opaque = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            JComboBox<String> combo = new JComboBox<>(new String[]{"全員", "佐藤花子"});
            AppTheme.styleComboBox(combo);
            combo.setRenderer(new DefaultListCellRenderer());
            combo.setSize(200, 30);
            BufferedImage image = new BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            combo.paint(g);
            g.dispose();

            Component row = combo.getRenderer().getListCellRendererComponent(
                    new JList<>(), "佐藤花子", 1, true, false);
            opaque.set(((JComponent) row).isOpaque());
        });
        assertTrue(opaque.get(), "一覧の選んだ行の地が塗られず、白い文字が見えなくなる");
    }
}
