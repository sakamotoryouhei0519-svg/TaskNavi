package org.tasknavi.team;

import org.tasknavi.AppVersion;
import org.tasknavi.model.Assignees;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 1 つのチームプロジェクトフォルダを、1 台の PC から操作する入口。
 *
 * <pre>
 * プロジェクトフォルダ\
 *   project.json          プロジェクト名など（{@link TeamProjectFile}）
 *   members\
 *     &lt;端末ID&gt;.json       その PC の表示名とアプリの版（その PC しか書かない。{@link TeamMembers}）
 *   changes\
 *     &lt;端末ID&gt;.jsonl      その PC が書いた変更の記録（その PC しか書かない。{@link TeamChangeLog}）
 * </pre>
 *
 * <p>書く前の確かめは {@link TeamTaskRules}、記録から今の状態を組み立てるのは {@link TeamReplay} が受け持つ。
 * ここは、記録を読み込んで持ち、新しい記録を作って書き足す。</p>
 *
 * <p>変更の base は「画面に出ている状態」から作る。書く直前に読み直すと、
 * 見ていない他の人の変更を黙って上書きしてしまい、重なりに気づけないため。</p>
 */
public final class TeamProject {
    public static final String PROJECT_FILE = "project.json";
    public static final String MEMBERS_DIR = "members";

    private final Path dir;
    private final String name;
    private final String deviceId;
    private volatile String author;
    private final Clock clock;
    /** この PC のアプリの版。開発版なら null。 */
    private final String appVersion;
    private final List<TeamEvent> events = new ArrayList<>();
    /** 書き込みは裏側のスレッド、読み取りは画面のスレッドから来る。どれも作り直して差し替えるだけ。 */
    private volatile TeamState state = TeamState.empty();
    private volatile List<TeamMember> members = List.of();
    private volatile int unreadableLines;
    private volatile int newerFormatLines;
    private volatile List<String> tooLargeFiles = List.of();
    private String fingerprint = "";
    /** 読み込んだことのある記録の ID。最初の読み込みまでは null（開いたときの分は知らせない）。 */
    private Set<String> knownEventIds;
    /** ほかの PC から届いて、まだ {@link #takeNotices()} していない変更。 */
    private final List<TeamEvent> arrived = new ArrayList<>();

    private TeamProject(Path dir, String name, String deviceId, String author, Clock clock, String appVersion) {
        if (!TeamChangeLog.isValidDeviceId(deviceId)) {
            throw new IllegalArgumentException("端末 ID に使えない文字があります: " + deviceId);
        }
        this.dir = dir;
        this.name = name;
        this.deviceId = deviceId;
        this.author = author == null || author.isBlank() ? deviceId : author.trim();
        this.clock = clock;
        this.appVersion = appVersion;
    }

    /** プロジェクト名から、Windows のフォルダ名に使えない文字を除いた名前を作る。 */
    public static String folderNameFor(String projectName) {
        String cleaned = projectName == null ? "" : projectName.trim()
                .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .replaceAll("[. ]+$", "");
        return cleaned.isEmpty() ? "TaskNavi-project" : cleaned;
    }

    public static boolean isProjectFolder(Path dir) {
        return Files.isRegularFile(dir.resolve(PROJECT_FILE));
    }

    public static TeamProject create(Path dir, String name, String deviceId, String author) throws IOException {
        return create(dir, name, deviceId, author, Clock.systemUTC());
    }

    static TeamProject create(Path dir, String name, String deviceId, String author, Clock clock) throws IOException {
        TeamProjectFile.create(dir, name, author, clock);
        return open(dir, deviceId, author, clock);
    }

    public static TeamProject open(Path dir, String deviceId, String author) throws IOException {
        return open(dir, deviceId, author, Clock.systemUTC());
    }

    static TeamProject open(Path dir, String deviceId, String author, Clock clock) throws IOException {
        return open(dir, deviceId, author, clock, AppVersion.release());
    }

    static TeamProject open(Path dir, String deviceId, String author, Clock clock, String appVersion)
            throws IOException {
        String projectName = TeamProjectFile.readName(dir);
        TeamProject project = new TeamProject(dir, projectName, deviceId, author, clock, appVersion);
        project.reload();
        project.registerSelf();
        return project;
    }

    /** 自分の表示名を変える。これから書く記録に使い、過去の記録の名前は変えない。 */
    public synchronized void rename(String newAuthor) throws IOException {
        if (newAuthor == null || newAuthor.isBlank()) {
            throw new IllegalArgumentException("表示名を入力してください");
        }
        author = newAuthor.trim();
        registerSelf();
    }

