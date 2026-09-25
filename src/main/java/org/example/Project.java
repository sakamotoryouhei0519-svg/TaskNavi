// packageとは、このファイルがどのグループ（パッケージ）に属しているかを宣言するもの
package org.example;

// importとは、他のファイルやライブラリから機能を借りてくること
// ArrayListとは、可変長の配列（リスト）を実装したクラス
import java.util.ArrayList;
// Collectionsとは、コレクション（リストやセットなど）を操作するためのユーティリティクラス
import java.util.Collections;
// Listとは、複数のデータを順番に並べて管理するインターフェース
import java.util.List;

/**
 * 大項目（Project）
 * Stage を複数持ち、その進捗率の平均を計算する責務を持つ。
 */
// public class Projectとは、Projectというクラスを宣言していること
public class Project {
    // private finalとは、このクラス内でだけ使えて変更できない値を宣言すること
    // intとは、整数を扱う型
    private final int id;
    // Stringとは、文字列を扱う型
    private final String name;
    // List<Stage>とは、Stageオブジェクトのリストを扱う型
    private final List<Stage> stages;

    // public Project()とは、コンストラクタ（クラスを作る時に実行される初期化処理）のこと
    public Project(int id, String name) {
        // this(...)とは、同じクラスの別のコンストラクタを呼び出すこと
        this(id, name, new ArrayList<>());
    }

    // public Projectとは、コンストラクタ（クラスを作る時に実行される初期化処理）のこと
    public Project(int id, String name, List<Stage> stages) {
        // this.id = idとは、引数で受け取ったidをフィールドのidに代入すること
        // thisとは、現在のオブジェクト自身を指すキーワード
        this.id = id;
        this.name = name;
        // stagesがnullでなければ新しいリストを作成、そうでなければ空のリストを作成
        //「必ず何かしらのリストオブジェクト（中身があるか、空っぽか）がセットされている状態」 を作ることで、
        // 絶対にエラーが起きない頑丈なコードにしている
        // ? : は三項演算子で、条件 ? 真の時の値 : 偽の時の値 という書き方
        this.stages = stages != null ? new ArrayList<>(stages) : new ArrayList<>();
    }

    // public intとは、整数を返すメソッド
    public int getId() {
        return id;
    }

    // public Stringとは、文字列を返すメソッド
    public String getName() {
        return name;
    }

    // public List<Stage>とは、Stageのリストを返すメソッド
    public List<Stage> getStages() {
        // unmodifiableListとは、変更できないリスト（読み取り専用）を作ること
        return Collections.unmodifiableList(stages);
    }

    // public voidとは、戻り値がないメソッド
    public void addStage(Stage stage) {
        // ifとは、もし〜だったらという条件分岐
        // stage == nullとは、stageが空（何も入っていない）かどうかを確認すること
        if (stage == null) {
            // returnとは、メソッドを終了して戻ること
            return;
        }
        // addとは、リストに要素を追加すること
        stages.add(stage);
    }

    // public intとは、整数を返すメソッド
    public int calculateChildrenProgressAverage() {
        // isEmptyとは、リストが空かどうかを確認すること

        /*なぜ大事？:
        平均値は「合計 ÷ 個数」で計算します。
        もしステージが1つもない（個数が0）の状態で割り算を行うと、
        「0で割るエラー（ArithmeticException）」が起きてアプリが強制終了してしまいます。
         役割:
         ステージが0個のときは、計算に進まず即座に「進捗率 0%」を返して
         安全に処理を終わらせています。*/
        if (stages.isEmpty()) {
            return 0;
        }

        /*なぜ大事？:
        リストに入っている複数のステージを1つずつ順番に取り出し、
        各ステージに「あなたの進捗率は何%？」と尋ねて total に加算（+=）していきます。

        オブジェクト指向のポイント:
        Project 自身が細かい計算をするのではなく、
        配下の Stage に計算をお任せ（委譲）して結果だけを
        受け取っている点がキレイな設計のコツです。*/
        // totalとは、合計値を入れる変数
        int total = 0;
        // for-eachとは、リストの 要素を1つずつ取り出して繰り返すこと
        //for ( 1つ分のデータの型 変数名 : 複数のデータが入ったリストや配列 ) {
        //    繰り返したい処理
        //}
        for (Stage stage : stages) {
            // += とは、足し算して代入すること（total = total + ...と同じ）
            total += stage.calculateChildrenProgressAverage();
        }
        // Math.roundとは、小数を四捨五入すること
        // (float) totalとは、整数を浮動小数点数に変換すること
        // stages.size()とは、リストの要素数を取得すること
        return Math.round((float) total / stages.size());
    }

    /*2. 身近にある「Enum で管理すべきもの」の例実務のアプリ開発では、
    以下のようなデータは100% Enumで作成されます。
    曜 日 月・火・水・木・金・土・日の 7種類しかない
    注文ステータス 「注文完了」「発送準備中」「発送済み」「キャンセル」の 4パターンしかない
    ユーザー権限「管理者（ADMIN）」「一般ユーザー（USER）」「ゲスト（GUEST）」など
    決済方法「クレジットカード」「PayPay」「銀行振込」「コンビニ払い」など*/

    //「『選択肢が決まっているデータ』を見つけたら、String（文字列）ではなく Enum を使って書く！」
    // public Stringとは、文字列を返すメソッド
    public String getStatusSummary() {
        if (stages.isEmpty()) {
            return TaskStatus.NOT_STARTED.name();
        }

        // 平均進捗率を計算
        int average = calculateChildrenProgressAverage();
        // >= とは、以上を意味する比較演算子
        if (average >= 100) {
            return TaskStatus.COMPLETED.name();
        }
        // > とは、より大きいを意味する比較演算子
        if (average > 0) {
            return TaskStatus.IN_PROGRESS.name();
        }
        return TaskStatus.NOT_STARTED.name();
    }
}
