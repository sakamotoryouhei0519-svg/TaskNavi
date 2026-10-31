package org.tasknavi.tools;

import org.tasknavi.io.CsvCodec;
import org.tasknavi.io.TaskCsv;
import org.tasknavi.model.Priority;
import org.tasknavi.model.TaskStatus;
import org.tasknavi.persistence.AppDataLocation;
import org.tasknavi.service.TaskService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 試す専用のデータフォルダを新しく作り、いろいろな場合のタスクを入れる。
 * 日付は動かした日を「今日」として決めるので、いつ作っても期限切れ・今日・明日がそろう。
 * 同じフォルダに、取り込みを試す CSV（全部通るものと、わざと間違えたもの）も書く。
 *
 * <p>動かし方は README の「開発メモ」。いつものデータ（%APPDATA%\TaskNavi）と、もうデータがあるフォルダには書かない。
 * 名前が Test で終わらないので、mvn test では動かない。</p>
 */
public final class TestDataTool {

    static final String GOOD_CSV = "test-data.csv";
    static final String BAD_CSV = "test-data-bad.csv";
    /** test-data-bad.csv のうち、取り込めない行の数。 */
    static final int BAD_ROW_COUNT = 10;

    private static final int MANY_FIRST_ID = 10_001;
    private static final int PHASES_PER_PROJECT = 10;
    private static final int TASKS_PER_PHASE = 8;
    private static final String[] PEOPLE = {"佐藤花子", "山田太郎", "鈴木一郎", "高橋恵子", "田中次郎"};