    private void registerSelf() throws IOException {
        if (TeamMembers.register(dir, members, deviceId, author, appVersion, clock)) {
            members = TeamMembers.read(dir);
        }
    }

    public String name() {
        return name;
    }

    public Path directory() {
        return dir;
    }

    public String deviceId() {
        return deviceId;
    }

    public String author() {
        return author;
    }

    public TeamState state() {
        return state;
    }

    /** 名前順。同じ人が 2 台で参加していれば 2 件になる。 */
    public List<TeamMember> members() {
        return members;
    }

    /** この PC のアプリの版。開発版なら null。 */
    public String appVersion() {
        return appVersion;
    }

    /**
     * この PC より新しい版のアプリを使っているメンバーのうち、いちばん新しい版の人。
     * いなければ、またはこの PC が開発版なら null。
     */
    public TeamMember newerAppMember() {
        return TeamMembers.newest(members, appVersion);
    }

    /** 直近の読み込みで読めなかった行の数（同期途中なら次の読み込みで直る）。 */
    public int unreadableLines() {
        return unreadableLines;
    }

    /** 直近の読み込みで、新しい版のアプリで書かれていて読めなかった行の数（アプリを新しくするまで直らない）。 */
    public int newerFormatLines() {
        return newerFormatLines;
    }

    /** 直近の読み込みで、大きすぎて読まなかった changes\ のファイルの名前（壊れたか細工されたファイル）。 */
    public List<String> tooLargeFiles() {
        return tooLargeFiles;
    }

    /** 新しい順の変更履歴。 */
    public List<TeamHistory.Entry> history() {
        List<TeamEvent> copy;
        synchronized (this) {
            copy = List.copyOf(events);
        }
        return TeamHistory.build(copy);
    }

    /** フォルダの記録を全部読み直して組み立てる。 */
    public synchronized TeamState reload() throws IOException {
        fingerprint = TeamChangeLog.fingerprint(dir);
        TeamChangeLog.ReadResult result = TeamChangeLog.readAll(dir);
        Set<String> ids = new HashSet<>();
        for (TeamEvent event : result.events()) {
            if (ids.add(event.eventId()) && knownEventIds != null && !knownEventIds.contains(event.eventId())
                    && !deviceId.equals(event.deviceId())) {
                arrived.add(event);
            }
        }
        knownEventIds = ids;
        events.clear();
        events.addAll(result.events());
        unreadableLines = result.unreadableLines();
        newerFormatLines = result.newerFormatLines();
        tooLargeFiles = result.tooLargeFiles();
        state = TeamReplay.replay(events);
        members = TeamMembers.read(dir);
        return state;
    }

    /**
     * フォルダのファイルが前回の読み込みから変わっていれば読み直す。
     *
     * @return 読み直したら true
     */
    public synchronized boolean reloadIfChanged() throws IOException {
        if (TeamChangeLog.fingerprint(dir).equals(fingerprint)) {
            return false;
        }
        reload();
        return true;
    }

    /**
     * 前回呼んでから、ほかの人が自分（表示名）を担当にした・自分の担当のタスクを変えた・
     * この PC からコメントしたことのあるタスクにコメントしたもの。開いたときにすでにあった記録は含めない。
     */
    public synchronized List<TeamNotice> takeNotices() {
        List<TeamNotice> notices = TeamNotice.forMe(arrived, state, author, deviceId);
        arrived.clear();
        return notices;
    }

    // ---------------------------------------------------------------- タスク

    /**
     * タスクを作る。LEVEL は親から決め、ORDER を省くと兄弟の末尾に置く。
     *
     * @return 新しいタスクの ID
     */
    public synchronized String createTask(Map<TeamField, String> values) throws IOException {
        Map<TeamField, String> fields = new EnumMap<>(TeamField.class);
        fields.putAll(values);
        TeamTaskRules.requireName(fields.get(TeamField.NAME));
        String parentId = fields.get(TeamField.PARENT);
        if (parentId == null || parentId.isBlank()) {
            fields.put(TeamField.PARENT, null);
        }
        fields.put(TeamField.LEVEL, String.valueOf(TeamTaskRules.levelUnder(state, parentId)));
        TeamTaskRules.requireDateOrder(fields.get(TeamField.START), fields.get(TeamField.END));
        if (!fields.containsKey(TeamField.ORDER)) {
            long siblings = state.tasks().stream()
                    .filter(task -> Objects.equals(task.parentId(), fields.get(TeamField.PARENT)))
                    .count();
            fields.put(TeamField.ORDER, String.valueOf(siblings));
        }
        String taskId = UUID.randomUUID().toString();
        write(taskId, TeamEvent.Op.CREATE, fields, Map.of(), null);
        return taskId;
    }

