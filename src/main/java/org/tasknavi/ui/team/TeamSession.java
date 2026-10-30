package org.tasknavi.ui.team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasknavi.AppMessages;
import org.tasknavi.event.TaskEventBus;
import org.tasknavi.event.TaskEventListener;
import org.tasknavi.model.Assignees;
import org.tasknavi.team.GoogleDriveFolders;
import org.tasknavi.team.TeamConflictGroup;
import org.tasknavi.team.TeamField;
import org.tasknavi.team.TeamNotice;
import org.tasknavi.team.TeamProject;
import org.tasknavi.team.TeamSettings;
import org.tasknavi.team.TeamState;
import org.tasknavi.team.TeamTaskStore;
import org.tasknavi.ui.AppDialog;
import org.tasknavi.ui.ErrorDialogUtil;
import org.tasknavi.ui.NoticeBar;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 開いている共有プロジェクト。作る・参加する・閉じる、自動の読み直し、重なった変更と履歴の表示を受け持つ。
 * タスクの表示と編集は各タブが {@code TaskService} 経由で行うので、ここでは扱わない。
 *
 * <p>フォルダの読み書きは {@link TeamBackground} の 1 本の列に並べ、ネットワークドライブが遅くても画面を止めない。
 * 開いていない最近のプロジェクトは {@link TeamOtherProjects} が持つ。</p>
 */
public final class TeamSession {

    private static final Logger logger = LoggerFactory.getLogger(TeamSession.class);
    /** 共有フォルダを見に行く間隔。ファイルの一覧と更新時刻を見るだけなので、短くしても重くならない。 */
    public static final int POLL_MS = 5_000;

    /** 開いているプロジェクトや、その中身・同期の様子が変わったことをほかの画面に伝える。 */
    public interface Listener {
        /** @param opened 閉じたら null */
        void projectChanged(TeamProject opened);

        /** 読み直しや書き込みで、開いているプロジェクトのタスクが変わった。 */
        void dataChanged();

        /** 最終確認の時刻・読み込み中・重なりの件数など、表示だけが変わった。 */
        void statusChanged();

        /** 開いていない共有プロジェクト（「今日」と期限の通知に使う）を読み込んだ、または中身が変わった。 */
        void otherProjectsChanged();

        /** ほかの人が自分を担当にした・自分の担当のタスクを変えた。キーはプロジェクト名（届いた順）。 */
        void noticesArrived(Map<String, List<TeamNotice>> byProject);
    }

    private final TeamSettings settings;
    private final Component owner;
    private final TeamBackground background = new TeamBackground(new TeamBackground.Hooks() {
        @Override
        public void started() {
            fireStatusChanged();
        }

        @Override
        public void succeeded() {
            lastChecked = Instant.now();
            lastPollFailed = false;
        }

        @Override
        public void finished() {
            if (!notifyIfChanged()) {
                fireStatusChanged();
            }
        }
    });
    private final Timer pollTimer = new Timer(POLL_MS, e -> poll());
    private final TaskEventListener busListener = event -> fireStatusChanged();
    private Listener listener;
    private TeamProject project;
    private final TeamOtherProjects others = new TeamOtherProjects();
    /** 画面の番号を保つため、プロジェクトごとに 1 つだけ作る。 */
    private final Map<TeamProject, TeamTaskStore> stores = new IdentityHashMap<>();
    private TeamState lastNotifiedState;
    private boolean polling;
    private Instant lastChecked;
    private boolean lastPollFailed;

