package org.example;

import java.time.LocalDate;

/**
 * 【タスクデータモデルクラス】
 * タスクの各属性（ID、名前、担当者、期間、進捗など）を保持するクラスです。
 *
 * 【重要単語の解説】
 * - モデルクラス: データの構造を定義するクラス（データを保持するだけ）
 * - フィールド: クラスが持つ変数（データ）
 * - プロパティ: フィールドの別名（外部からアクセスするデータ）
 * - LocalDate: Java 8以降の日付を扱うクラス（日付計算が容易）
 *
 * 【コードの読み方】
 * 1. フィールドでデータを定義
 * 2. コンストラクタで初期値を設定
 * 3. ゲッターで値を取得
 * 4. セッターで値を変更
 */
public class Task {
    public static final String DEFAULT_PRIORITY = "中";
    public static final String[] PRIORITY_OPTIONS = {"高", "中", "低"};
    public static final String STATUS_NOT_STARTED = TaskStatus.NOT_STARTED.getLabel();
    public static final String STATUS_IN_PROGRESS = TaskStatus.IN_PROGRESS.getLabel();
    public static final String STATUS_COMPLETED = TaskStatus.COMPLETED.getLabel();

    // ==========================================
    //  【フィールド（プロパティ）】
    // ==========================================
    // 【重要】private: 同じクラス内からのみアクセス可能（カプセル化）
    // 【重要】int: 整数型（小数点なし）
    // 【重要】Integer: intのオブジェクト版（nullを許容する）
    // 【重要】String: 文字列型
    // 【重要】LocalDate: 日付型

    private int id;              // 【重要】タスクID（データベースの主キー）
    private Integer parentId;    // 【重要】親タスクID（nullの場合はルートプロジェクト）
    private int level;           // 【重要】階層レベル（1=プロジェクト、2=タスク、3=サブタスク...）
    private String name;         // 【重要】タスク名
    private String assignee;     // 【重要】担当者名
    private int progress;       // 【重要】進捗率（0〜100）
    private String status;       // 【重要】状態（未着手・進行中・完了）
    private String priority;     // 【重要】優先度（高・中・低）
    private LocalDate startDate;  // 【重要】開始日
    private LocalDate endDate;    // 【重要】終了日

    // ==========================================
    //  【コンストラクタ】
    // ==========================================
    // 【重要】コンストラクタ: オブジェクト生成時に呼び出される初期化メソッド
    // 【重要】this: 現在のオブジェクト自身を指すキーワード
    // 【コードの読み方】
    // - 引数で受け取った値をフィールドに代入
    // - assigneeは初期値として空文字を設定
    public Task(int id, String name, Integer parentId, int level, int progress, String status, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.level = level;
        this.progress = progress;
        this.status = normalizeStatus(status);
        this.priority = DEFAULT_PRIORITY; // デフォルト優先度
        this.startDate = startDate;
        this.endDate = endDate;
        this.assignee = "";
    }

    public static String normalizeStatus(String status) {
        return TaskStatus.fromString(status).getLabel();
    }

    // ==========================================
    //  【ゲッター & セッター】
    // ==========================================
    // 【重要】ゲッター: フィールドの値を取得するメソッド（getで始まる）
    // 【重要】セッター: フィールドの値を設定するメソッド（setで始まる）
    // 【重要】カプセル化: フィールドをprivateにして、メソッド経由でアクセスする設計
    // 【コードの読み方】
    // - getId(): idの値を取得
    // - setId(int id): idの値を設定
    // - getTaskId(): idの別名（互換性のため）

    /**
     * 【ID を取得する】
     * データベース上の主キーを返します。
     * 画面上ではこの ID を使って「どのタスクを編集したか」を識別します。
     */
    public int getId() { return id; }

    /**
     * 【旧コード互換用の ID 取得】
     * 古い実装が getTaskId() を呼んでいても動くようにしています。
     * これは「後方互換」を維持するためのメソッドです。
     */
    public int getTaskId() { return id; } // 【重要】getTaskId() エラー対策（古いコードとの互換性）

    /**
     * 【ID を設定する】
     * データベースから自動採番された ID を Task オブジェクトに反映します。
     */
    public void setId(int id) { this.id = id; }

    /**
     * 【親タスクの ID を取得】
     * 親がいない場合は null を返します。
     * これにより「ルートタスクかどうか」を判定できます。
     */
    public Integer getParentId() { return parentId; }

