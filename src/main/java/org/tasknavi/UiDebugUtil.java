package org.tasknavi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public final class UiDebugUtil {

    private static final Logger logger = LoggerFactory.getLogger(UiDebugUtil.class);

    private UiDebugUtil() {
    }

    public static void dumpComponentTree(String label, Component root) {
        if (!isEnabled()) {
            return;
        }
        logger.info("[UI_DEBUG] {}", label);
        dumpComponent(root, 0);
    }

    public static void logButtonState(String label, AbstractButton button) {
        if (!isEnabled() || button == null) {
            return;
        }
        Font font = button.getFont();
        String text = button.getText();
        logger.info(
                "[UI_DEBUG] {} text={} preferred={} min={} font={}/{} canDisplayCalendar={}",
                label,
                quote(text),
                dimensionToString(button.getPreferredSize()),
                dimensionToString(button.getMinimumSize()),
                font == null ? "null" : font.getFontName(),
                font == null ? -1 : font.getSize(),
                font != null && font.canDisplay(0x1F4C5)
        );
    }

    private static void dumpComponent(Component component, int depth) {
        if (component == null) {
            logger.info("[UI_DEBUG] {}<null>", "  ".repeat(depth));
            return;
        }
        String indent = "  ".repeat(depth);
        logger.info("[UI_DEBUG] {}{}", indent, describe(component));
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                dumpComponent(child, depth + 1);
            }
        }
    }

    private static String describe(Component component) {
        Font font = component.getFont();
        StringBuilder sb = new StringBuilder();
        sb.append(component.getClass().getSimpleName());
        sb.append(" name=").append(quote(component.getName()));
        sb.append(" bounds=").append(rectangleToString(component.getBounds()));
        sb.append(" preferred=").append(dimensionToString(component.getPreferredSize()));
        sb.append(" visible=").append(component.isVisible());
        if (font != null) {
            sb.append(" font=").append(font.getFontName()).append("/").append(font.getSize());
        }
        if (component instanceof JLabel) {
            sb.append(" text=").append(quote(((JLabel) component).getText()));
        } else if (component instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) component;
            sb.append(" text=").append(quote(button.getText()));
            if (font != null) {
                sb.append(" canDisplayCalendar=").append(font.canDisplay(0x1F4C5));
            }
        } else if (component instanceof JTextField) {
            sb.append(" text=").append(quote(((JTextField) component).getText()));
        }
        return sb.toString();
    }

    private static String dimensionToString(Dimension dimension) {
        if (dimension == null) {
            return "null";
        }
        return dimension.width + "x" + dimension.height;
    }

    private static String rectangleToString(Rectangle rectangle) {
        if (rectangle == null) {
            return "null";
        }
        return rectangle.x + "," + rectangle.y + " " + rectangle.width + "x" + rectangle.height;
    }

    private static String quote(String value) {
        return value == null ? "<null>" : "\"" + value + "\"";
    }

    public static boolean isEnabled() {
        String flag = System.getProperty("tasknavi.uiDebug");
        if (flag == null || flag.isBlank()) {
            flag = System.getenv("TASKNAVI_UI_DEBUG");
        }
        return Boolean.parseBoolean(flag);
    }
}
