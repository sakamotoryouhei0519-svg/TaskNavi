package org.example;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 【CSV / JSON 形式のデータ入出力を担当するユーティリティ】
 * このクラスは、タスク一覧をファイルへ保存したり、別のファイルから復元したりするための
 * 変換処理をまとめて管理します。
 * <p>
 * Java では文字列の CSV とオブジェクトの Task を直接やり取りできないため、
 * ここでは各フィールドを変換しながら保存・復元を行います。
 */
public class CsvUtil {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CsvUtil.class);

    /**
     * 【CSV 形式でエクスポートする】
     * タスク一覧を CSV へ書き出し、Excel や他のツールから参照しやすくします。
     */
    public static void exportToCsv(File file, List<Task> tasks) throws IOException {
        exportDelimited(file, tasks,
                "taskId,parentId,level,title,assignee,startDate,endDate,status,priority,progress",
                ",", false, true);
    }

    /**
     * 【Excel で文字化けしないように BOM 付き CSV を出力する】
     * UTF-8 だけでは Excel で文字化けすることがあるため、BOM を先頭に付与しています。
     */
    public static void exportToCsvWithBom(File file, List<Task> tasks) throws IOException {
        exportDelimited(file, tasks,
                "タスクID,親タスクID,レベル,タスク名,担当者,開始日,終了日,状態,優先度,進捗率(% )",
                ",", true, true);
    }

    /**
     * 【TSV 形式でエクスポートする】
     * 区切り文字をタブにすることで、カンマを含む値との衝突を避けやすくなります。
     */
    public static void exportToTsv(File file, List<Task> tasks) throws IOException {
        exportDelimited(file, tasks,
                "タスクID\t親タスクID\tレベル\tタスク名\t担当者\t開始日\t終了日\t状態\t優先度\t進捗率(% )",
                "\t", true, false);
    }

    /**
     * 【JSON 形式へ出力する】
     * Gson を使って Task のリストを JSON として保存します。
     * これにより、構造を保ったまま複雑なタスク情報を再利用できます。
     */
    public static void exportToJson(File file, List<Task> tasks) throws IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(tasks, writer);
        }
    }

    /**
     * 【JSON からタスク一覧を読み込む】
     * JSON の文字列を Java の List<Task> に戻します。
     * LocalDate のような Java 独自型はアダプターが必要です。
     */
    public static List<Task> importFromJson(File file) throws IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, new TypeToken<List<Task>>() {
            }.getType());
        }
    }

    /**
     * 【CSV からタスク一覧を読み込む】
     * ヘッダー行をスキップして、各行を Task オブジェクトへ変換します。
     */
    public static List<Task> importFromCsv(File file) throws IOException {
        List<Task> tasks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String currentLine = line.startsWith("\uFEFF") ? line.substring(1) : line;

                if (currentLine.trim().isEmpty()) {
                    continue;
                }

                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                try {
                    String[] cols = parseCsvLine(currentLine);
                    if (cols.length < 9) {
                        System.err.println("CSV import skipped invalid line " + lineNumber + ": column count too small");
                        continue;
                    }

                    int taskId = Integer.parseInt(cols[0].trim());
                    Integer parentId = cols[1].trim().isEmpty() ? 0 : Integer.parseInt(cols[1].trim());
                    int level = Integer.parseInt(cols[2].trim());
                    String title = cols[3].trim();
                    String assignee = cols[4].trim();

                    if (title.isEmpty()) {
                        throw new IllegalArgumentException("title is blank");
                    }

                    LocalDate startDate = LocalDate.parse(cols[5].trim());
                    LocalDate endDate = LocalDate.parse(cols[6].trim());

                    String status = cols[7].trim();
                    String priority = (cols.length >= 9) ? cols[8].trim() : Task.DEFAULT_PRIORITY;
                    int progress = (cols.length >= 10) ? Integer.parseInt(cols[9].trim()) : 0;

                    Task task = new Task(taskId, title, parentId, level, 0, progress, status, startDate, endDate);
                    task.setAssignee(assignee);
                    task.setPriority(Priority.fromString(priority));
                    tasks.add(task);
                } catch (Exception ex) {
                    logger.error("CSV import skipped invalid line " + lineNumber + ": " + ex.getMessage());
                }
            }
        }
        return tasks;
    }

    private static void exportDelimited(File file, List<Task> tasks, String header, String delimiter, boolean useBom, boolean quoteFields) throws IOException {
        try (OutputStream os = new FileOutputStream(file);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            if (useBom) {
                byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
                os.write(bom);
            }

            writer.println(header);

            for (Task t : tasks) {
                List<String> values = new ArrayList<>();
                values.add(String.valueOf(t.getTaskId()));
                values.add(t.getParentId() == null || t.getParentId() == 0 ? "" : String.valueOf(t.getParentId()));
                values.add(String.valueOf(t.getLevel()));
                values.add(normalizeForDelimitedField(t.getTitle(), quoteFields));
                values.add(normalizeForDelimitedField(t.getAssignee(), quoteFields));
                values.add(t.getStartDate() == null ? "" : t.getStartDate().toString());
                values.add(t.getEndDate() == null ? "" : t.getEndDate().toString());
                values.add(normalizeForDelimitedField(t.getStatusCode(), quoteFields));
                values.add(normalizeForDelimitedField(t.getPriorityCode(), quoteFields));
                values.add(String.valueOf(t.getProgress()));

                writer.println(joinDelimitedValues(values, delimiter));
            }
        }
    }

    private static String normalizeForDelimitedField(String value, boolean quoteFields) {
        if (value == null) {
            return "";
        }

        String sanitized = value
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\t", " ");

        if (!quoteFields) {
            return sanitized;
        }

        if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n")) {
            return "\"" + sanitized.replace("\"", "\"\"") + "\"";
        }
        return sanitized;
    }

    private static String joinDelimitedValues(List<String> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private static String[] parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    buffer.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                columns.add(buffer.toString());
                buffer.setLength(0);
            } else {
                buffer.append(ch);
            }
        }

        columns.add(buffer.toString());
        return columns.toArray(new String[0]);
    }

    /**
     * 【LocalDate を JSON 用の文字列に変換するアダプター】
     * Gson は標準では LocalDate を扱えないため、
     * 文字列へ変換して保存し、読み込み時に戻す仕組みを使います。
     */
    private static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        @Override
        public JsonElement serialize(LocalDate date, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(date.toString());
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return LocalDate.parse(json.getAsString());
        }
    }
}