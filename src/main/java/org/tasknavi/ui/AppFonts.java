package org.tasknavi.ui;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 画面の文字の書体。
 * Java の「SansSerif」は、起動したときの言語が日本語でないと、日本語を中国語の明朝体（SimSun）で描く。
 * そのときだけ、日本語が読めるゴシック体を先に使う。その書体に無い記号は、StyleContext が他の書体で補う。
 */
final class AppFonts {

    private static final List<String> JAPANESE_GOTHIC = List.of("Meiryo UI", "Yu Gothic UI", "MS UI Gothic");

    static final String FAMILY = chooseFamily(Locale.getDefault(), availableFamilies());

    private AppFonts() {
    }

    static String chooseFamily(Locale locale, Set<String> available) {
        if ("ja".equals(locale.getLanguage())) {
            return Font.SANS_SERIF;
        }
        for (String family : JAPANESE_GOTHIC) {
            if (available.contains(family)) {
                return family;
            }
        }
        return Font.SANS_SERIF;
    }

    static Font font(int style, int size) {
        return StyleContext.getDefaultStyleContext().getFont(FAMILY, style, size);
    }

    /** こちらで書体を決めていない部品（表・一覧・メニューなど）も、Look &amp; Feel の大きさと太さのまま同じ書体にする。 */
    static void applyToDefaults() {
        if (Font.SANS_SERIF.equals(FAMILY)) {
            return;
        }
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        for (Object key : new ArrayList<>(defaults.keySet())) {
            if (defaults.get(key) instanceof Font current) {
                UIManager.put(key, new FontUIResource(font(current.getStyle(), current.getSize())));
            }
        }
    }

    private static Set<String> availableFamilies() {
        return new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
    }
}
