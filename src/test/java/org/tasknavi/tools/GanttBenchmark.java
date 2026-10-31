package org.tasknavi.tools;

import org.tasknavi.model.Task;
import org.tasknavi.persistence.TaskStore;
import org.tasknavi.service.TaskService;
import org.tasknavi.ui.gantt.GanttPanel;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ガントのタブを、タスクが何千件もあるときに描き直すのに何ミリ秒かかるかを測る。
 * 画面は出さず、ガントのタブと同じ組み立て（名前の列・日付の見出し・バーの欄）を画面1枚分の画像に描く。
 * 数字は20回の真ん中の値。
 *
 * <p>動かし方は README の「開発メモ」。名前が Test で終わらないので、mvn test では動かない。</p>
 */
public final class GanttBenchmark {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int WARMUP = 30;
    private static final int RUNS = 20;
    private static final int PHASES_PER_PROJECT = 10;

    private GanttBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        int[] sizes = args.length > 0
                ? Arrays.stream(args).mapToInt(Integer::parseInt).toArray()
                : new int[]{500, 1000, 3000, 5000, 10000};
        System.out.println("件数\t全体\tバーの欄\t名前の列\t日付の見出し\tマウス移動（ミリ秒）");
        for (int size : sizes) {
            SwingUtilities.invokeAndWait(() -> measure(size));
        }
    }

    private static void measure(int size) {
        List<Task> tasks = tasks(size, LocalDate.now());
        GanttPanel panel = new GanttPanel(new TaskService(new FixedStore(tasks)));
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setColumnHeaderView(panel.createDateHeader());
        scroll.setRowHeaderView(panel.createTaskNameColumn());
        scroll.setSize(WIDTH, HEIGHT);
        scroll.doLayout();
        scroll.getViewport().doLayout();
        scroll.getRowHeader().doLayout();
        scroll.getColumnHeader().doLayout();

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        double all = median(() -> paint(scroll, image));
        double bars = median(() -> paint(scroll.getViewport(), image));
        double names = median(() -> paint(scroll.getRowHeader(), image));
        double header = median(() -> paint(scroll.getColumnHeader(), image));
        int[] y = {0};
        double move = median(() -> {
            y[0] = (y[0] + 37) % HEIGHT;
            panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
                    200, y[0], 0, false));
        });
        System.out.printf("%d\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f%n", tasks.size(), all, bars, names, header, move);
    }

    private static void paint(Component component, BufferedImage image) {
        Graphics2D g = image.createGraphics();
        try {
            g.setClip(0, 0, component.getWidth(), component.getHeight());
            component.paint(g);
        } finally {
            g.dispose();
        }
    }

    private static double median(Runnable action) {
        for (int i = 0; i < WARMUP; i++) {
            action.run();
        }
        long[] nanos = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            action.run();
            nanos[i] = System.nanoTime() - start;
        }
        Arrays.sort(nanos);
        return nanos[RUNS / 2] / 1_000_000.0;
    }

    /** プロジェクト → 10 の段階 → タスク の3階層で、だいたい {@code size} 件。期間は今日の前後半年に散らす。 */
    static List<Task> tasks(int size, LocalDate today) {
        int projects = Math.max(1, size / 300);
        int perPhase = Math.max(1, (size - projects * (1 + PHASES_PER_PROJECT)) / (projects * PHASES_PER_PROJECT));
        List<Task> tasks = new ArrayList<>();
        int id = 1;
        for (int p = 0; p < projects; p++) {
            int projectId = id++;
            tasks.add(new Task(projectId, "プロジェクト" + p, null, 1, p, 0, Task.STATUS_IN_PROGRESS, null, null));
            for (int s = 0; s < PHASES_PER_PROJECT; s++) {
                int phaseId = id++;
                tasks.add(new Task(phaseId, "段階" + s, projectId, 2, s, 0, Task.STATUS_IN_PROGRESS, null, null));
                for (int t = 0; t < perPhase; t++) {
                    int n = id++;
                    LocalDate start = today.plusDays((n * 7L) % 360 - 180);
                    String status = n % 3 == 0 ? Task.STATUS_COMPLETED : n % 3 == 1 ? Task.STATUS_IN_PROGRESS : Task.STATUS_NOT_STARTED;
                    tasks.add(new Task(n, "タスク" + n, phaseId, 3, t, (n * 13) % 101, status, start, start.plusDays(n % 20)));
                }
            }
        }
        return tasks;
    }

    private record FixedStore(List<Task> tasks) implements TaskStore {
        @Override
        public List<Task> getAllTasks() {
            return tasks;
        }

        @Override
        public Task getTaskById(int taskId) {
            return tasks.stream().filter(task -> task.getId() == taskId).findFirst().orElse(null);
        }

        @Override
        public int addTask(Task task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateTasks(List<Task> updated) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateTaskOrderIndex(int taskId, int newOrderIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateSiblingOrder(List<Task> orderedTasks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Task> deleteTask(int taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void restoreTasks(List<Task> deletedTasks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void importTasks(List<Task> imported, boolean replaceExisting) {
            throw new UnsupportedOperationException();
        }
    }
}
