package org.tasknavi.team;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.tasknavi.model.Assignees;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
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
 *   project.json          プロジェクト名など
 *   members\
 *     &lt;端末ID&gt;.json       その PC の表示名（その PC しか書かない）
 *   changes\
 *     &lt;端末ID&gt;.jsonl      その PC が書いた変更の記録（その PC しか書かない）
 * </pre>
 *
 * <p>変更の base は「画面に出ている状態」から作る。書く直前に読み直すと、
 * 見ていない他の人の変更を黙って上書きしてしまい、重なりに気づけないため。</p>
 */
public final class TeamProject {
    public static final String PROJECT_FILE = "project.json";
    public static final String MEMBERS_DIR = "members";
    public static final int MAX_LEVEL = 3;
    public static final int MAX_COMMENT_LENGTH = 2000;
    private static final int PROJECT_FORMAT = 1;

    private final Path dir;
    private final String name;
    private final String deviceId;
    private volatile String author;
    private final Clock clock;
    private final List<TeamEvent> events = new ArrayList<>();
    /** 書き込みは裏側のスレッド、読み取りは画面のスレッドから来る。どれも作り直して差し替えるだけ。 */
    private volatile TeamState state = TeamState.empty();
    private volatile List<TeamMember> members = List.of();
    private volatile int unreadableLines;
    private volatile int newerFormatLines;
    private String fingerprint = "";
    /** 読み込んだことのある記録の ID。最初の読み込みまでは null（開いたときの分は知らせない）。 */
    private Set<String> knownEventIds;
    /** ほかの PC から届いて、まだ {@link #takeNotices()} していない変更。 */
    private final List<TeamEvent> arrived = new ArrayList<>();

