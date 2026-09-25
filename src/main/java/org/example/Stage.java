// packageとは、このファイルがどのグループ（パッケージ）に属しているかを宣言するもの
package org.example;

// importとは、他のファイルやライブラリから機能を借りてくること
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中項目（Stage）
 * Project に属する中間階層で、Task を複数持つことができる。
 */
// public class Stageとは、Stageというクラスを宣言していること
public class Stage {
    // private finalとは、このクラス内でだけ使えて変更できない値を宣言すること
    private final int id;
    private final String name;
    // List<Task>とは、Taskオブジェクトのリストを扱う型
    private final List<Task> tasks;

    // public Stageとは、コンストラクタ（クラスを作る時に実行される初期化処理）のこと
    public Stage(int id, String name) {
        // this(...)とは、同じクラスの別のコンストラクタを呼び出すこと
        this(id, name, new ArrayList<>());
    }

    // public Stageとは、コンストラクタ（クラスを作る時に実行される初期化処理）のこと
    public Stage(int id, String name, List<Task> tasks) {
        this.id = id;
        this.name = name;
        // tasksがnullでなければ新しいリストを作成、そうでなければ空のリストを作成
        this.tasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
    }

    // public intとは、整数を返すメソッド
    public int getId() {
        return id;
    }

    // public Stringとは、文字列を返すメソッド
    public String getName() {
        return name;
    }

    // public List<Task>とは、Taskのリストを返すメソッド
    public List<Task> getTasks() {
        // unmodifiableListとは、変更できないリスト（読み取り専用）を作ること
        return Collections.unmodifiableList(tasks);
    }

    // public voidとは、戻り値がないメソッド
    public void addTask(Task task) {
        // ifとは、もし〜だったらという条件分岐
        if (task == null) {
            return;
        }
        // addとは、リストに要素を追加すること
        tasks.add(task);
    }

    // public intとは、整数を返すメソッド
    public int calculateChildrenProgressAverage() {
        // isEmptyとは、リストが空かどうかを確認すること
        if (tasks.isEmpty()) {
            return 0;
        }

        int total = 0;
        // for-eachとは、リストの要素を1つずつ取り出して繰り返すこと
        for (Task task : tasks) {
            total += task.getProgress();
        }
        return Math.round((float) total / tasks.size());
    }

    // public Stringとは、文字列を返すメソッド
    public String getStatusSummary() {
        if (tasks.isEmpty()) {
            return TaskStatus.NOT_STARTED.name();
        }

        int average = calculateChildrenProgressAverage();
        if (average >= 100) {
            return TaskStatus.COMPLETED.name();
        }
        if (average > 0) {
            return TaskStatus.IN_PROGRESS.name();
        }
        return TaskStatus.NOT_STARTED.name();
    }
}
