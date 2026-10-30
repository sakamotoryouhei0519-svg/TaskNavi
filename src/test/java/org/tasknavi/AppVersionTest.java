package org.tasknavi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppVersionTest {

    @Test
    void comparesEachNumberNotTheText() {
        assertTrue(AppVersion.compare("1.10.0", "1.9.0") > 0);
        assertTrue(AppVersion.compare("1.2.0", "1.3.0") < 0);
        assertTrue(AppVersion.compare("2.0", "1.99.99") > 0);
        assertEquals(0, AppVersion.compare("1.2", "1.2.0"));
        assertEquals(0, AppVersion.compare("1.2.0", "1.2.0"));
    }

    @Test
    void wordsInTheVersionCountAsZero() {
        assertEquals(0, AppVersion.compare("1.3.0-SNAPSHOT", "1.3.0"));
        assertTrue(AppVersion.compare("1.3.0-beta", "1.2.9") > 0);
    }

    @Test
    void testsRunWithoutAReleaseVersion() {
        assertNull(AppVersion.release());
    }
}
