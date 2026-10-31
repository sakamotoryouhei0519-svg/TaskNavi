package org.tasknavi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tasknavi.io.TaskCsv;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 文言のファイルが日本語・英語でそろっていて、コードで使うキーが全部あること。英語を選ぶと英語で出ること。 */
class AppMessagesTest {

    private static final Pattern GET = Pattern.compile("AppMessages\\.get\\(\\s*\"([\\w.\\-]+)\"");
    private static final Pattern FORMAT = Pattern.compile("AppMessages\\.format\\(\\s*\"([\\w.\\-]+)\"");

    @AfterEach
    void restoreLocale() {
        AppMessages.setLocale(Locale.JAPAN);
    }

    @Test
    void everyLanguageHasTheSameKeys() throws IOException {
        Set<String> base = load("messages.properties").stringPropertyNames();

        assertEquals(base, load("messages_ja.properties").stringPropertyNames());
        assertEquals(base, load("messages_en.properties").stringPropertyNames());
    }

    @Test
    void everyKeyUsedInTheCodeIsInTheFiles() throws IOException {
        Set<String> defined = load("messages_en.properties").stringPropertyNames();
        Set<String> missing = new TreeSet<>(usedKeys(GET));
        missing.addAll(usedKeys(FORMAT));
        missing.removeAll(defined);

        assertEquals(Set.of(), missing);
    }

    /** MessageFormat では ' 1つが引用の始まりになり、文から消える。'' と書く。 */
    @Test
    void formattedMessagesHaveNoLoneQuote() throws IOException {
        for (String file : List.of("messages_ja.properties", "messages_en.properties")) {
            Properties messages = load(file);
            for (String key : usedKeys(FORMAT)) {
                String value = messages.getProperty(key, "");
                assertFalse(value.replace("''", "").contains("'"), file + " " + key);
            }
        }
    }

    @Test
    void errorsAreInEnglishWhenEnglishIsChosen() {
        AppMessages.setLocale(Locale.ENGLISH);

        assertEquals("Task ID 1234 was not found.", AppException.taskNotFound(1234).getUserMessage());
        TaskCsv.ReadResult read = TaskCsv.parse("ID,親ID,タスク名,進捗(%)\n1,,A,150\n2,9,B,\n");
        assertEquals("Progress \"150\" must be a number from 0 to 100.", read.errors().get(0).message());
        assertEquals("No row has the parent ID \"9\".", read.errors().get(1).message());
    }

    @Test
    void severalCsvProblemsAreJoinedAsSentences() {
        String csv = "ID,親ID,タスク名,開始日,進捗(%)\n1,,,2026/13/01,abc\n";

        AppMessages.setLocale(Locale.JAPAN);
        String ja = TaskCsv.parse(csv).errors().get(0).message();
        AppMessages.setLocale(Locale.ENGLISH);
        String en = TaskCsv.parse(csv).errors().get(0).message();

        assertTrue(ja.startsWith("タスク名が空です。開始日「2026/13/01」"), ja);
        assertTrue(ja.endsWith("の数字にしてください。"), ja);
        assertTrue(en.startsWith("The task name is empty. Start date \"2026/13/01\" is not a date"), en);
        assertTrue(en.endsWith("from 0 to 100."), en);
    }

    private static Properties load(String name) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = AppMessagesTest.class.getResourceAsStream("/" + name)) {
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return properties;
    }

    private static Set<String> usedKeys(Pattern call) throws IOException {
        Set<String> keys = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = call.matcher(Files.readString(file));
                while (matcher.find()) {
                    keys.add(matcher.group(1));
                }
            }
        }
        return keys;
    }
}
