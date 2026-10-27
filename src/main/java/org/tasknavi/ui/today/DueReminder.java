package org.tasknavi.ui.today;

import org.tasknavi.AppMessages;
import org.tasknavi.service.DueTasks;
import org.tasknavi.service.TaskService;
import org.tasknavi.service.TaskSource;
import org.tasknavi.ui.AppIcon;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 起動時と日付が変わったときに、期限切れ・今日まで・明日までの件数を Windows の通知で知らせる。
 * 自分だけのタスクと、最近開いた共有プロジェクトのタスクをまとめて数える。
 * 日付が変わったら全画面を描き直させ、期限切れの表示を新しい日付に合わせる。
 */
public final class DueReminder {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DueReminder.class);
    private static final int CHECK_INTERVAL_MS = 10 * 60 * 1000;

    private final TaskService taskService;
    private final Supplier<List<TaskSource>> sources;
    private final Runnable onOpen;
    private final Timer timer;
    private TrayIcon trayIcon;
    private LocalDate lastCheckedDay;

    /**
     * @param sources 数えるプロジェクト（自分だけのタスクと、最近開いた共有プロジェクト）
     * @param onOpen  通知やアイコンを押したときに呼ぶ（ウィンドウを前に出して今日のタブを開く）
     */
    public DueReminder(TaskService taskService, Supplier<List<TaskSource>> sources, Runnable onOpen) {
        this.taskService = taskService;
        this.sources = sources;
        this.onOpen = onOpen;
        this.timer = new Timer(CHECK_INTERVAL_MS, e -> check());
    }

    public void start() {
        installTrayIcon();
        check();
        timer.start();
    }

    public void stop() {
        timer.stop();
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }

    private void check() {
        LocalDate today = LocalDate.now();
        if (today.equals(lastCheckedDay)) {
            return;
        }
        boolean dayChanged = lastCheckedDay != null;
        lastCheckedDay = today;
        if (dayChanged) {
            taskService.reload();
        }
        String message = buildMessage(dueAcross(sources.get(), today), today);
        if (message != null && trayIcon != null) {
            trayIcon.displayMessage("TaskNavi", message, TrayIcon.MessageType.INFO);
        }
    }

    static DueTasks dueAcross(List<TaskSource> sources, LocalDate today) {
        List<DueTasks> parts = new ArrayList<>();
        for (TaskSource source : sources) {
            try {
                parts.add(DueTasks.of(source.service().getAllTasks(), today));
            } catch (RuntimeException e) {
                logger.warn("「{}」のタスクを数えられませんでした", source.name(), e);
            }
        }
        return DueTasks.merge(parts);
    }

    /** 知らせることがなければ null。 */
    static String buildMessage(DueTasks due, LocalDate today) {
        List<String> parts = new ArrayList<>();
        if (!due.overdue().isEmpty()) {
            parts.add(AppMessages.format("reminder.overdue", "期限切れ {0}件", due.overdue().size()));
        }
        if (!due.dueToday().isEmpty()) {
            parts.add(AppMessages.format("reminder.today", "今日まで {0}件", due.dueToday().size()));
        }
        long tomorrow = due.countDueOn(today.plusDays(1));
        if (tomorrow > 0) {
            parts.add(AppMessages.format("reminder.tomorrow", "明日まで {0}件", tomorrow));
        }
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private void installTrayIcon() {
        if (!SystemTray.isSupported()) {
            logger.info("この環境では通知領域が使えないため、期限の通知は出しません。");
            return;
        }
        TrayIcon icon = new TrayIcon(createImage(), "TaskNavi");
        icon.setImageAutoSize(true);
        icon.addActionListener(e -> SwingUtilities.invokeLater(onOpen));
        try {
            SystemTray.getSystemTray().add(icon);
            trayIcon = icon;
        } catch (AWTException e) {
            logger.warn("通知領域にアイコンを置けませんでした", e);
        }
    }

    private static Image createImage() {
        return AppIcon.render(32);
    }
}
