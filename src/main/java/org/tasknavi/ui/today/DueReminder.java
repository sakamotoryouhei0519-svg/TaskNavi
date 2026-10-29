package org.tasknavi.ui.today;

import org.tasknavi.AppMessages;
import org.tasknavi.service.DueTasks;
import org.tasknavi.service.TaskService;
import org.tasknavi.service.TaskSource;
import org.tasknavi.team.TeamNotice;
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
import java.util.Map;
import java.util.function.Supplier;

/**
 * 起動時と日付が変わったときに、期限切れ・今日まで・明日までの件数を Windows の通知で知らせる。
 * 自分だけのタスクと、最近開いた共有プロジェクトのタスクをまとめて数える。共有プロジェクトは「今日」タブの
 * 「自分の担当だけ」に合わせる。
 * 日付が変わったら全画面を描き直させ、期限切れの表示を新しい日付に合わせる。
 * ほかの人が自分を担当にしたときや、自分の担当のタスクを変えたときも、同じアイコンから知らせる。
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
        String message = buildMessage(dueAcross(sources.get(), today, TodayPanel.onlyMine()), today);
        if (message != null && trayIcon != null) {
            trayIcon.displayMessage("TaskNavi", message, TrayIcon.MessageType.INFO);
        }
    }

    /** @param onlyMine 共有プロジェクトは自分の担当（と担当者が空のもの）だけ数える */
    static DueTasks dueAcross(List<TaskSource> sources, LocalDate today, boolean onlyMine) {
        List<DueTasks> parts = new ArrayList<>();
        for (TaskSource source : sources) {
            try {
                parts.add(DueTasks.of(source.service().getAllTasks(), today,
                        onlyMine ? source::isMine : task -> true));
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

    /** ほかの人が自分を担当にした・自分の担当のタスクを変えた・自分もコメントしたタスクにコメントしたことを知らせる。 */
    public void showNotices(Map<String, List<TeamNotice>> byProject) {
        String message = buildNoticeMessage(byProject);
        if (message != null && trayIcon != null) {
            trayIcon.displayMessage("TaskNavi", message, TrayIcon.MessageType.INFO);
        }
    }

    /** 1 件ならタスク名と人を、2 件以上なら件数とプロジェクト名を出す。知らせることがなければ null。 */
    static String buildNoticeMessage(Map<String, List<TeamNotice>> byProject) {
        int total = byProject.values().stream().mapToInt(List::size).sum();
        if (total == 0) {
            return null;
        }
        if (total > 1) {
            return AppMessages.format("notice.many", "あなたに関わるタスクに {0} 件の変更があります（{1}）",
                    total, String.join("、", byProject.keySet()));
        }
        Map.Entry<String, List<TeamNotice>> only = byProject.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty()).findFirst().orElseThrow();
        TeamNotice notice = only.getValue().get(0);
        return switch (notice.kind()) {
            case ASSIGNED -> AppMessages.format("notice.assigned", "{0}さんが「{1}」をあなたの担当にしました（{2}）",
                    notice.author(), notice.taskName(), only.getKey());
            case COMMENTED -> AppMessages.format("notice.commented", "{0}さんがあなたの担当の「{1}」にコメントしました（{2}）",
                    notice.author(), notice.taskName(), only.getKey());
            case REPLIED -> AppMessages.format("notice.replied", "{0}さんが、あなたもコメントした「{1}」にコメントしました（{2}）",
                    notice.author(), notice.taskName(), only.getKey());
            case CHANGED -> AppMessages.format("notice.changed", "{0}さんがあなたの担当の「{1}」を変えました（{2}）",
                    notice.author(), notice.taskName(), only.getKey());
        };
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
