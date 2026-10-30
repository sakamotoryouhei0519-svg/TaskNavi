package org.tasknavi.tools;

import org.tasknavi.model.Task;
import org.tasknavi.persistence.Database;
import org.tasknavi.persistence.SampleDataUtil;
import org.tasknavi.service.TaskService;
import org.tasknavi.team.TeamField;
import org.tasknavi.team.TeamProject;
import org.tasknavi.team.TeamSettings;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.main.MainFrame;
import org.tasknavi.ui.taskdialog.TaskDialog;
import org.tasknavi.ui.team.TeamSession;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

/**
 * README の画面の写真を作る。見本のデータだけを一時フォルダに作って本物のメイン画面を開き、各タブを PNG にする。
 * いつものデータ・共有プロジェクト・テーマの設定には触れない（終わったら元に戻す）。
 *
 * <p>動かし方は README の「開発メモ」。名前が Test で終わらないので、mvn test では動かない。</p>
 */
public final class ScreenshotTool {

    private static final String PROFILE = "screenshot";
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 780;
    private static final double SCALE = 1.5;
    private static final int TODAY = 0;
    private static final int WBS = 1;
    private static final int KANBAN = 2;
    private static final int GANTT = 3;
    private static final int CALENDAR = 4;
    private static final String COMMENTED_TASK = "画面の案";