    private TestDataTool() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("使い方: TestDataTool <新しいデータのフォルダ> [足すタスクの数]");
            System.exit(2);
        }
        Path dir = Path.of(args[0]).toAbsolutePath().normalize();
        int extra = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        String appData = System.getenv("APPDATA");
        if (appData != null && dir.equals(Path.of(appData, "TaskNavi").toAbsolutePath().normalize())) {
            System.err.println("いつものデータのフォルダには書きません: " + dir);
            System.exit(1);
        }
        if (Files.exists(dir.resolve(AppDataLocation.DB_FILE_NAME))) {
            System.err.println("もうデータがあります。フォルダを消してから、もう一度動かしてください: " + dir);
            System.exit(1);
        }
        Files.createDirectories(dir);
        System.setProperty("tasknavi.dataDir", dir.toString());

        LocalDate today = LocalDate.now();
        List<List<String>> rows = new ArrayList<>(goodRows(today));
        if (extra > 0) {
            rows.addAll(manyRows(extra, today).subList(1, extra + 1));
        }
        TaskCsv.ReadResult result = TaskCsv.parse(CsvCodec.format(rows));
        if (!result.errors().isEmpty()) {
            throw new IllegalStateException("見本の行に間違いがあります: " + result.errors());
        }
        TaskService.createDefault().importTasks(result.tasks(), false);

        Files.write(dir.resolve(GOOD_CSV), CsvCodec.encodeForExcel(CsvCodec.format(goodRows(today))));
        Files.write(dir.resolve(BAD_CSV), CsvCodec.encodeForExcel(CsvCodec.format(badRows(today))));

        System.out.println("タスクを " + result.tasks().size() + " 件入れました: " + dir);
        System.out.println("取り込みを試す CSV: " + GOOD_CSV + "（全部通る） / " + BAD_CSV
                + "（" + BAD_ROW_COUNT + " 行が理由つきで止まる）");
        System.out.println("このデータで起動するには:");
        System.out.println("  $env:TASKNAVI_DATA_DIR = \"" + dir + "\"");
        System.out.println("  & \"C:\\TaskNavi\\TaskNavi\\TaskNavi.exe\"");
        System.exit(0);
    }

    /** 期限・状態・文字・カレンダーの重なり・空のプロジェクトなど、画面で確かめたい場合を1つずつ入れる。 */
    static List<List<String>> goodRows(LocalDate today) {
        Rows r = new Rows(today, 1);

        int a = r.add(null, "動作確認A 期限と状態", "佐藤花子", -20, 40, TaskStatus.IN_PROGRESS, Priority.HIGH, 30, "");
        int near = r.add(a, "期限の近いもの", "チーム一律", -12, 5, TaskStatus.IN_PROGRESS, Priority.MEDIUM, 40, "");
        r.add(near, "期限切れ・未着手", "山田太郎", -10, -3, TaskStatus.NOT_STARTED, Priority.HIGH, 0, "期限切れの色で出る");
        r.add(near, "期限切れ・進行中", "鈴木一郎", -7, -1, TaskStatus.IN_PROGRESS, Priority.HIGH, 40, "");
        r.add(near, "今日が期限", "佐藤花子", -3, 0, TaskStatus.IN_PROGRESS, Priority.MEDIUM, 70, "「今日」タブに出る");
        r.add(near, "今日1日だけ", "佐藤花子", 0, 0, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0, "");
        r.add(near, "明日が期限", "高橋恵子", 0, 1, TaskStatus.NOT_STARTED, Priority.LOW, 0, "");
        r.add(near, "今週中", "田中次郎", 1, 5, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0, "");
        r.add(near, "期限は過ぎたが完了", "山田太郎", -12, -2, TaskStatus.COMPLETED, Priority.MEDIUM, 100,
                "完了なら期限切れの色にならない");
        int later = r.add(a, "先の予定", "チーム一律", 7, 40, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0, "");
        r.add(later, "来週から", "鈴木一郎", 7, 12, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0, "");
        r.add(later, "来月の作業", "高橋恵子", 30, 40, TaskStatus.NOT_STARTED, Priority.LOW, 0, "");
        r.add(later, "日付なし", "田中次郎", null, null, TaskStatus.NOT_STARTED, Priority.LOW, 0,
                "ガントとカレンダーには出ない");

        int b = r.add(null, "動作確認B 表示の崩れ", "高橋恵子", -5, 20, TaskStatus.IN_PROGRESS, Priority.MEDIUM, 20, "");
        int text = r.add(b, "長い文字・特別な文字", "チーム一律", -5, 10, TaskStatus.IN_PROGRESS, Priority.MEDIUM, 30, "");
        r.add(text, "とても長いタスク名の確認用：お客様向け管理画面の検索条件と一覧表示と CSV 出力の仕様をまとめ、"
                + "レビューで指摘された点をすべて直してから、もう一度レビューを受ける", "高橋恵子", -5, 3,
                TaskStatus.IN_PROGRESS, Priority.HIGH, 60, "名前が切れても、ボタンが押せるか");
        r.add(text, "担当者なし", "", -2, 4, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0, "");
        r.add(text, "'=1+1", "山田太郎", -1, 6, TaskStatus.NOT_STARTED, Priority.LOW, 0,
                "Excel で開いても計算されず、文字のまま");
        r.add(text, "カンマ, と \"引用符\" を含む名前", "鈴木一郎", 0, 7, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0, "");
        r.add(text, "メモが複数行", "佐藤花子", 1, 8, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0,
                "1行目\n2行目: 改行もそのまま残る\n3行目");
        r.add(text, "English name / API v2", "Tanaka", 2, 9, TaskStatus.IN_PROGRESS, Priority.MEDIUM, 10, "");
        r.add(text, "記号 ✅ ★ ① を含む名前", "田中次郎", 3, 10, TaskStatus.NOT_STARTED, Priority.LOW, 0, "");
        int same = r.add(b, "同じ日に重なる", "チーム一律", 2, 2, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0,
                "カレンダーで「+他 n 件」になる");
        for (int i = 1; i <= 6; i++) {
            r.add(same, "重なる作業 " + i, PEOPLE[i % PEOPLE.length], 2, 2, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0, "");
        }
        r.add(b, "タスクのない工程", "チーム一律", 0, 10, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0, "");

        int c = r.add(null, "動作確認C 全部完了", "山田太郎", -60, -30, TaskStatus.COMPLETED, Priority.LOW, 100, "");
        int done = r.add(c, "終わった工程", "チーム一律", -60, -30, TaskStatus.COMPLETED, Priority.LOW, 100, "");
        r.add(done, "終わった作業 1", "山田太郎", -60, -45, TaskStatus.COMPLETED, Priority.LOW, 100, "");
        r.add(done, "終わった作業 2", "鈴木一郎", -44, -30, TaskStatus.COMPLETED, Priority.LOW, 100, "");

        r.add(null, "動作確認D 中身のないプロジェクト", "佐藤花子", 0, 30, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0, "");

        int e = r.add(null, "動作確認E 長い期間", "田中次郎", -90, 180, TaskStatus.IN_PROGRESS, Priority.MEDIUM, 25,
                "ガントで横に長く続く");
        for (int i = 0; i < 3; i++) {
            int phase = r.add(e, "期間 " + (i + 1), "チーム一律", -90 + i * 90, i * 90, TaskStatus.NOT_STARTED,
                    Priority.MEDIUM, 0, "");
            r.add(phase, "月をまたぐ作業 " + (i + 1), PEOPLE[i], -100 + i * 90 + 25, -90 + i * 90 + 45,
                    i == 0 ? TaskStatus.COMPLETED : TaskStatus.NOT_STARTED, Priority.MEDIUM, i == 0 ? 100 : 0, "");
        }
        return r.rows;
    }

    /** 取り込む前に、何行目が何の理由で駄目かが出るのを確かめる。最初の3行だけ通る。 */
    static List<List<String>> badRows(LocalDate today) {
        Rows r = new Rows(today, 1001);
        int project = r.add(null, "取り込めるプロジェクト", "佐藤花子", 0, 30, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0, "");
        int phase = r.add(project, "取り込める工程", "チーム一律", 0, 30, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0, "");
        int task = r.add(phase, "取り込めるタスク", "山田太郎", 0, 5, TaskStatus.NOT_STARTED, Priority.MEDIUM, 0, "");

        r.raw(String.valueOf(r.nextId++), String.valueOf(phase), "3", "状態が「保留」", "", r.day(0), r.day(1), "保留", "中", "0", "");
        r.raw(String.valueOf(r.nextId++), String.valueOf(phase), "3", "開始日が終了日より後", "", r.day(5), r.day(1), "未着手", "中", "0", "");
        r.raw(String.valueOf(r.nextId++), "9999", "", "親IDがない番号", "", r.day(0), r.day(1), "未着手", "中", "0", "");
        r.raw(String.valueOf(r.nextId++), String.valueOf(phase), "3", "日付が読めない", "", "2026/13/40", r.day(1), "未着手", "中", "0", "");
        r.raw(String.valueOf(r.nextId++), String.valueOf(phase), "3", "進捗が150", "", r.day(0), r.day(1), "進行中", "中", "150", "");
        r.raw(String.valueOf(r.nextId++), String.valueOf(phase), "3", "", "", r.day(0), r.day(1), "未着手", "中", "0", "名前が空");
        r.raw(String.valueOf(r.nextId++), String.valueOf(phase), "3", "優先度が「最優先」", "", r.day(0), r.day(1), "未着手", "最優先", "0", "");
        r.raw(String.valueOf(r.nextId++), String.valueOf(task), "", "タスクの下にタスク（4階層目）", "", r.day(0), r.day(1), "未着手", "中", "0", "");
        r.raw(String.valueOf(task), String.valueOf(phase), "3", "IDが上の行と同じ", "", r.day(0), r.day(1), "未着手", "中", "0", "");
        r.raw(String.valueOf(r.nextId++), String.valueOf(phase), "2", "階層が親と合わない", "", r.day(0), r.day(1), "未着手", "中", "0", "");
        return r.rows;
    }

    /** 重さを試すための行。プロジェクト → 工程10 → 各工程にタスク8 を、count 件になるまで繰り返す。 */
    static List<List<String>> manyRows(int count, LocalDate today) {
        Rows r = new Rows(today, MANY_FIRST_ID);
        int added = 0;
        for (int p = 1; added < count; p++) {
            int project = r.add(null, "負荷確認 " + p, PEOPLE[p % PEOPLE.length], -30, 90,
                    TaskStatus.IN_PROGRESS, Priority.MEDIUM, 0, "");
            added++;
            for (int s = 0; s < PHASES_PER_PROJECT && added < count; s++) {
                int phase = r.add(project, "工程 " + p + "-" + (s + 1), "チーム一律", -30 + s * 12, -18 + s * 12,
                        TaskStatus.NOT_STARTED, Priority.MEDIUM, 0, "");
                added++;
                for (int t = 0; t < TASKS_PER_PHASE && added < count; t++) {
                    int from = -30 + s * 12 + t;
                    r.add(phase, "作業 " + p + "-" + (s + 1) + "-" + (t + 1), PEOPLE[(p + s + t) % PEOPLE.length],
                            from, from + 4, from + 4 < 0 ? TaskStatus.COMPLETED : TaskStatus.NOT_STARTED,
                            Priority.values()[t % Priority.values().length], from + 4 < 0 ? 100 : 0, "");
                    added++;
                }
            }
        }
        return r.rows;
    }

    /** CSV の行を、今日から何日目かで日付を指定して足していく。階層は親から決める。 */
    private static final class Rows {
        private final List<List<String>> rows = new ArrayList<>();
        private final Map<Integer, Integer> levels = new HashMap<>();
        private final LocalDate today;
        private int nextId;

        private Rows(LocalDate today, int firstId) {
            this.today = today;
            this.nextId = firstId;
            rows.add(TaskCsv.HEADER);
        }

        private int add(Integer parent, String name, String assignee, Integer from, Integer to,
                        TaskStatus status, Priority priority, int progress, String memo) {
            int id = nextId++;
            int level = parent == null ? 1 : levels.get(parent) + 1;
            levels.put(id, level);
            raw(String.valueOf(id), parent == null ? "" : String.valueOf(parent), String.valueOf(level), name, assignee,
                    from == null ? "" : day(from), to == null ? "" : day(to),
                    status.getLabel(), priority.getLabel(), String.valueOf(progress), memo);
            return id;
        }

        private void raw(String... fields) {
            rows.add(List.of(fields));
        }

        private String day(int offset) {
            return today.plusDays(offset).toString();
        }
    }
}