    /**
     * 【親タスク ID を設定】
     * WBS の親子関係を構築するときに使います。
     */
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    /**
     * 【階層レベルを取得】
     * 1 が親、2 が子、3 が孫のように階層化されます。
     */
    public int getLevel() { return level; }

    /**
     * 【階層レベルを設定】
     * タスクの親子関係が変わったときに再計算されることがあります。
     */
    public void setLevel(int level) { this.level = level; }

    /**
     * 【タスク名を取得】
     * 画面に表示する名前そのものです。
     */
    public String getName() { return name; }

    /**
     * 【互換用の名前取得】
     * 古いコードが getTitle() を使っていても問題ないようにしています。
     */
    public String getTitle() { return name; } // 【重要】getTitle() エラー対策（古いコードとの互換性）

    /**
     * 【タスク名を設定】
     * 画面から入力された値をこのオブジェクトに保存します。
     */
    public void setName(String name) { this.name = name; }

    // 【重要】三項演算子: 条件 ? 真の値 : 偽の値
    // 【コードの読み方】
    // - assigneeがnullなら空文字、そうでなければassigneeを返す
    /**
     * 【担当者名を取得】
     * まだ担当者が決まっていない場合でも null にならないように空文字を返します。
     */
    public String getAssignee() { return assignee != null ? assignee : ""; }

    /**
     * 【担当者名を設定】
     * 画面やDBから担当者名を反映する際に使用します。
     */
    public void setAssignee(String assignee) { this.assignee = assignee; }

    /**
     * 【進捗率を取得】
     * 0〜100 の整数で、タスクの完了度を表します。
     */
    public int getProgress() { return progress; }

    /**
     * 【進捗率を設定】
     * 更新時に渡された値をそのまま保存します。
     */
    public void setProgress(int progress) {
       if (progress < 0) {
           this.progress = 0;
       } else if (progress > 100) {
           this.progress = 100;
       } else {
           this.progress = progress;
       }
    }

    /**
     * 【状態を取得】
     * 例: 「未着手」「進行中」「完了」などを保持します。
     */
    public String getStatus() { return status != null ? normalizeStatus(status) : STATUS_NOT_STARTED; }

    /**
     * 【状態を設定】
     * 進捗率連動や手動変更時に使います。
     */
    public void setStatus(String status) { this.status = normalizeStatus(status); }

    public boolean isNotStarted() { return TaskStatus.NOT_STARTED.getLabel().equals(getStatus()); }

    public boolean isInProgress() { return TaskStatus.IN_PROGRESS.getLabel().equals(getStatus()); }

    public boolean isCompleted() { return TaskStatus.COMPLETED.getLabel().equals(getStatus()); }

    /**
     * 【優先度を取得】
     * 優先度が未設定ならデフォルト値（中）を返します。
     */
    public String getPriority() { return priority != null ? priority : DEFAULT_PRIORITY; }

    /**
     * 【優先度を設定】
     * null を受け取った場合は自動でデフォルト値に置き換えます。
     */
    public void setPriority(String priority) { this.priority = priority != null ? priority : DEFAULT_PRIORITY; }

    @Override
    public boolean equals(Object obj) {
       if (this == obj) {
           return true;
       }
       if (!(obj instanceof Task)) {
           return false;
       }
       Task other = (Task) obj;
       return this.id == other.id;
    }

    @Override
    public int hashCode() {
       return Integer.hashCode(id);
    }

    /**
     * 【開始日を取得】
     * 予定開始日や実績開始日として利用できます。
     */
    public LocalDate getStartDate() { return startDate; }

    /**
     * 【開始日を設定】
     * 日付入力フォームの値をこのオブジェクトに保存する処理に使います。
     */
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    /**
     * 【終了日を取得】
     * 予定終了日や完了予定日などに使います。
     */
    public LocalDate getEndDate() { return endDate; }

    /**
     * 【終了日を設定】
     * 日付更新時に呼ばれるメソッドです。
     */
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    // ==========================================
    //  【toStringメソッド】
    // ==========================================
    // 【重要】@Override: 親クラスのメソッドを上書きするアノテーション
    // 【重要】toString(): オブジェクトを文字列表現に変換するメソッド
    // 【コードの読み方】
    // - オブジェクトを文字列として扱う際、タスク名を返す
    @Override
    public String toString() {
        return name;
    }
}