    /**
     * 取り込む 1 件。
     *
     * @param parentIndex 親が同じ取り込みの中にあればその位置（自分より前）。無ければ -1
     */
    public record ImportDraft(Map<TeamField, String> values, int parentIndex) {
    }

    /**
     * まとめて取り込む。全件を確かめてから 1 回で書くので、途中で失敗しても何も書かれない。
     *
     * @param replaceExisting 今あるタスクを全部消してから取り込む
     * @return 作ったタスクの ID（drafts と同じ順）
     */
    public synchronized List<String> importTasks(List<ImportDraft> drafts, boolean replaceExisting) throws IOException {
        TeamImport.Batch batch = TeamImport.build(state, drafts, replaceExisting, deviceId, author, Instant.now(clock));
        writeAll(batch.events());
        return batch.taskIds();
    }

    /** 担当者が name のタスクの数。 */
    public int countAssignedTo(String name) {
        return (int) state.tasks().stream().filter(task -> Assignees.same(task.assignee(), name)).count();
    }

    /**
     * 担当者が from のタスクを、まとめて to に付け替える（表示名を変えたとき）。全部を 1 回で書く。
     *
     * @return 付け替えた数
     */
    public synchronized int reassign(String from, String to) throws IOException {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("担当者の名前を入力してください");
        }
        long clockValue = state.maxClock();
        Instant now = Instant.now(clock);
        List<TeamEvent> batch = new ArrayList<>();
        for (TeamTask task : state.tasks()) {
            if (!Assignees.same(task.assignee(), from)) {
                continue;
            }
            TeamState.Version version = state.versions().get(task.id());
            Map<TeamField, String> base = new EnumMap<>(TeamField.class);
            String seenEventId = version.fieldEventId().get(TeamField.ASSIGNEE);
            if (seenEventId != null) {
                base.put(TeamField.ASSIGNEE, seenEventId);
            }
            batch.add(new TeamEvent(UUID.randomUUID().toString(), task.id(), TeamEvent.Op.UPDATE, ++clockValue,
                    deviceId, author, now, Map.of(TeamField.ASSIGNEE, to.trim()), base, version.lastEventId()));
        }
        if (batch.isEmpty()) {
            return 0;
        }
        writeAll(batch);
        return batch.size();
    }

    /** 今の状態を見て変えた、として記録する。 */
    public void updateTask(String taskId, Map<TeamField, String> changes) throws IOException {
        updateTask(taskId, changes, state);
    }

    /**
     * 画面で見ていた状態 seen から変えた項目だけを記録する。何も変わらなければ何も書かない。
     * 編集の途中で読み直しが入っても、見ていなかった他の人の変更は「重なり」として検出される。
     */
    public synchronized void updateTask(String taskId, Map<TeamField, String> requested, TeamState seen)
            throws IOException {
        requireLive(taskId);
        Map<TeamField, String> changes = new EnumMap<>(TeamField.class);
        changes.putAll(requested);
        changes.remove(TeamField.LEVEL);
        changes.remove(TeamField.COMMENT);
        if (changes.containsKey(TeamField.PARENT)) {
            String newParent = changes.get(TeamField.PARENT);
            newParent = newParent == null || newParent.isBlank() ? null : newParent;
            changes.put(TeamField.PARENT, newParent);
            TeamTaskRules.requireMovable(state, taskId, newParent);
        }
        TeamTask seenTask = seen.find(taskId).orElse(state.find(taskId).orElseThrow());
        TeamState.Version seenVersion = seen.versions().getOrDefault(taskId, state.versions().get(taskId));
        TeamTask current = state.find(taskId).orElseThrow();
        if (changes.containsKey(TeamField.NAME)) {
            TeamTaskRules.requireName(changes.get(TeamField.NAME));
        }
        TeamTaskRules.requireDateOrder(valueAfter(changes, current, TeamField.START),
                valueAfter(changes, current, TeamField.END));
        Map<TeamField, String> values = new EnumMap<>(TeamField.class);
        Map<TeamField, String> base = new EnumMap<>(TeamField.class);
        changes.forEach((field, value) -> {
            if (!Objects.equals(value, TeamTaskRules.currentValue(seenTask, field))) {
                values.put(field, value);
                String seenEventId = seenVersion.fieldEventId().get(field);
                if (seenEventId != null) {
                    base.put(field, seenEventId);
                }
            }
        });
        if (values.isEmpty()) {
            return;
        }
        write(taskId, TeamEvent.Op.UPDATE, values, base, seenVersion.lastEventId());
    }

    private static String valueAfter(Map<TeamField, String> changes, TeamTask current, TeamField field) {
        return changes.containsKey(field) ? changes.get(field) : TeamTaskRules.currentValue(current, field);
    }

    /** 子孫も含めて削除する（子から先に記録）。 */
    public void deleteTask(String taskId) throws IOException {
        deleteTask(taskId, state);
    }

    /** 見ていなかった子や変更があれば、削除と重なったことが記録に残る。 */
    public synchronized void deleteTask(String taskId, TeamState seen) throws IOException {
        requireLive(taskId);
        List<String> order = TeamTaskRules.subtree(state, taskId);
        for (int i = order.size() - 1; i >= 0; i--) {
            String id = order.get(i);
            TeamState.Version seenVersion = seen.versions().get(id);
            write(id, TeamEvent.Op.DELETE, Map.of(), Map.of(), seenVersion != null ? seenVersion.lastEventId() : null);
        }
    }

    /**
     * 削除したタスクを戻す。消えている先祖（表示に必要）と、消えている子孫もまとめて戻す。
     *
     * @return 戻したタスクの数
     */
    public synchronized int restoreTask(String taskId) throws IOException {
        if (!state.versions().containsKey(taskId)) {
            throw new IllegalArgumentException("タスクが見つかりません");
        }
        int restored = 0;
        for (String id : TeamTaskRules.restoreTargets(state.versions(), taskId)) {
            TeamState.Version version = state.versions().get(id);
            if (version != null && version.deleted()) {
                write(id, TeamEvent.Op.RESTORE, Map.of(), Map.of(), version.lastEventId());
                restored++;
            }
        }
        return restored;
    }

    // ---------------------------------------------------------------- コメント

    /** タスクにコメントを書き足す。タスクの値は変えないので、誰と同時に書いても重ならない。 */
    public synchronized void addComment(String taskId, String text) throws IOException {
        requireLive(taskId);
        write(taskId, TeamEvent.Op.COMMENT, Map.of(TeamField.COMMENT, TeamTaskRules.requireCommentText(text)),
                Map.of(), null);
    }

    /** この PC で書いた、まだ削除していないコメントか（編集・削除ができるのはこれだけ）。 */
    public boolean canChange(TeamComment comment) {
        return comment != null && !comment.deleted() && deviceId.equals(comment.deviceId());
    }

    /** 自分のコメントを編集する。元の文は変更履歴に残る。 */
    public synchronized void editComment(String taskId, String commentId, String text) throws IOException {
        TeamComment comment = requireOwnComment(taskId, commentId);
        String body = TeamTaskRules.requireCommentText(text);
        if (body.equals(comment.text())) {
            return;
        }
        write(taskId, TeamEvent.Op.COMMENT_EDIT, Map.of(TeamField.COMMENT, body), Map.of(), commentId);
    }

    /** 自分のコメントを削除する。一覧には「削除されました」と残り、元の文は変更履歴に残る。 */
    public synchronized void deleteComment(String taskId, String commentId) throws IOException {
        requireOwnComment(taskId, commentId);
        write(taskId, TeamEvent.Op.COMMENT_DELETE, Map.of(), Map.of(), commentId);
    }

    private TeamComment requireOwnComment(String taskId, String commentId) {
        requireLive(taskId);
        TeamComment comment = state.comments(taskId).stream()
                .filter(each -> each.eventId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("コメントが見つかりません"));
        if (!canChange(comment)) {
            throw new IllegalArgumentException(comment.deleted()
                    ? "このコメントはもう削除されています"
                    : "編集・削除できるのは、この PC で書いた自分のコメントだけです");
        }
        return comment;
    }

    // ---------------------------------------------------------------- 書き込み

    private void requireLive(String taskId) {
        TeamState.Version version = state.versions().get(taskId);
        if (version == null || version.deleted() || state.find(taskId).isEmpty()) {
            throw new IllegalArgumentException("タスクが見つかりません（削除された可能性があります）");
        }
    }

    private void write(String taskId, TeamEvent.Op op, Map<TeamField, String> values,
                       Map<TeamField, String> base, String baseEventId) throws IOException {
        TeamEvent event = new TeamEvent(UUID.randomUUID().toString(), taskId, op, state.maxClock() + 1,
                deviceId, author, Instant.now(clock), values, base, baseEventId);
        TeamChangeLog.append(dir, deviceId, event);
        events.add(event);
        state = TeamReplay.replay(events);
    }

    private void writeAll(List<TeamEvent> batch) throws IOException {
        TeamChangeLog.appendAll(dir, deviceId, batch);
        events.addAll(batch);
        state = TeamReplay.replay(events);
    }
}
