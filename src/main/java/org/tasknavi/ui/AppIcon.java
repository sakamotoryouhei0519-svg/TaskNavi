package org.tasknavi.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * アプリのアイコン（青い角丸に白いチェック）。
 * 窓・通知領域・配布用 exe（src/main/packaging/TaskNavi.ico）で同じ絵を使う。
 */
public final class AppIcon {

    private static final Color BACKGROUND = new Color(37, 99, 235);
    private static final int[] WINDOW_SIZES = {16, 20, 24, 32, 40, 48, 64, 128, 256};

    private AppIcon() {
    }

    public static BufferedImage render(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            float scale = size / 32f;
            int arc = Math.round(10 * scale);
            g2.setColor(BACKGROUND);
            g2.fillRoundRect(0, 0, size, size, arc, arc);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(Math.max(1.5f, 4f * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolyline(
                    new int[]{Math.round(8 * scale), Math.round(14 * scale), Math.round(25 * scale)},
                    new int[]{Math.round(17 * scale), Math.round(23 * scale), Math.round(10 * scale)},
                    3);
        } finally {
            g2.dispose();
        }
        return image;
    }

    /** 窓のタイトルバーとタスクバー用。Windows が画面の拡大率に合う大きさを選ぶ。 */
    public static List<Image> windowIcons() {
        return java.util.Arrays.stream(WINDOW_SIZES)
                .mapToObj(size -> (Image) render(size))
                .toList();
    }
}
