package org.example;

import javax.swing.*;
import java.awt.*;

public class EmojiTest {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            JFrame frame = new JFrame("EmojiTest");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JPanel panel = new JPanel();
            panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            panel.add(createSampleRow("IconManager", IconManager.getIconButton(IconManager.IconType.CALENDAR)));
            panel.add(Box.createVerticalStrut(12));
            panel.add(createSampleRow("Segoe UI Emoji", createEmojiButton("Segoe UI Emoji")));
            panel.add(Box.createVerticalStrut(12));
            panel.add(createSampleRow("Segoe UI Symbol", createEmojiButton("Segoe UI Symbol")));
            panel.add(Box.createVerticalStrut(12));
            panel.add(createSampleRow("Dialog", createEmojiButton("Dialog")));

            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            UiDebugUtil.dumpComponentTree("EmojiTest", frame);
        });
    }

    private static JPanel createSampleRow(String label, JButton button) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.add(new JLabel(label + ":"));
        button.setPreferredSize(new Dimension(48, 36));
        row.add(button);
        UiDebugUtil.logButtonState("EmojiTest/" + label, button);
        return row;
    }

    private static JButton createEmojiButton(String fontName) {
        JButton button = new JButton(new String(Character.toChars(0x1F4C5)));
        button.setFont(new Font(fontName, Font.PLAIN, 20));
        return button;
    }
}
