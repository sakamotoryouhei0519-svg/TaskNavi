package org.tasknavi.persistence;

import org.tasknavi.model.Task;

import java.util.List;

/**
 * タスクの保存先。個人のタスクは PC の中のデータベース（{@link TaskDao}）、
 * 共有プロジェクトは共有フォルダ（{@code team.TeamTaskStore}）。画面は TaskService を通してどちらも同じように扱う。
 */
public interface TaskStore {

    List<Task> getAllTasks();

    Task getTaskById(int taskId);

    /** @return 新しいタスクの ID */
    int addTask(Task task);

    /** 検証に失敗したら 1 件も書かない。 */
    void updateTasks(List<Task> tasks);

    void updateTaskOrderIndex(int taskId, int newOrderIndex);

    void updateSiblingOrder(List<Task> orderedTasks);

    /** @return 消したタスク（子を含む）。{@link #restoreTasks} に渡せば元に戻せる */
    List<Task> deleteTask(int taskId);

    void restoreTasks(List<Task> deletedTasks);

    /** ID・親 ID は {@code tasks} の中だけで通じる番号として扱い、保存時に振り直す。 */
    void importTasks(List<Task> tasks, boolean replaceExisting);

    /** 担当者の欄で選べる名前。空なら選ぶボタンを出さない。 */
    default List<String> assigneeOptions() {
        return List.of();
    }
}