    private ScreenshotTool() {
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "docs/images");
        Files.createDirectories(out);
        Path work = Files.createTempDirectory("tasknavi-screenshot");
        System.setProperty("tasknavi.dataDir", work.resolve("data").toString());
        System.setProperty("tasknavi.team.profile", PROFILE);
        boolean originalDark = Preferences.userNodeForPackage(AppTheme.class).getBoolean("tasknavi.theme.dark", false);
        try {
            run(out, work);
        } finally {
            AppTheme.setDarkMode(originalDark);
            Preferences.userNodeForPackage(TeamSettings.class).node("profiles").node(PROFILE).removeNode();
            try (Stream<Path> files = Files.walk(work)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
        System.exit(0);
    }

    private static void run(Path out, Path work) throws Exception {
        Database.prepareOnStartup();
        SampleDataUtil.insertSampleTasksIfEmpty();
        TeamSettings.userDefault().setDisplayName("佐藤花子");
        Path teamDir = createTeamProject(work.resolve("team").resolve("新サービス立ち上げ"));

        AtomicReference<MainFrame> frameRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            AppTheme.setDarkMode(false);
            AppTheme.applySwingDefaults();
            MainFrame frame = new MainFrame(TaskService.createDefault());
            frame.setSize(WIDTH, HEIGHT);
            frame.setVisible(true);
            frameRef.set(frame);
        });
        MainFrame frame = frameRef.get();
        try {
            selectTab(frame, TODAY);
            capture(frame.getContentPane(), out.resolve("today.png"));
            selectTab(frame, CALENDAR);
            capture(frame.getContentPane(), out.resolve("calendar.png"));

            TeamSession session = field(frame, "teamSession", TeamSession.class);
            SwingUtilities.invokeAndWait(() -> session.open(teamDir));
            waitUntil(() -> session.project() != null && !session.busy());
            selectTab(frame, WBS);
            selectTreeRow(frame, COMMENTED_TASK);
            capture(frame.getContentPane(), out.resolve("wbs.png"));
            selectTab(frame, KANBAN);
            capture(frame.getContentPane(), out.resolve("kanban.png"));
            selectTab(frame, GANTT);
            capture(frame.getContentPane(), out.resolve("gantt.png"));
            captureTaskDialog(frame, COMMENTED_TASK, out.resolve("task-dialog.png"));

            SwingUtilities.invokeAndWait(() -> field(frame, "themeToggleButton", JToggleButton.class).doClick());
            selectTab(frame, GANTT);
            capture(frame.getContentPane(), out.resolve("gantt-dark.png"));
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    /** 3 人で使っている共有プロジェクト。2 人が同じ項目を同時に変えた「重なり」とコメントも入れる。 */
    private static Path createTeamProject(Path dir) throws Exception {
        TeamProject tanaka = TeamProject.create(dir, "新サービス立ち上げ", "pc-tanaka", "田中次郎");
        TeamProject suzuki = TeamProject.open(dir, "pc-suzuki", "鈴木一郎");
        String project = tanaka.createTask(Map.of(TeamField.NAME, "新サービス立ち上げ", TeamField.ASSIGNEE, "田中次郎",
                TeamField.START, "2026-10-01", TeamField.END, "2026-12-20", TeamField.STATUS, Task.STATUS_IN_PROGRESS,
                TeamField.PROGRESS, "35"));
        String plan = tanaka.createTask(Map.of(TeamField.NAME, "企画", TeamField.PARENT, project,
                TeamField.START, "2026-10-01", TeamField.END, "2026-10-31", TeamField.STATUS, Task.STATUS_IN_PROGRESS,
                TeamField.PROGRESS, "70"));
        tanaka.createTask(Map.of(TeamField.NAME, "利用者への聞き取り", TeamField.PARENT, plan, TeamField.ASSIGNEE, "佐藤花子",
                TeamField.START, "2026-10-01", TeamField.END, "2026-10-14", TeamField.STATUS, Task.STATUS_COMPLETED,
                TeamField.PROGRESS, "100"));
        String spec = tanaka.createTask(Map.of(TeamField.NAME, "画面の案", TeamField.PARENT, plan, TeamField.ASSIGNEE, "鈴木一郎",
                TeamField.START, "2026-10-15", TeamField.END, "2026-10-30", TeamField.STATUS, Task.STATUS_IN_PROGRESS,
                TeamField.PROGRESS, "60", TeamField.PRIORITY, "HIGH"));
        tanaka.createTask(Map.of(TeamField.NAME, "見積もり", TeamField.PARENT, plan, TeamField.ASSIGNEE, "田中次郎",
                TeamField.START, "2026-10-20", TeamField.END, "2026-11-04", TeamField.STATUS, Task.STATUS_NOT_STARTED));
        String build = tanaka.createTask(Map.of(TeamField.NAME, "開発", TeamField.PARENT, project,
                TeamField.START, "2026-11-01", TeamField.END, "2026-12-20"));
        tanaka.createTask(Map.of(TeamField.NAME, "申し込み画面", TeamField.PARENT, build, TeamField.ASSIGNEE, "佐藤花子",
                TeamField.START, "2026-11-01", TeamField.END, "2026-11-20", TeamField.PRIORITY, "HIGH"));
        tanaka.createTask(Map.of(TeamField.NAME, "管理画面", TeamField.PARENT, build, TeamField.ASSIGNEE, "鈴木一郎",
                TeamField.START, "2026-11-10", TeamField.END, "2026-12-05"));
        tanaka.createTask(Map.of(TeamField.NAME, "受け入れテスト", TeamField.PARENT, build, TeamField.ASSIGNEE, "田中次郎",
                TeamField.START, "2026-12-01", TeamField.END, "2026-12-20", TeamField.PRIORITY, "LOW"));

        suzuki.reload();
        tanaka.addComment(spec, "スマホの画面も入れてください");
        suzuki.addComment(spec, "了解です。金曜までに足します");
        tanaka.updateTask(spec, Map.of(TeamField.END, "2026-11-06"));
        suzuki.updateTask(spec, Map.of(TeamField.END, "2026-11-04"));
        return dir;
    }

    private static void selectTab(MainFrame frame, int index) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            findTabs(frame.getContentPane()).setSelectedIndex(index);
            frame.validate();
        });
        Thread.sleep(800);
    }

    private static void selectTreeRow(MainFrame frame, String name) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTree tree = find(frame.getContentPane(), JTree.class);
            for (int row = 0; row < tree.getRowCount(); row++) {
                if (String.valueOf(tree.getPathForRow(row).getLastPathComponent()).contains(name)) {
                    tree.setSelectionRow(row);
                    return;
                }
            }
            throw new IllegalStateException("WBS に見つかりません: " + name);
        });
        Thread.sleep(800);
    }

    private static void captureTaskDialog(MainFrame frame, String name, Path file) throws Exception {
        AtomicReference<TaskDialog> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            TaskService service = field(frame, "taskService", TaskService.class);
            Task task = service.getAllTasks().stream()
                    .filter(t -> t.getName().equals(name))
                    .findFirst().orElseThrow();
            TaskDialog dialog = new TaskDialog(frame, service, task);
            dialog.setModal(false);
            dialog.setVisible(true);
            ref.set(dialog);
        });
        Thread.sleep(800);
        try {
            capture(ref.get().getContentPane(), file);
        } finally {
            SwingUtilities.invokeAndWait(ref.get()::dispose);
        }
    }

    private static void capture(Container container, Path file) throws Exception {
        AtomicReference<BufferedImage> image = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            int w = container.getWidth();
            int h = container.getHeight();
            BufferedImage img = new BufferedImage((int) (w * SCALE), (int) (h * SCALE), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.scale(SCALE, SCALE);
                ((JComponent) container).printAll(g);
            } finally {
                g.dispose();
            }
            image.set(img);
        });
        ImageIO.write(image.get(), "png", file.toFile());
        System.out.println("wrote " + file);
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws Exception {
        for (int i = 0; i < 100; i++) {
            AtomicReference<Boolean> done = new AtomicReference<>(false);
            SwingUtilities.invokeAndWait(() -> done.set(condition.getAsBoolean()));
            if (done.get()) {
                Thread.sleep(800);
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("共有プロジェクトを開けませんでした");
    }

    private static JTabbedPane findTabs(Container container) {
        return find(container, JTabbedPane.class);
    }

    /** 見えている部品のうち、最初に見つかった type。 */
    private static <T> T find(Container container, Class<T> type) {
        for (Component child : container.getComponents()) {
            if (!child.isShowing()) {
                continue;
            }
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container nested) {
                T found = find(nested, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T> T field(Object owner, String name, Class<T> type) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(owner));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
