package org.tasknavi.ui.team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasknavi.AppMessages;
import org.tasknavi.AppVersion;
import org.tasknavi.event.TaskEventBus;
import org.tasknavi.event.TaskEventListener;
import org.tasknavi.model.Assignees;
import org.tasknavi.team.GoogleDriveFolders;
import org.tasknavi.team.TeamConflictGroup;
import org.tasknavi.team.TeamField;
import org.tasknavi.team.TeamMember;
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
import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 開いている共有プロジェクト。作る・参加する・閉じる、自動の読み直し、重なった変更と履歴の表示を受け持つ。
 * タスクの表示と編集は各タブが {@code TaskService} 経由で行うので、ここでは扱わない。
 *
 * <p>フォルダの読み書きは裏側の 1 本の列に並べ、ネットワークドライブが遅くても画面を止めない。</p>
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
    /** 読み書きを 1 本の列に並べる。書き込みの途中に読み直しが割り込まない。 */
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "team-io");
        thread.setDaemon(true);
        return thread;
    });
    private final Timer pollTimer = new Timer(POLL_MS, e -> poll());
    private final TaskEventListener busListener = event -> fireStatusChanged();
    private Listener listener;
    private TeamProject project;
    /** 最近開いた共有プロジェクトのうち、今開いていないもの。読むだけで、画面の切り替えには使わない。 */
    private final Map<Path, TeamProject> others = new LinkedHashMap<>();
    /** 画面の番号を保つため、プロジェクトごとに 1 つだけ作る。 */
    private final Map<TeamProject, TeamTaskStore> stores = new IdentityHashMap<>();
    private TeamState lastNotifiedState;
    private int pending;
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
        io.execute(() -> SwingUtilities.invokeLater(onReady));
    }

    public void stop() {
        pollTimer.stop();
        TaskEventBus.getInstance().unregister(busListener);
        io.shutdownNow();
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
        return pending > 0;
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

    /** 最近開いた共有プロジェクトのうち、今開いていて読めたもの（最近開いた順）。 */
    public List<TeamProject> otherProjects() {
        return List.copyOf(others.values());
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
        runInBackground(() -> TeamProject.create(request.folder(), request.name(), deviceId, author), created -> {
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
        Path key = key(dir);
        TeamProject loaded = others.get(key);
        Callable<TeamProject> work = loaded != null
                ? () -> {
                    loaded.reload();
                    return loaded;
                }
                : () -> TeamProject.open(dir, deviceId, author);
        runInBackground(work, opened -> {
            TeamProject previous = project;
            others.remove(key);
            setProject(opened);
            keepAsOther(previous);
            settings.remember(opened.directory());
        }, e -> {
            if (quiet) {
                return;
            }
            boolean recent = settings.recentProjects().contains(key);
            if (recent && AppDialog.confirm(owner, AppMessages.get("project.title", "プロジェクト"),
                    AppMessages.format("team.open.forget",
                            "「{0}」を開けませんでした。\n{1}\n最近開いたプロジェクトから外しますか？",
                            dir.getFileName(), e.getMessage()))) {
                settings.forget(dir);
                if (others.remove(key) != null) {
                    fireOthersChanged();
                }
            } else if (!recent) {
                ErrorDialogUtil.showError(owner, AppMessages.format(
                        "team.open.failed", "開けませんでした: {0}", e.getMessage()));
            }
        });
    }

    /** 共有プロジェクトを閉じ、自分だけのタスクに戻る。 */
    public void close() {
        if (project == null) {
            return;
        }
        TeamProject previous = project;
        setProject(null);
        keepAsOther(previous);
        settings.clearLastProject();
    }

    /** 閉じた・切り替えたプロジェクトも、「今日」と期限の通知には出し続ける。 */
    private void keepAsOther(TeamProject previous) {
        if (previous == null) {
            return;
        }
        Path key = key(previous.directory());
        if (settings.recentProjects().contains(key)) {
            others.put(key, previous);
        }
    }

    /** 最近開いたプロジェクトを読むだけのために開く。前回開いていたものは別に開くので除く。 */
    private void loadOthers() {
        String deviceId = settings.deviceId();
        String author = settings.displayName();
        Path last = settings.lastProject().map(TeamSession::key).orElse(null);
        List<Path> dirs = settings.recentProjects().stream().filter(dir -> !dir.equals(last)).toList();
        io.execute(() -> {
            List<TeamProject> loaded = new ArrayList<>();
            for (Path dir : dirs) {
                try {
                    loaded.add(TeamProject.open(dir, deviceId, author));
                } catch (Exception e) {
                    logger.debug("最近のプロジェクトを開けませんでした: {}", dir, e);
                }
            }
            SwingUtilities.invokeLater(() -> {
                boolean added = false;
                for (TeamProject opened : loaded) {
                    Path key = key(opened.directory());
                    boolean isCurrent = project != null && key(project.directory()).equals(key);
                    if (!isCurrent && !others.containsKey(key)) {
                        others.put(key, opened);
                        added = true;
                    }
                }
                if (added) {
                    fireOthersChanged();
                }
            });
        });
    }

    private static Path key(Path dir) {
        return dir.toAbsolutePath().normalize();
    }

    public void reload() {
        TeamProject opened = project;
        if (opened == null) {
            return;
        }
        runInBackground(() -> {
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
        opened.addAll(others.values());
        if (opened.isEmpty()) {
            fireStatusChanged();
            return;
        }
        boolean reassign = askReassign(oldName, name, opened);
        runInBackground(() -> {
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
        if (opened == null) {
            return;
        }
        TeamMember newer = opened.newerAppMember();
        String who = newer != null
                ? AppMessages.format("team.update.who", "{0} さんが TaskNavi {1} を使っています。この PC は {2} です。",
                        newer.name(), newer.appVersion(), AppVersion.current())
                : AppMessages.format("team.update.who.unknown",
                        "新しい版の TaskNavi で書かれた記録があります。この PC は {0} です。", AppVersion.current());
        String appPath = System.getProperty("jpackage.app-path");
        String folder = appPath != null && Path.of(appPath).getParent() != null
                ? Path.of(appPath).getParent().toString()
                : AppMessages.get("team.update.folder.default", "C:\\TaskNavi\\TaskNavi など");
        String steps = AppMessages.format("team.update.steps",
                "入れ替え方:\n1. TaskNavi を閉じる\n2. 新しい zip を受け取る（配った人から、またはリリースのページから）\n"
                        + "3. 今のアプリのフォルダ（{0}）を消して、同じ場所に zip を展開する\n4. TaskNavi.exe を起動する\n\n"
                        + "タスクのデータは別の場所にあるので消えません。", folder);
        int choice = AppDialog.choose(owner, AppMessages.get("team.update.title", "アプリの更新"), who + "\n\n" + steps,
                AppMessages.get("team.update.open", "リリースのページを開く"),
                AppMessages.get("team.conflict.close", "閉じる"));
        if (choice == 0) {
            try {
                Desktop.getDesktop().browse(URI.create(AppVersion.RELEASES_URL));
            } catch (Exception e) {
                ErrorDialogUtil.showError(owner, AppMessages.format("team.update.open.failed",
                        "ブラウザを開けませんでした。次のアドレスを開いてください: {0}", AppVersion.RELEASES_URL));
            }
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
            runInBackground(() -> opened.restoreTask(group.taskId()), restored -> {
                settings.acknowledge(opened.directory(), group.keys());
                NoticeBar.info(owner, AppMessages.format("team.notice.restored", "{0} 件のタスクを戻しました。", restored));
            }, this::showSaveError);
            return;
        }
        Map<TeamField, String> lost = new EnumMap<>(TeamField.class);
        group.items().forEach(item -> lost.put(item.field(), item.lostValue()));
        runInBackground(() -> {
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
        runInBackground(opened::history, history -> SwingUtilities.invokeLater(
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
        List<TeamProject> otherList = List.copyOf(others.values());
        if ((opened == null && otherList.isEmpty()) || busy() || polling) {
            return;
        }
        polling = true;
        io.execute(() -> {
            boolean failed = false;
            if (opened != null) {
                try {
                    opened.reloadIfChanged();
                } catch (Exception e) {
                    failed = true;
                    logger.debug("共有フォルダの確認に失敗しました", e);
                }
            }
            boolean othersChanged = false;
            for (TeamProject other : otherList) {
                try {
                    othersChanged |= other.reloadIfChanged();
                } catch (Exception e) {
                    logger.debug("共有フォルダの確認に失敗しました: {}", other.directory(), e);
                }
            }
            Map<String, List<TeamNotice>> notices = new LinkedHashMap<>();
            List<TeamProject> all = new ArrayList<>(otherList);
            if (opened != null) {
                all.add(0, opened);
            }
            for (TeamProject each : all) {
                List<TeamNotice> taken = each.takeNotices();
                if (!taken.isEmpty()) {
                    notices.put(each.name(), taken);
                }
            }
            boolean finalFailed = failed;
            boolean finalOthersChanged = othersChanged;
            SwingUtilities.invokeLater(() -> {
                polling = false;
                if (finalOthersChanged) {
                    fireOthersChanged();
                }
                if (!notices.isEmpty() && listener != null) {
                    listener.noticesArrived(notices);
                }
                if (opened == null || project != opened) {
                    return;
                }
                lastChecked = Instant.now();
                lastPollFailed = finalFailed;
                if (!notifyIfChanged()) {
                    fireStatusChanged();
                }
            });
        });
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

    private <T> void runInBackground(Callable<T> work, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        pending++;
        fireStatusChanged();
        io.execute(() -> {
            T result = null;
            Exception error = null;
            try {
                result = work.call();
            } catch (Exception e) {
                error = e;
            }
            T finalResult = result;
            Exception finalError = error;
            SwingUtilities.invokeLater(() -> {
                pending--;
                if (finalError == null) {
                    lastChecked = Instant.now();
                    lastPollFailed = false;
                    onSuccess.accept(finalResult);
                } else {
                    logger.warn("チームプロジェクトの読み書きに失敗しました", finalError);
                    onFailure.accept(finalError);
                }
                if (!notifyIfChanged()) {
                    fireStatusChanged();
                }
            });
        });
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