    /** @param owner ダイアログやお知らせを出す画面 */
    public TeamSession(TeamSettings settings, Component owner) {
        this.settings = settings;
        this.owner = owner;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * 自動の読み直しを始め、前回開いていたプロジェクトとほかの最近のプロジェクトを裏側で開く。
     *
     * @param onReady 開き終わったら（失敗しても）画面のスレッドで呼ぶ
     */
    public void start(Runnable onReady) {
        TaskEventBus.getInstance().register(busListener);
        pollTimer.start();
        if (!settings.displayName().isEmpty()) {
            settings.lastProject().ifPresent(dir -> open(dir, true));
            loadOthers();
        }
        background.execute(() -> SwingUtilities.invokeLater(onReady));
    }

    public void stop() {
        pollTimer.stop();
        TaskEventBus.getInstance().unregister(busListener);
        background.shutdown();
    }

    // ---------------------------------------------------------------- 状態

    /** 開いていなければ null。 */
    public TeamProject project() {
        return project;
    }

    public List<Path> recentProjects() {
        return settings.recentProjects();
    }

    public String displayName() {
        return settings.displayName();
    }

    public boolean busy() {
        return background.busy();
    }

    /** まだ一度も確認していなければ null。 */
    public Instant lastChecked() {
        return lastChecked;
    }

    public boolean lastCheckFailed() {
        return lastPollFailed;
    }

    public int unacknowledgedConflictCount() {
        return project != null ? unacknowledgedConflicts(project).size() : 0;
    }

    /** 最近開いた共有プロジェクトのうち、今開いていなくて読めたもの（最近開いた順）。 */
    public List<TeamProject> otherProjects() {
        return others.list();
    }

    public TeamTaskStore storeOf(TeamProject opened) {
        return stores.computeIfAbsent(opened, TeamTaskStore::new);
    }

    // ---------------------------------------------------------------- プロジェクト

    public void createProject() {
        if (!ensureDisplayName()) {
            return;
        }
        TeamDialogs.CreateRequest request = TeamDialogs.askCreate(owner, defaultLocation());
        if (request == null) {
            return;
        }
        String deviceId = settings.deviceId();
        String author = settings.displayName();
        background.run(() -> TeamProject.create(request.folder(), request.name(), deviceId, author), created -> {
            setProject(created);
            settings.remember(created.directory());
            NoticeBar.info(owner, AppMessages.format("team.notice.created",
                    "「{0}」を作りました。このフォルダをメンバーに共有すると、「共有フォルダから参加する」で参加できます。",
                    created.name()));
        }, e -> ErrorDialogUtil.showError(owner, AppMessages.format(
                "team.create.failed", "作れませんでした: {0}", e.getMessage())));
    }

    public void chooseAndOpen() {
        if (!ensureDisplayName()) {
            return;
        }
        Path dir = TeamDialogs.askProjectFolder(owner, defaultLocation());
        if (dir != null) {
            open(dir, false);
        }
    }

    public void open(Path dir) {
        open(dir, false);
    }

    /** @param quiet 起動時の自動オープン。失敗してもダイアログを出さない（ドライブがまだ準備中のことがある）。 */
    private void open(Path dir, boolean quiet) {
        if (!quiet && !ensureDisplayName()) {
            return;
        }
        String deviceId = settings.deviceId();
        String author = settings.displayName();
        TeamProject loaded = others.get(dir);
        Callable<TeamProject> work = loaded != null
                ? () -> {
                    loaded.reload();
                    return loaded;
                }
                : () -> TeamProject.open(dir, deviceId, author);
        background.run(work, opened -> {
            TeamProject previous = project;
            others.remove(dir);
            setProject(opened);
            others.keep(previous, settings.recentProjects());
            settings.remember(opened.directory());
        }, e -> {
            if (quiet) {
                return;
            }
            boolean recent = settings.recentProjects().contains(TeamOtherProjects.key(dir));
            if (recent && AppDialog.confirm(owner, AppMessages.get("project.title", "プロジェクト"),
                    AppMessages.format("team.open.forget",
                            "「{0}」を開けませんでした。\n{1}\n最近開いたプロジェクトから外しますか？",
                            dir.getFileName(), e.getMessage()))) {
                settings.forget(dir);
                if (others.remove(dir)) {
                    fireOthersChanged();
                }
            } else if (!recent) {
                ErrorDialogUtil.showError(owner, AppMessages.format(
                        "team.open.failed", "開けませんでした: {0}", e.getMessage()));
            }
        });
    }

    /** 共有プロジェクトを閉じ、自分だけのタスクに戻る。閉じたものも「今日」と期限の通知には出し続ける。 */
    public void close() {
        if (project == null) {
            return;
        }
        TeamProject previous = project;
        setProject(null);
        others.keep(previous, settings.recentProjects());
        settings.clearLastProject();
    }

    /** 最近開いたプロジェクトを読むだけのために開く。前回開いていたものは別に開くので除く。 */
    private void loadOthers() {
        String deviceId = settings.deviceId();
        String author = settings.displayName();
        Path last = settings.lastProject().map(TeamOtherProjects::key).orElse(null);
        List<Path> dirs = settings.recentProjects().stream().filter(dir -> !dir.equals(last)).toList();
        background.execute(() -> {
            List<TeamProject> loaded = TeamOtherProjects.openAll(dirs, deviceId, author);
            SwingUtilities.invokeLater(() -> {
                if (others.addLoaded(loaded, project)) {
                    fireOthersChanged();
                }
            });
        });
    }

    public void reload() {
        TeamProject opened = project;
        if (opened == null) {
            return;
        }
        background.run(() -> {
            opened.reload();
            return opened;
        }, reloaded -> {
        }, e -> ErrorDialogUtil.showError(owner, AppMessages.format(
                "team.reload.failed", "読み直せませんでした: {0}", e.getMessage())));
    }

    /** 開いている全部のプロジェクトで表示名を変える。前の名前が担当のタスクがあれば、担当者も直すか聞く。 */
    public void changeDisplayName() {
        String oldName = settings.displayName();
        String name = TeamDialogs.askDisplayName(owner, oldName);
        if (name == null) {
            return;
        }
        settings.setDisplayName(name);
        List<TeamProject> opened = new ArrayList<>();
        if (project != null) {
            opened.add(project);
        }
        opened.addAll(others.list());
        if (opened.isEmpty()) {
            fireStatusChanged();
            return;
        }
        boolean reassign = askReassign(oldName, name, opened);
        background.run(() -> {
            for (TeamProject each : opened) {
                each.rename(name);
                if (reassign) {
                    each.reassign(oldName, name);
                }
            }
            return opened;
        }, renamed -> fireOthersChanged(), e -> ErrorDialogUtil.showError(owner, e.getMessage()));
    }

    private boolean askReassign(String oldName, String newName, List<TeamProject> opened) {
        if (oldName.isBlank() || Assignees.same(oldName, newName)) {
            return false;
        }
        int count = opened.stream().mapToInt(each -> each.countAssignedTo(oldName)).sum();
        return count > 0 && AppDialog.confirm(owner, AppMessages.get("team.rename.title", "表示名の変更"),
                AppMessages.format("team.rename.reassign",
                        "「{0}」が担当のタスクが {1} 件あります。担当者も「{2}」に直しますか？", oldName, count, newName));
    }

    private void setProject(TeamProject opened) {
        project = opened;
        lastNotifiedState = opened != null ? opened.state() : null;
        lastChecked = opened != null ? Instant.now() : null;
        lastPollFailed = false;
        if (listener != null) {
            listener.projectChanged(opened);
        }
    }

    // ---------------------------------------------------------------- メンバー・重なり・履歴

    public void showMembers() {
        TeamProject opened = project;
        if (opened != null) {
            TeamMembersDialog.show(owner, opened.members(), opened.deviceId(), this::changeDisplayName);
        }
    }

    /** 新しい版のアプリを使っている人がいるときの、入れ替え方。 */
    public void showUpdateHelp() {
        TeamProject opened = project;
        if (opened != null) {
            TeamUpdateHelp.show(owner, opened);
        }
    }

    public void showConflicts() {
        TeamProject opened = project;
        if (opened == null) {
            return;
        }
        List<TeamConflictGroup> groups = unacknowledgedConflicts(opened);
        Map<String, TeamState.Version> versions = opened.state().versions();
        TeamConflictDialog.Choice choice = TeamConflictDialog.show(owner, groups,
                id -> versions.containsKey(id) ? versions.get(id).name() : null,
                keys -> {
                    settings.acknowledge(opened.directory(), keys);
                    fireStatusChanged();
                });
        if (choice == null) {
            return;
        }
        TeamConflictGroup group = choice.group();
        if (choice.kind() == TeamConflictDialog.Choice.Kind.RESTORE) {
            background.run(() -> opened.restoreTask(group.taskId()), restored -> {
                settings.acknowledge(opened.directory(), group.keys());
                NoticeBar.info(owner, AppMessages.format("team.notice.restored", "{0} 件のタスクを戻しました。", restored));
            }, this::showSaveError);
            return;
        }
        Map<TeamField, String> lost = new EnumMap<>(TeamField.class);
        group.items().forEach(item -> lost.put(item.field(), item.lostValue()));
        background.run(() -> {
            opened.updateTask(group.taskId(), lost);
            return group.taskId();
        }, id -> {
            settings.acknowledge(opened.directory(), group.keys());
            NoticeBar.info(owner, AppMessages.get("notice.saved", "保存しました"));
        }, this::showSaveError);
    }

    public void showHistory() {
        TeamProject opened = project;
        if (opened == null) {
            return;
        }
        background.run(opened::history, history -> SwingUtilities.invokeLater(
                        () -> TeamHistoryDialog.show(owner, history, null, "")),
                e -> ErrorDialogUtil.showError(owner, e.getMessage()));
    }

    private List<TeamConflictGroup> unacknowledgedConflicts(TeamProject opened) {
        return TeamConflictGroup.unacknowledged(opened.state().conflicts(),
                settings.acknowledgedConflicts(opened.directory()));
    }

    // ---------------------------------------------------------------- 読み直し

    /** ファイルが変わっていたときだけ読み直す。開いていないプロジェクトも「今日」のために見る。 */
    private void poll() {
        TeamProject opened = project;
        List<TeamProject> otherList = others.list();
        if ((opened == null && otherList.isEmpty()) || busy() || polling) {
            return;
        }
        polling = true;
        background.execute(() -> {
            TeamPoll result = TeamPoll.check(opened, otherList);
            SwingUtilities.invokeLater(() -> {
                polling = false;
                if (result.othersChanged()) {
                    fireOthersChanged();
                }
                if (!result.notices().isEmpty() && listener != null) {
                    listener.noticesArrived(result.notices());
                }
                if (opened == null || project != opened) {
                    return;
                }
                lastChecked = Instant.now();
                logPollResult(opened, result.failure());
                lastPollFailed = result.failure() != null;
                if (!notifyIfChanged()) {
                    fireStatusChanged();
                }
            });
        });
    }

    /** 5 秒ごとに同じ失敗を書かないよう、読めなくなったときと戻ったときだけ残す。 */
    private void logPollResult(TeamProject opened, Exception failure) {
        if (failure != null && !lastPollFailed) {
            logger.warn("共有フォルダを読めなくなりました: {}", opened.directory(), failure);
        } else if (failure == null && lastPollFailed) {
            logger.info("共有フォルダをまた読めるようになりました: {}", opened.directory());
        }
    }

    private void fireOthersChanged() {
        if (listener != null) {
            listener.otherProjectsChanged();
        }
    }

    /** @return 変わっていて知らせたら true */
    private boolean notifyIfChanged() {
        TeamProject opened = project;
        if (opened == null || opened.state() == lastNotifiedState) {
            return false;
        }
        lastNotifiedState = opened.state();
        if (listener != null) {
            listener.dataChanged();
        }
        return true;
    }

    private void fireStatusChanged() {
        if (listener != null) {
            listener.statusChanged();
        }
    }

    private void showSaveError(Exception e) {
        ErrorDialogUtil.showError(owner, AppMessages.format("team.save.failed", "保存できませんでした: {0}", e.getMessage()));
    }

    private boolean ensureDisplayName() {
        if (!settings.displayName().isEmpty()) {
            return true;
        }
        String name = TeamDialogs.askDisplayName(owner, "");
        if (name == null) {
            return false;
        }
        settings.setDisplayName(name);
        return true;
    }

    private Path defaultLocation() {
        if (project != null && project.directory().getParent() != null) {
            return project.directory().getParent();
        }
        for (Path recent : settings.recentProjects()) {
            if (recent.getParent() != null) {
                return recent.getParent();
            }
        }
        return GoogleDriveFolders.preferredLocation().orElse(Path.of(System.getProperty("user.home")));
    }
}
