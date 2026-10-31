package org.tasknavi.io;

import org.tasknavi.AppMessages;
import org.tasknavi.model.Priority;
import org.tasknavi.model.Task;
import org.tasknavi.model.TaskStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * タスク一覧 ⇔ CSV。Excel で開いて編集し、そのまま取り込み直せる形にする。
 * <p>
 * 読み込みは 1 行ずつ検証し、駄目な行は理由付きで {@link ReadResult#errors()} に回す（ほかの行は取り込める）。
 * 親子関係は「ID」「親ID」列で表し、取り込み時の ID は振り直す前提なので、
 * 返すタスクの ID・親 ID はファイル内だけで通じる仮の番号になる。
 */
public final class TaskCsv {

    public static final List<String> HEADER = List.of(
            "ID", "親ID", "階層", "タスク名", "担当者", "開始日", "終了日", "状態", "優先度", "進捗(%)", "メモ");
    public static final int MAX_LEVEL = 3;

    public record RowError(int lineNumber, String message) {
    }

    public record ReadResult(List<Task> tasks, List<RowError> errors) {
    }

    private enum Column { ID, PARENT_ID, LEVEL, NAME, ASSIGNEE, START, END, STATUS, PRIORITY, PROGRESS, MEMO }

    /** 見出しの別名。旧形式（英語見出し・「進捗率(% )」）の CSV も読めるようにしている。 */
    private static final Map<String, Column> HEADER_ALIASES = Map.ofEntries(
            Map.entry("id", Column.ID), Map.entry("taskid", Column.ID), Map.entry("タスクid", Column.ID),
            Map.entry("親id", Column.PARENT_ID), Map.entry("parentid", Column.PARENT_ID),
            Map.entry("親タスクid", Column.PARENT_ID),
            Map.entry("階層", Column.LEVEL), Map.entry("level", Column.LEVEL), Map.entry("レベル", Column.LEVEL),
            Map.entry("タスク名", Column.NAME), Map.entry("名前", Column.NAME),
            Map.entry("title", Column.NAME), Map.entry("name", Column.NAME),
            Map.entry("担当者", Column.ASSIGNEE), Map.entry("assignee", Column.ASSIGNEE),
            Map.entry("開始日", Column.START), Map.entry("startdate", Column.START),
            Map.entry("終了日", Column.END), Map.entry("期限", Column.END), Map.entry("enddate", Column.END),
            Map.entry("状態", Column.STATUS), Map.entry("ステータス", Column.STATUS), Map.entry("status", Column.STATUS),
            Map.entry("優先度", Column.PRIORITY), Map.entry("priority", Column.PRIORITY),
            Map.entry("進捗(%)", Column.PROGRESS), Map.entry("進捗率(%)", Column.PROGRESS),
            Map.entry("進捗", Column.PROGRESS), Map.entry("進捗率", Column.PROGRESS),
            Map.entry("progress", Column.PROGRESS),
            Map.entry("メモ", Column.MEMO), Map.entry("memo", Column.MEMO));

    private static final DateTimeFormatter DATE_INPUT =
            DateTimeFormatter.ofPattern("uuuu/M/d").withResolverStyle(ResolverStyle.STRICT);

    private TaskCsv() {
    }

    // ---------------------------------------------------------------- 書き出し

    public static void write(Path file, List<Task> tasks) throws IOException {
        Files.write(file, CsvCodec.encodeForExcel(toCsv(tasks)));
    }

    /** 親の直後に子が並ぶ順（WBS と同じ見え方）で書く。 */
    public static String toCsv(List<Task> tasks) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(HEADER);
        for (Task task : treeOrder(tasks)) {
            rows.add(List.of(
                    String.valueOf(task.getId()),
                    hasParent(task) ? String.valueOf(task.getParentId()) : "",
                    String.valueOf(task.getLevel()),
                    protectFromFormula(task.getName()),
                    protectFromFormula(task.getAssignee()),
                    task.getStartDate() == null ? "" : task.getStartDate().toString(),
                    task.getEndDate() == null ? "" : task.getEndDate().toString(),
                    task.getStatus().getLabel(),
                    task.getPriority().getLabel(),
                    String.valueOf(task.getProgress()),
                    protectFromFormula(task.getMemo())));
        }
        return CsvCodec.format(rows);
    }

    static List<Task> treeOrder(List<Task> tasks) {
        Map<Integer, List<Task>> children = new HashMap<>();
        Set<Integer> ids = new HashSet<>();
        for (Task task : tasks) {
            ids.add(task.getId());
        }
        List<Task> roots = new ArrayList<>();
        for (Task task : tasks) {
            if (hasParent(task) && ids.contains(task.getParentId())) {
                children.computeIfAbsent(task.getParentId(), k -> new ArrayList<>()).add(task);
            } else {
                roots.add(task);
            }
        }
        Comparator<Task> siblingOrder = Comparator.comparingInt(Task::getOrderIndex).thenComparingInt(Task::getId);
        roots.sort(Comparator.comparingInt(Task::getLevel).thenComparing(siblingOrder));
        children.values().forEach(list -> list.sort(siblingOrder));

        List<Task> ordered = new ArrayList<>(tasks.size());
        Set<Integer> visited = new HashSet<>();
        for (Task root : roots) {
            appendSubtree(root, children, visited, ordered);
        }
        return ordered;
    }

    private static void appendSubtree(Task task, Map<Integer, List<Task>> children, Set<Integer> visited,
                                      List<Task> out) {
        if (!visited.add(task.getId())) {
            return;
        }
        out.add(task);
        for (Task child : children.getOrDefault(task.getId(), List.of())) {
            appendSubtree(child, children, visited, out);
        }
    }

    /** Excel が「=」などで始まる値を数式として実行しないよう、先頭に ' を付ける。読み込み時に外す。 */
    static String protectFromFormula(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return isFormulaStart(value.charAt(0)) ? "'" + value : value;
    }

    private static String unprotect(String value) {
        if (value.length() >= 2 && value.charAt(0) == '\'' && isFormulaStart(value.charAt(1))) {
            return value.substring(1);
        }
        return value;
    }

    private static boolean isFormulaStart(char ch) {
        return ch == '=' || ch == '+' || ch == '-' || ch == '@';
    }

    // ---------------------------------------------------------------- 読み込み

    public static ReadResult read(Path file) throws IOException {
        return parse(CsvCodec.decode(Files.readAllBytes(file)));
    }

    public static ReadResult parse(String text) {
        List<CsvCodec.Row> rows = CsvCodec.parse(text, CsvCodec.detectDelimiter(text));
        if (rows.isEmpty()) {
            return new ReadResult(List.of(), List.of(new RowError(1, AppMessages.get("csv.error.empty", "ファイルにデータがありません。"))));
        }
        Map<Column, Integer> columns = mapHeader(rows.get(0).fields());
        if (!columns.containsKey(Column.NAME)) {
            return new ReadResult(List.of(), List.of(new RowError(rows.get(0).lineNumber(),
                    AppMessages.get("csv.error.no.name.column", "1 行目に「タスク名」の列がありません。TaskNavi で書き出した CSV の見出し行を使ってください。"))));
        }

        List<ParsedRow> parsed = new ArrayList<>();
        Map<String, ParsedRow> byKey = new HashMap<>();
        for (CsvCodec.Row row : rows.subList(1, rows.size())) {
            ParsedRow parsedRow = parseRow(row, columns);
            if (parsedRow.key != null) {
                ParsedRow first = byKey.putIfAbsent(parsedRow.key, parsedRow);
                if (first != null) {
                    parsedRow.problems.add(AppMessages.format("csv.error.duplicate.id", "ID「{0}」が {1} 行目と重複しています",
                            parsedRow.key, String.valueOf(first.lineNumber)));
                }
            }
            parsed.add(parsedRow);
        }
        for (ParsedRow row : parsed) {
            resolveHierarchy(row, byKey, new HashSet<>());
        }
        return buildResult(parsed);
    }

    private static Map<Column, Integer> mapHeader(List<String> header) {
        Map<Column, Integer> columns = new LinkedHashMap<>();
        for (int i = 0; i < header.size(); i++) {
            Column column = HEADER_ALIASES.get(normalizeHeader(header.get(i)));
            if (column != null) {
                columns.putIfAbsent(column, i);
            }
        }
        return columns;
    }

    private static String normalizeHeader(String header) {
        return header.replace('（', '(').replace('）', ')').replace('％', '%')
                .replaceAll("\\s", "")
                .toLowerCase();
    }

    private static ParsedRow parseRow(CsvCodec.Row row, Map<Column, Integer> columns) {
        ParsedRow parsed = new ParsedRow(row.lineNumber());
        parsed.key = blankToNull(field(row, columns, Column.ID));
        String parentKey = blankToNull(field(row, columns, Column.PARENT_ID));
        parsed.parentKey = "0".equals(parentKey) ? null : parentKey;
        parsed.declaredLevel = parseLevel(field(row, columns, Column.LEVEL), parsed.problems);

        String name = unprotect(field(row, columns, Column.NAME));
        if (name.isEmpty()) {
            parsed.problems.add(AppMessages.get("csv.error.name.empty", "タスク名が空です"));
        }
        LocalDate start = parseDate(field(row, columns, Column.START), AppMessages.get("csv.label.start", "開始日"), parsed.problems);
        LocalDate end = parseDate(field(row, columns, Column.END), AppMessages.get("csv.label.end", "終了日"), parsed.problems);
        if (start != null && end != null && start.isAfter(end)) {
            parsed.problems.add(AppMessages.get("csv.error.dates.order", "開始日が終了日より後になっています"));
        }

        Task task = new Task(0, name, null, 1, 0, 0,
                parseStatus(field(row, columns, Column.STATUS), parsed.problems), start, end);
        task.setProgress(parseProgress(field(row, columns, Column.PROGRESS), parsed.problems));
        task.setPriority(parsePriority(field(row, columns, Column.PRIORITY), parsed.problems));
        task.setAssignee(unprotect(field(row, columns, Column.ASSIGNEE)));
        task.setMemo(unprotect(field(row, columns, Column.MEMO)));
        parsed.task = task;
        return parsed;
    }

    private static String field(CsvCodec.Row row, Map<Column, Integer> columns, Column column) {
        Integer index = columns.get(column);
        if (index == null || index >= row.fields().size()) {
            return "";
        }
        return row.fields().get(index).strip();
    }

    private static Integer parseLevel(String text, List<String> problems) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            int level = Integer.parseInt(text);
            if (level >= 1 && level <= MAX_LEVEL) {
                return level;
            }
        } catch (NumberFormatException ignored) {
            // 下でまとめて伝える
        }
        problems.add(AppMessages.format("csv.error.level", "階層「{0}」は 1〜{1} の数字にしてください", text, String.valueOf(MAX_LEVEL)));
        return null;
    }

    /** 2026-10-24 / 2026/10/24 / 2026.10.24 / 2026年10月24日 を受け付ける。Excel が付ける時刻は無視する。 */
    static LocalDate parseDate(String text, String label, List<String> problems) {
        if (text.isEmpty()) {
            return null;
        }
        String datePart = text.split("[ T]", 2)[0];
        String normalized = datePart.replace('-', '/').replace('.', '/')
                .replace("年", "/").replace("月", "/").replace("日", "");
        try {
            return LocalDate.parse(normalized, DATE_INPUT);
        } catch (DateTimeParseException e) {
            problems.add(AppMessages.format("csv.error.date", "{0}「{1}」を日付として読めません（例: 2026/10/24）", label, text));
            return null;
        }
    }

    private static TaskStatus parseStatus(String text, List<String> problems) {
        if (text.isEmpty()) {
            return TaskStatus.NOT_STARTED;
        }
        for (TaskStatus status : TaskStatus.values()) {
            if (status.getLabel().equals(text) || status.name().equalsIgnoreCase(text)) {
                return status;
            }
        }
        problems.add(AppMessages.format("csv.error.status", "状態「{0}」は 未着手・進行中・完了 のどれかにしてください", text));
        return TaskStatus.NOT_STARTED;
    }

    private static Priority parsePriority(String text, List<String> problems) {
        if (text.isEmpty()) {
            return Priority.MEDIUM;
        }
        for (Priority priority : Priority.values()) {
            if (priority.getLabel().equals(text) || priority.name().equalsIgnoreCase(text)) {
                return priority;
            }
        }
        problems.add(AppMessages.format("csv.error.priority", "優先度「{0}」は 高・中・低 のどれかにしてください", text));
        return Priority.MEDIUM;
    }

    private static int parseProgress(String text, List<String> problems) {
        if (text.isEmpty()) {
            return 0;
        }
        try {
            double value = Double.parseDouble(text.replace("%", "").replace("％", "").strip());
            if (value >= 0 && value <= 100) {
                return (int) Math.round(value);
            }
        } catch (NumberFormatException ignored) {
            // 下でまとめて伝える
        }
        problems.add(AppMessages.format("csv.error.progress", "進捗「{0}」は 0〜100 の数字にしてください", text));
        return 0;
    }

    private static void resolveHierarchy(ParsedRow row, Map<String, ParsedRow> byKey, Set<ParsedRow> visiting) {
        if (row.resolved) {
            return;
        }
        if (!visiting.add(row)) {
            row.problems.add(AppMessages.get("csv.error.cycle", "親子関係が循環しています"));
            row.resolved = true;
            return;
        }
        int level = 1;
        if (row.parentKey != null) {
            ParsedRow parent = byKey.get(row.parentKey);
            if (parent == null || parent == row) {
                row.problems.add(AppMessages.format("csv.error.parent.missing", "親ID「{0}」の行が見つかりません", row.parentKey));
            } else {
                resolveHierarchy(parent, byKey, visiting);
                if (!parent.problems.isEmpty()) {
                    row.problems.add(AppMessages.format("csv.error.parent.invalid",
                            "親の行（{0} 行目）を取り込めないため、この行も取り込めません", String.valueOf(parent.lineNumber)));
                } else if (parent.level >= MAX_LEVEL) {
                    row.problems.add(AppMessages.format("csv.error.too.deep", "階層 {0} のタスクの下には追加できません",
                            String.valueOf(MAX_LEVEL)));
                } else {
                    level = parent.level + 1;
                    row.parent = parent;
                }
            }
        }
        if (row.declaredLevel != null && row.problems.isEmpty() && row.declaredLevel != level) {
            row.problems.add(row.parent == null
                    ? AppMessages.get("csv.error.root.level", "親ID が空の行は階層 1（プロジェクト）です")
                    : AppMessages.format("csv.error.level.expected", "階層は {0} にしてください（親が階層 {1} のため）",
                            String.valueOf(level), String.valueOf(level - 1)));
        }
        row.level = level;
        row.resolved = true;
        visiting.remove(row);
    }

    private static ReadResult buildResult(List<ParsedRow> parsed) {
        List<Task> tasks = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        Map<ParsedRow, Integer> siblingCount = new HashMap<>();
        int rootCount = 0;
        int nextId = 1;
        for (ParsedRow row : parsed) {
            if (row.problems.isEmpty()) {
                row.newId = nextId++;
            }
        }
        for (ParsedRow row : parsed) {
            if (!row.problems.isEmpty()) {
                errors.add(new RowError(row.lineNumber, String.join(AppMessages.get("csv.error.separator", "。"), row.problems)
                        + AppMessages.get("csv.error.end", "。")));
                continue;
            }
            Task task = row.task;
            task.setId(row.newId);
            task.setLevel(row.level);
            if (row.parent == null) {
                task.setParentId(null);
                task.setOrderIndex(rootCount++);
            } else {
                task.setParentId(row.parent.newId);
                int order = siblingCount.merge(row.parent, 1, Integer::sum) - 1;
                task.setOrderIndex(order);
            }
            tasks.add(task);
        }
        return new ReadResult(tasks, errors);
    }

    private static boolean hasParent(Task task) {
        return task.getParentId() != null && task.getParentId() != 0;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static final class ParsedRow {
        private final int lineNumber;
        private final List<String> problems = new ArrayList<>();
        private String key;
        private String parentKey;
        private Integer declaredLevel;
        private Task task;
        private ParsedRow parent;
        private int level;
        private int newId;
        private boolean resolved;

        private ParsedRow(int lineNumber) {
            this.lineNumber = lineNumber;
        }
    }
}
