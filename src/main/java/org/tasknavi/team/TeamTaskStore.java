package org.tasknavi.team;

import org.tasknavi.AppException;
import org.tasknavi.model.Task;
import org.tasknavi.persistence.TaskStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 共有プロジェクトを、個人用の画面がそのまま使える保存先として見せる。
 *
 * <p>画面は int の ID でタスクを扱うので、UUID に番号を振る。番号はアプリを閉じるまで変えない
 * （読み直すたびに振り直すと、選択中のタスクが別のタスクにすり替わるため）。</p>
 *
 * <p>渡したタスクには、そのとき見せた状態の版（{@link Task#getRevision()}）を付ける。保存のときはその版と比べ、
 * 画面を開いたあとに届いた他の人の変更を黙って上書きせず「重なり」として残す。</p>
 */
public final class TeamTaskStore implements TaskStore {

    /** 古い版を覚えておく数。編集画面を開いたまま何度読み直しが入っても、開いた時点と比べられるように。 */
    private static final int KEPT_STATES = 64;

    private final TeamProject project;
    private final Map<String, Integer> idByUuid = new HashMap<>();
    private final Map<Integer, String> uuidById = new HashMap<>();
    private int nextId = 1;
    private final Map<Long, TeamState> served = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, TeamState> eldest) {
            return size() > KEPT_STATES;
        }
    };
    private TeamState lastState;
    private long lastRevision;

    public TeamTaskStore(TeamProject project) {
        this.project = Objects.requireNonNull(project, "project");
    }

    public TeamProject project() {
        return project;
    }

    /** 画面で使っている番号から UUID を引く。無ければ null。 */
    public synchronized String uuidOf(int taskId) {
        return uuidById.get(taskId);
    }

    @Override
    public List<Task> getAllTasks() {
        TeamState state = project.state();
        long revision = revisionOf(state);
        List<Task> tasks = new ArrayList<>(state.tasks().size());
        for (TeamTask teamTask : state.tasks()) {
            tasks.add(toTask(teamTask, revision));
        }
        return tasks;
    }

    @Override
    public Task getTaskById(int taskId) {
        String uuid = uuidOf(taskId);
        if (uuid == null) {
            return null;
        }
        TeamState state = project.state();
        long revision = revisionOf(state);
        return state.find(uuid).map(teamTask -> toTask(teamTask, revision)).orElse(null);
    }

    @Override
    public int addTask(Task task) {
        Map<TeamField, String> values = editableValues(task, true);
        values.put(TeamField.PARENT, parentUuid(task));
        values.put(TeamField.ORDER, String.valueOf(task.getOrderIndex()));
        String uuid = call(() -> project.createTask(values));
        return idOf(uuid);
    }

    @Override
    public void updateTasks(List<Task> tasks) {
        if (tasks == null) {
            return;
        }
        for (Task task : tasks) {
            if (task.getName() == null || task.getName().isBlank()) {
                throw AppException.validation("タスク名が空です", "タスク名を入力してください。");
            }
        }
        for (Task task : tasks) {
            String uuid = requireUuid(task.getId());
            TeamState seen = seenState(task.getRevision());
            boolean hasChildren = project.state().tasks().stream().anyMatch(other -> uuid.equals(other.parentId()));
            Map<TeamField, String> values = editableValues(task, !hasChildren);
            values.put(TeamField.PARENT, parentUuid(task));
            values.put(TeamField.ORDER, String.valueOf(task.getOrderIndex()));
            run(() -> project.updateTask(uuid, values, seen));
        }
    }

    @Override
    public void updateTaskOrderIndex(int taskId, int newOrderIndex) {
        String uuid = requireUuid(taskId);
        run(() -> project.updateTask(uuid, Map.of(TeamField.ORDER, String.valueOf(newOrderIndex))));
    }

    @Override
    public void updateSiblingOrder(List<Task> orderedTasks) {
        if (orderedTasks == null) {
            return;
        }
        for (Task task : orderedTasks) {
            updateTaskOrderIndex(task.getId(), task.getOrderIndex());
        }
    }

    @Override
    public List<Task> deleteTask(int taskId) {
        String uuid = requireUuid(taskId);
        List<Task> all = getAllTasks();
        Set<Integer> removedIds = new HashSet<>();
        collectSubtree(taskId, all, removedIds);
        TeamState seen;
        synchronized (this) {
            seen = lastState != null ? lastState : project.state();
        }
        run(() -> project.deleteTask(uuid, seen));
        return all.stream().filter(task -> removedIds.contains(task.getId())).toList();
    }

    @Override
    public void restoreTasks(List<Task> deletedTasks) {
        if (deletedTasks == null) {
            return;
        }
        Set<Integer> ids = new HashSet<>();
        deletedTasks.forEach(task -> ids.add(task.getId()));
        for (Task task : deletedTasks) {
            if (task.getParentId() == null || !ids.contains(task.getParentId())) {
                String uuid = requireUuid(task.getId());
                run(() -> project.restoreTask(uuid));
            }
        }
    }

    @Override
    public void importTasks(List<Task> tasks, boolean replaceExisting) {
        if (tasks == null) {
            return;
        }
        List<Task> parentsFirst = new ArrayList<>(tasks);
        parentsFirst.removeIf(Objects::isNull);
        parentsFirst.sort(Comparator.comparingInt(Task::getLevel).thenComparingInt(Task::getOrderIndex));
        Map<Integer, Integer> indexByImportId = new HashMap<>();
        List<TeamProject.ImportDraft> drafts = new ArrayList<>(parentsFirst.size());
        for (Task task : parentsFirst) {
            Integer parentIndex = task.getParentId() != null ? indexByImportId.get(task.getParentId()) : null;
            indexByImportId.put(task.getId(), drafts.size());
            drafts.add(new TeamProject.ImportDraft(editableValues(task, true), parentIndex != null ? parentIndex : -1));
        }
        call(() -> project.importTasks(drafts, replaceExisting));
    }

    /** メンバーの名前のあとに、メンバー以外の担当者（社外の人など）を並べる。 */
    @Override
    public List<String> assigneeOptions() {
        Set<String> names = new LinkedHashSet<>();
        project.members().forEach(member -> names.add(member.name()));
        project.state().tasks().stream()
                .map(TeamTask::assignee)
                .filter(name -> !name.isBlank())
                .sorted()
                .forEach(names::add);
        return List.copyOf(names);
    }

    // ---------------------------------------------------------------- 変換

    private Task toTask(TeamTask teamTask, long revision) {
        Task task = new Task(idOf(teamTask.id()), teamTask.name(),
                teamTask.parentId() != null ? idOf(teamTask.parentId()) : null,
                teamTask.level(), teamTask.order(), teamTask.progress(), teamTask.status(),
                teamTask.startDate(), teamTask.endDate());
        task.setAssignee(teamTask.assignee());
        task.setPriority(teamTask.priority());
        task.setMemo(teamTask.memo());
        task.setRevision(revision);
        return task;
    }

    private static Map<TeamField, String> editableValues(Task task, boolean includeProgress) {
        Map<TeamField, String> values = new EnumMap<>(TeamField.class);
        values.put(TeamField.NAME, task.getName());
        values.put(TeamField.ASSIGNEE, Objects.requireNonNullElse(task.getAssignee(), ""));
        values.put(TeamField.START, task.getStartDate() != null ? task.getStartDate().toString() : null);
        values.put(TeamField.END, task.getEndDate() != null ? task.getEndDate().toString() : null);
        values.put(TeamField.PRIORITY, task.getPriorityCode());
        values.put(TeamField.MEMO, Objects.requireNonNullElse(task.getMemo(), ""));
        if (includeProgress) {
            values.put(TeamField.STATUS, task.getStatusCode());
            values.put(TeamField.PROGRESS, String.valueOf(task.getProgress()));
        }
        return values;
    }

    private String parentUuid(Task task) {
        Integer parentId = task.getParentId();
        if (parentId == null || parentId <= 0) {
            return null;
        }
        return requireUuid(parentId);
    }

    private synchronized int idOf(String uuid) {
        return idByUuid.computeIfAbsent(uuid, key -> {
            int id = nextId++;
            uuidById.put(id, key);
            return id;
        });
    }

    private String requireUuid(int taskId) {
        String uuid = uuidOf(taskId);
        if (uuid == null) {
            throw AppException.notFound("タスク");
        }
        return uuid;
    }

    private synchronized long revisionOf(TeamState state) {
        if (state != lastState) {
            lastState = state;
            lastRevision++;
            served.put(lastRevision, state);
        }
        return lastRevision;
    }

    /** 渡した版がもう残っていなければ（極端に古い）、今の状態と比べる。 */
    private synchronized TeamState seenState(long revision) {
        TeamState seen = served.get(revision);
        return seen != null ? seen : project.state();
    }

    private static void collectSubtree(int taskId, List<Task> all, Set<Integer> out) {
        out.add(taskId);
        for (Task task : all) {
            if (Objects.equals(task.getParentId(), taskId) && !out.contains(task.getId())) {
                collectSubtree(task.getId(), all, out);
            }
        }
    }

    // ---------------------------------------------------------------- 例外の変換

    private interface Write {
        void run() throws IOException;
    }

    private interface Read<T> {
        T call() throws IOException;
    }

    private static void run(Write write) {
        call(() -> {
            write.run();
            return null;
        });
    }

    /** 入力の誤りは画面にそのまま見せ、ファイルの読み書きの失敗は共有フォルダの問題として伝える。 */
    private static <T> T call(Read<T> read) {
        try {
            return read.call();
        } catch (IllegalArgumentException e) {
            throw AppException.validation(e.getMessage(), e.getMessage());
        } catch (IOException e) {
            throw AppException.businessRule("共有フォルダの読み書きに失敗しました: " + e.getMessage(),
                    "共有フォルダに保存できませんでした。Google ドライブがつながっているか確認してください。");
        }
    }
}