    private TeamProject(Path dir, String name, String deviceId, String author, Clock clock) {
        if (!TeamChangeLog.isValidDeviceId(deviceId)) {
            throw new IllegalArgumentException("端末 ID に使えない文字があります: " + deviceId);
        }
        this.dir = dir;
        this.name = name;
        this.deviceId = deviceId;
        this.author = author == null || author.isBlank() ? deviceId : author.trim();
        this.clock = clock;
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
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("プロジェクト名を入力してください");
        }
        Path file = dir.resolve(PROJECT_FILE);
        if (Files.exists(file)) {
            throw new IOException("このフォルダには既に共有プロジェクトがあります: " + dir);
        }
        Files.createDirectories(dir.resolve(TeamChangeLog.CHANGES_DIR));
        JsonObject json = new JsonObject();
        json.addProperty("format", PROJECT_FORMAT);
        json.addProperty("name", name.trim());
        json.addProperty("createdBy", author);
        json.addProperty("createdAt", Instant.now(clock).toString());
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
        return open(dir, deviceId, author, clock);
    }

    public static TeamProject open(Path dir, String deviceId, String author) throws IOException {
        return open(dir, deviceId, author, Clock.systemUTC());
    }

    static TeamProject open(Path dir, String deviceId, String author, Clock clock) throws IOException {
        Path file = dir.resolve(PROJECT_FILE);
        if (!Files.isRegularFile(file)) {
            throw new IOException("共有プロジェクトのフォルダではありません: " + dir);
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException(PROJECT_FILE + " を読めません: " + e.getMessage(), e);
        }
        int format = json.has("format") ? json.get("format").getAsInt() : PROJECT_FORMAT;
        if (format > PROJECT_FORMAT) {
            throw new IOException("新しい版の TaskNavi で作られたプロジェクトです。アプリを更新してください");
        }
        String projectName = json.has("name") ? json.get("name").getAsString() : dir.getFileName().toString();
        TeamProject project = new TeamProject(dir, projectName, deviceId, author, clock);
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

    /** members\&lt;自分の端末ID&gt;.json を、無いときか名前が変わったときだけ書く。 */
    private void registerSelf() throws IOException {
        TeamMember current = members.stream()
                .filter(member -> member.deviceId().equals(deviceId))
                .findFirst()
                .orElse(null);
        if (current != null && current.name().equals(author)) {
            return;
        }
        Instant joinedAt = current != null ? current.joinedAt() : Instant.now(clock);
        JsonObject json = new JsonObject();
        json.addProperty("name", author);
        json.addProperty("joinedAt", joinedAt.toString());
        Path file = dir.resolve(MEMBERS_DIR).resolve(deviceId + ".json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
        members = readMembers();
    }

    private List<TeamMember> readMembers() throws IOException {
        Path membersDir = dir.resolve(MEMBERS_DIR);
        List<TeamMember> result = new ArrayList<>();
        if (!Files.isDirectory(membersDir)) {
            return result;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(membersDir, "*.json")) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                String id = fileName.substring(0, fileName.length() - ".json".length());
                if (!TeamChangeLog.isValidDeviceId(id)) {
                    continue;
                }
                try {
                    JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                            .getAsJsonObject();
                    String memberName = json.get("name").getAsString();
                    Instant joinedAt = json.has("joinedAt") ? Instant.parse(json.get("joinedAt").getAsString()) : null;
                    result.add(new TeamMember(id, memberName, joinedAt));
                } catch (RuntimeException e) {
                    // 同期の途中。次の読み直しで読める。
                }
            }
        }
        result.sort(Comparator.comparing(TeamMember::name).thenComparing(TeamMember::deviceId));
        return List.copyOf(result);
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

    /** 直近の読み込みで読めなかった行の数（同期途中なら次の読み込みで直る）。 */
    public int unreadableLines() {
        return unreadableLines;
    }

    /** 直近の読み込みで、新しい版のアプリで書かれていて読めなかった行の数（アプリを新しくするまで直らない）。 */
    public int newerFormatLines() {
        return newerFormatLines;
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
        state = TeamReplay.replay(events);
        members = readMembers();
        return state;
    }

    /**
     * タスクを作る。LEVEL は親から決め、ORDER を省くと兄弟の末尾に置く。
     *
     * @return 新しいタスクの ID
     */
    public synchronized String createTask(Map<TeamField, String> values) throws IOException {
        Map<TeamField, String> fields = new EnumMap<>(TeamField.class);
        fields.putAll(values);
        String taskName = fields.get(TeamField.NAME);
        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("タスク名を入力してください");
        }
        String parentId = fields.get(TeamField.PARENT);
        int level = 1;
        if (parentId != null && !parentId.isBlank()) {
            TeamTask parent = state.find(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("親タスクが見つかりません"));
            level = parent.level() + 1;
            if (level > MAX_LEVEL) {
                throw new IllegalArgumentException("階層は " + MAX_LEVEL + " までです");
            }
        } else {
            fields.put(TeamField.PARENT, null);
        }
        fields.put(TeamField.LEVEL, String.valueOf(level));
        requireDateOrder(fields.get(TeamField.START), fields.get(TeamField.END));
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
        long clockValue = state.maxClock();
        Instant now = Instant.now(clock);
        List<TeamEvent> batch = new ArrayList<>();
        if (replaceExisting) {
            List<TeamTask> live = state.tasks();
            for (int i = live.size() - 1; i >= 0; i--) {
                String id = live.get(i).id();
                TeamState.Version version = state.versions().get(id);
                batch.add(new TeamEvent(UUID.randomUUID().toString(), id, TeamEvent.Op.DELETE, ++clockValue,
                        deviceId, author, now, Map.of(), Map.of(), version != null ? version.lastEventId() : null));
            }
        }
        List<String> ids = new ArrayList<>(drafts.size());
        List<Integer> levels = new ArrayList<>(drafts.size());
        Map<String, Integer> siblings = new HashMap<>();
        for (int i = 0; i < drafts.size(); i++) {
            ImportDraft draft = drafts.get(i);
            Map<TeamField, String> fields = new EnumMap<>(TeamField.class);
            fields.putAll(draft.values());
            String row = (i + 1) + " 件目: ";
            String taskName = fields.get(TeamField.NAME);
            if (taskName == null || taskName.isBlank()) {
                throw new IllegalArgumentException(row + "タスク名を入力してください");
            }
            int parentIndex = draft.parentIndex();
            if (parentIndex >= i) {
                throw new IllegalArgumentException(row + "親タスクは先に並べてください");
            }
            String parentId = parentIndex >= 0 ? ids.get(parentIndex) : null;
            int level = parentIndex >= 0 ? levels.get(parentIndex) + 1 : 1;
            if (level > MAX_LEVEL) {
                throw new IllegalArgumentException(row + "階層は " + MAX_LEVEL + " までです");
            }
            try {
                requireDateOrder(fields.get(TeamField.START), fields.get(TeamField.END));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(row + e.getMessage(), e);
            }
            fields.put(TeamField.PARENT, parentId);
            fields.put(TeamField.LEVEL, String.valueOf(level));
            String siblingKey = String.valueOf(parentId);
            int nextOrder = siblings.merge(siblingKey, 1, Integer::sum) - 1;
            if (!fields.containsKey(TeamField.ORDER)) {
                long existing = parentId == null && !replaceExisting
                        ? state.tasks().stream().filter(task -> task.parentId() == null).count() : 0;
                fields.put(TeamField.ORDER, String.valueOf(existing + nextOrder));
            }
            String taskId = UUID.randomUUID().toString();
            ids.add(taskId);
            levels.add(level);
            batch.add(new TeamEvent(UUID.randomUUID().toString(), taskId, TeamEvent.Op.CREATE, ++clockValue,
                    deviceId, author, now, fields, Map.of(), null));
        }
        TeamChangeLog.appendAll(dir, deviceId, batch);
        events.addAll(batch);
        state = TeamReplay.replay(events);
        return ids;
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
        TeamChangeLog.appendAll(dir, deviceId, batch);
        events.addAll(batch);
        state = TeamReplay.replay(events);
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
            requireMovable(taskId, newParent);
        }
        TeamTask seenTask = seen.find(taskId).orElse(state.find(taskId).orElseThrow());
        TeamState.Version seenVersion = seen.versions().getOrDefault(taskId, state.versions().get(taskId));
        TeamTask current = state.find(taskId).orElseThrow();
        if (changes.containsKey(TeamField.NAME) && (changes.get(TeamField.NAME) == null || changes.get(TeamField.NAME).isBlank())) {
            throw new IllegalArgumentException("タスク名を入力してください");
        }
        requireDateOrder(
                changes.containsKey(TeamField.START) ? changes.get(TeamField.START) : currentValue(current, TeamField.START),
                changes.containsKey(TeamField.END) ? changes.get(TeamField.END) : currentValue(current, TeamField.END));
        Map<TeamField, String> values = new EnumMap<>(TeamField.class);
        Map<TeamField, String> base = new EnumMap<>(TeamField.class);
        changes.forEach((field, value) -> {
            if (!Objects.equals(value, currentValue(seenTask, field))) {
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

    /** タスクにコメントを書き足す。タスクの値は変えないので、誰と同時に書いても重ならない。 */
    public synchronized void addComment(String taskId, String text) throws IOException {
        requireLive(taskId);
        write(taskId, TeamEvent.Op.COMMENT, Map.of(TeamField.COMMENT, requireCommentText(text)), Map.of(), null);
    }

    /** この PC で書いた、まだ削除していないコメントか（編集・削除ができるのはこれだけ）。 */
    public boolean canChange(TeamComment comment) {
        return comment != null && !comment.deleted() && deviceId.equals(comment.deviceId());
    }

    /** 自分のコメントを編集する。元の文は変更履歴に残る。 */
    public synchronized void editComment(String taskId, String commentId, String text) throws IOException {
        TeamComment comment = requireOwnComment(taskId, commentId);
        String body = requireCommentText(text);
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

    private static String requireCommentText(String text) {
        String body = text == null ? "" : text.strip();
        if (body.isEmpty()) {
            throw new IllegalArgumentException("コメントを入力してください");
        }
        if (body.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("コメントは " + MAX_COMMENT_LENGTH + " 文字までです");
        }
        return body;
    }

    /** 子孫も含めて削除する（子から先に記録）。 */
    public void deleteTask(String taskId) throws IOException {
        deleteTask(taskId, state);
    }

    /** 見ていなかった子や変更があれば、削除と重なったことが記録に残る。 */
    public synchronized void deleteTask(String taskId, TeamState seen) throws IOException {
        requireLive(taskId);
        List<String> order = new ArrayList<>();
        collectSubtree(taskId, order);
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
        Map<String, TeamState.Version> versions = state.versions();
        if (!versions.containsKey(taskId)) {
            throw new IllegalArgumentException("タスクが見つかりません");
        }
        List<String> targets = new ArrayList<>();
        List<String> ancestors = new ArrayList<>();
        String parentId = versions.get(taskId).parentId();
        for (int depth = 0; parentId != null && depth < 16; depth++) {
            TeamState.Version parent = versions.get(parentId);
            if (parent == null) {
                break;
            }
            if (parent.deleted()) {
                ancestors.add(0, parentId);
            }
            parentId = parent.parentId();
        }
        targets.addAll(ancestors);
        collectDeletedSubtree(taskId, versions, targets, 0);
        int restored = 0;
        for (String id : targets) {
            TeamState.Version version = state.versions().get(id);
            if (version != null && version.deleted()) {
                write(id, TeamEvent.Op.RESTORE, Map.of(), Map.of(), version.lastEventId());
                restored++;
            }
        }
        return restored;
    }

    private static void collectDeletedSubtree(String taskId, Map<String, TeamState.Version> versions,
                                              List<String> out, int depth) {
        out.add(taskId);
        if (depth > 16) {
            return;
        }
        versions.forEach((id, version) -> {
            if (taskId.equals(version.parentId()) && version.deleted()) {
                collectDeletedSubtree(id, versions, out, depth + 1);
            }
        });
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

    /** 自分の下や子孫の下には動かせない。動かした先で階層が {@link #MAX_LEVEL} を超えてもいけない。 */
    private void requireMovable(String taskId, String newParentId) {
        TeamTask task = state.find(taskId).orElseThrow();
        if (Objects.equals(task.parentId(), newParentId)) {
            return;
        }
        List<String> subtree = new ArrayList<>();
        collectSubtree(taskId, subtree);
        int parentLevel = 0;
        if (newParentId != null) {
            if (subtree.contains(newParentId)) {
                throw new IllegalArgumentException("自分の下には移動できません");
            }
            parentLevel = state.find(newParentId)
                    .orElseThrow(() -> new IllegalArgumentException("移動先の親タスクが見つかりません"))
                    .level();
        }
        int deepest = subtree.stream()
                .mapToInt(id -> state.find(id).map(TeamTask::level).orElse(task.level()))
                .max().orElse(task.level());
        if (parentLevel + 1 + (deepest - task.level()) > MAX_LEVEL) {
            throw new IllegalArgumentException("階層は " + MAX_LEVEL + " までです");
        }
    }

    private static void requireDateOrder(String start, String end) {
        if (start == null || end == null || start.isBlank() || end.isBlank()) {
            return;
        }
        try {
            if (LocalDate.parse(end.trim()).isBefore(LocalDate.parse(start.trim()))) {
                throw new IllegalArgumentException("終了日は開始日以降の日付にしてください");
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日付は YYYY-MM-DD 形式で入力してください");
        }
    }

    private void collectSubtree(String taskId, List<String> out) {
        out.add(taskId);
        for (TeamTask task : state.tasks()) {
            if (taskId.equals(task.parentId())) {
                collectSubtree(task.id(), out);
            }
        }
    }

    private TeamState.Version requireLive(String taskId) {
        TeamState.Version version = state.versions().get(taskId);
        if (version == null || version.deleted() || state.find(taskId).isEmpty()) {
            throw new IllegalArgumentException("タスクが見つかりません（削除された可能性があります）");
        }
        return version;
    }

    private void write(String taskId, TeamEvent.Op op, Map<TeamField, String> values,
                       Map<TeamField, String> base, String baseEventId) throws IOException {
        TeamEvent event = new TeamEvent(UUID.randomUUID().toString(), taskId, op, state.maxClock() + 1,
                deviceId, author, Instant.now(clock), values, base, baseEventId);
        TeamChangeLog.append(dir, deviceId, event);
        events.add(event);
        state = TeamReplay.replay(events);
    }

    static String currentValue(TeamTask task, TeamField field) {
        return switch (field) {
            case PARENT -> task.parentId();
            case LEVEL -> String.valueOf(task.level());
            case ORDER -> String.valueOf(task.order());
            case NAME -> task.name();
            case ASSIGNEE -> task.assignee();
            case START -> task.startDate() != null ? task.startDate().toString() : null;
            case END -> task.endDate() != null ? task.endDate().toString() : null;
            case STATUS -> task.status().name();
            case PRIORITY -> task.priority().name();
            case PROGRESS -> String.valueOf(task.progress());
            case MEMO -> task.memo();
            case COMMENT -> null;
        };
    }
}
