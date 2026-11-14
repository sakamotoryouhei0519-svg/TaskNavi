package org.tasknavi.ui;

import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class AppFontsTest {

    @Test
    void japaneseKeepsTheUsualFont() {
        assertEquals(Font.SANS_SERIF, AppFonts.chooseFamily(Locale.JAPAN, () -> Set.of("Meiryo UI")));
    }

    @Test
    void japaneseDoesNotListTheFonts() {
        assertEquals(Font.SANS_SERIF, AppFonts.chooseFamily(Locale.JAPAN, () -> fail("日本語のときは書体の一覧を取らない")));
    }

    @Test
    void otherLanguagesUseAJapaneseGothicFont() {
        assertEquals("Meiryo UI", AppFonts.chooseFamily(Locale.US, () -> Set.of("Yu Gothic UI", "Meiryo UI")));
        assertEquals("Yu Gothic UI", AppFonts.chooseFamily(Locale.US, () -> Set.of("Yu Gothic UI")));
    }

    @Test
    void withoutAJapaneseFontTheUsualFontStays() {
        assertEquals(Font.SANS_SERIF, AppFonts.chooseFamily(Locale.US, () -> Set.of("Arial")));
    }
}
