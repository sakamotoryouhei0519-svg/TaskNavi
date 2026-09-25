// packageとは、このファイルがどのグループ（パッケージ）に属しているかを宣言するもの
// org.exampleというグループに属していることを示しています
package org.example;

// importとは、他のファイルやライブラリから機能を借りてくること
// javax.swing.*は、GUI（画面を作るための部品）を使うための機能をまとめて借りています

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;

// EmptyBorderとは、部品の周りに空白を作るための枠線のこと
// java.awt.*は、画面の色や配置など、GUIの基本的な機能を借りています
// LocalDateTimeとは、日付と時刻を扱うための型のこと

// public class Mainとは、Mainという名前のクラス（設計図）を宣言していること
// クラスとは、プログラムの部品をまとめた箱のようなもの
public class Main {
    // private static finalとは、このクラス内でだけ使える固定の値を宣言すること
    // intとは、整数を扱う型のこと
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    // SPLASH_DELAY_MSとは、起動画面を表示する時間（ミリ秒）を表す変数名
    // 1000ミリ秒＝1秒間、起動画面を表示することを意味します
    private static final int SPLASH_DELAY_MS = 1000;

    // public static void mainとは、プログラムの最初に実行される特別なメソッド
    // String[] argsとは、コマンドラインから渡される引数（文字列の配列）のこと
    public static void main(String[] args) {
        // Thread.setDefaultUncaughtExceptionHandlerとは、予期せぬエラーが起きた時の処理を登録すること
        // (thread, throwable) -> {} はラムダ式という書き方で、エラーが起きた時の処理を简潔に書いています
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // Logger.errorとは、エラー情報をログ（記録）に残すこと
            logger.error("未処理の例外が発生しました: " + thread.getName(), throwable);
            // ifとは、もし〜だったらという条件分岐のこと
            // SwingUtilities.isEventDispatchThreadとは、今画面描画用のスレッドで動いているかを確認すること
            if (SwingUtilities.isEventDispatchThread()) {
                // 画面描画用スレッドで動いているなら、直接エラー画面を表示
                showFatalStartupError("アプリケーションで予期せぬエラーが発生しました。\n詳細はログを確認してください。", throwable);
            } else {
                // elseとは、もし〜でなかったらという意味
                // 画面描画用スレッドでないなら、後で実行するように予約
                SwingUtilities.invokeLater(() -> showFatalStartupError("アプリケーションで予期せぬエラーが発生しました。\n詳細はログを確認してください。", throwable));
            }
        });

        // try-catchとは、エラーが起きるかもしれない処理を安全に実行するための仕組み
        try {
            // SampleDataUtil.insertSampleTasksIfEmptyとは、サンプルデータを準備するメソッド
            SampleDataUtil.insertSampleTasksIfEmpty();
        } catch (Exception e) {
            // catchとは、エラーが起きた時の処理を書く場所
            // Exception eとは、発生したエラー情報を変数eに入れること
            logger.error("サンプルデータ初期化エラー", e);
            showFatalStartupError("初期データの準備に失敗しました。\nアプリケーションを再起動してください。", e);
            return;
        }

        // SwingUtilities.invokeLaterとは、画面描画用のスレッドで後で実行するように予約すること
        SwingUtilities.invokeLater(() -> {
            // try-catchでエラー処理を安全に行います
            try {
                // UIManager.setLookAndFeelとは、画面の見た目（デザイン）を設定すること
                // getCrossPlatformLookAndFeelClassNameとは、どのOSでも同じ見た目にするデザインの名前を取得すること
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                // エラーが起きたらログに記録
                logger.error("Look & Feel設定エラー", e);
            }

            // applyGlobalUiDefaultsとは、画面全体のデザイン設定を適用するメソッド
            applyGlobalUiDefaults();

            // JWindowとは、タイトルバーのないシンプルなウィンドウのこと
            // createSplashWindowとは、起動画面を作るメソッド
            JWindow splash = createSplashWindow();
            // setVisible(true)とは、ウィンドウを表示すること
            splash.setVisible(true);

            // Timerとは、指定した時間後に何かを実行するためのタイマーのこと
            // SPLASH_DELAY_MS（1000ミリ秒）後に、中の処理を実行します
            Timer timer = new Timer(SPLASH_DELAY_MS, e -> {
                // splash.dispose()とは、起動画面を閉じること
                splash.dispose();
                // shouldSkipLogin()とは、ログインをスキップするかどうかを確認するメソッド
                if (shouldSkipLogin()) {
                    // テスト用の自動ログインを実行
                    autoLoginForTest();
                    // メイン画面を開く
                    openMainFrame();
                    // returnとは、ここで処理を終了すること
                    return;
                }
                // LoginFrameとは、ログイン画面のクラス
                // new LoginFrame()とは、ログイン画面を作ること
                LoginFrame loginFrame = new LoginFrame();
                // ログイン画面を表示
                loginFrame.setVisible(true);
            });
            // setRepeats(false)とは、タイマーを1回だけ実行すること
            timer.setRepeats(false);
            // start()とは、タイマーを開始すること
            timer.start();
        });
    }

    // public static booleanとは、どこからでも呼び出せる真偽値（true/false）を返すメソッド
    public static boolean shouldSkipLogin() {
        // System.getPropertyとは、システム設定（プロパティ）から値を取得すること
        // String valueとは、文字列型の変数valueを宣言すること
        String value = System.getProperty("tasknavi.skipLogin");
        // ifとは、もし〜だったらという条件分岐
        // value == nullとは、valueが空（何も入っていない）かどうかを確認すること
        // ||とは、または（OR）という意味
        // value.isBlank()とは、valueが空白かどうかを確認すること
        if (value == null || value.isBlank()) {
            // System.getenvとは、環境変数から値を取得すること
            value = System.getenv("TASKNAVI_SKIP_LOGIN");
        }
        // Boolean.parseBooleanとは、文字列をtrue/falseに変換すること
        // returnとは、結果を返すこと
        return Boolean.parseBoolean(value);
    }

    // public static voidとは、どこからでも呼び出せる戻り値がないメソッド
    public static void autoLoginForTest() {
        // !shouldSkipLogin()とは、ログインスキップが無効ならという意味
        // !とは、否定（NOT）の意味
        if (!shouldSkipLogin()) {
            // スキップしないなら、ここで終了
            return;
        }

        // ユーザー名をシステム設定から取得、なければ"testuser"を使う
        String username = System.getProperty("tasknavi.autoLoginUser", "testuser");
        // メールアドレスをシステム設定から取得、なければユーザー名+"@example.com"を使う
        String email = System.getProperty("tasknavi.autoLoginEmail", username + "@example.com");
        // UserSession.loginとは、ユーザーをログイン状態にするメソッド
        // new User()とは、Userクラスのインスタンス（実体）を作ること
        UserSession.login(new User(0, username, email, "USER", username, LocalDateTime.now()));
    }

    // private static voidとは、このクラス内でだけ使える戻り値がないメソッド
    private static void applyGlobalUiDefaults() {
        // UIManager.putとは、画面部品のデザイン設定を登録すること
        // "Button.background"とは、ボタンの背景色の設定名
        // Color.WHITEとは、白色のこと
        UIManager.put("Button.background", Color.WHITE);
        // ボタンの文字色を設定
        UIManager.put("Button.foreground", AppTheme.TEXT_PRIMARY);
        // ボタンの枠線を設定
        // BorderFactory.createCompoundBorderとは、複数の枠線を組み合わせること
        UIManager.put("Button.border", BorderFactory.createCompoundBorder(
                // BorderFactory.createLineBorderとは、直線の枠線を作ること
                BorderFactory.createLineBorder(AppTheme.BORDER_COLOR, 1, true),
                // EmptyBorderとは、空白の枠線を作ること（上下左右の余白を指定）
                new EmptyBorder(4, 12, 4, 12)
        ));
        // トグルボタン（オン/オフ切り替えボタン）の背景色
        UIManager.put("ToggleButton.background", Color.WHITE);
        // トグルボタンの文字色
        UIManager.put("ToggleButton.foreground", AppTheme.TEXT_PRIMARY);
        // パネル（画面の部品を乗せる板）の背景色
        UIManager.put("Panel.background", AppTheme.PANEL_BG);
        // ラベル（文字表示部品）の文字色
        UIManager.put("Label.foreground", AppTheme.TEXT_PRIMARY);
    }

    // public static voidとは、どこからでも呼び出せる戻り値がないメソッド
    public static void openMainFrame() {
        // MainFrameに必要な TaskService を準備する
        TaskDao taskDao = new TaskDao();
        TaskService taskService = new TaskService(taskDao);

        // TaskService を渡して MainFrame を作成する
        MainFrame mainFrame = new MainFrame(taskService);
        // メイン画面を表示
        mainFrame.setVisible(true);
        // UiDebugUtil.dumpComponentTreeとは、画面部品の構造をログに出力するデバッグ用メソッド
        UiDebugUtil.dumpComponentTree("MainFrame", mainFrame);
    }

    // private static JWindowとは、このクラス内でだけ使えるJWindow型を返すメソッド
    private static JWindow createSplashWindow() {
        // JWindowとは、タイトルバーのないシンプルなウィンドウのこと
        JWindow splash = new JWindow();
        // JPanelとは、画面部品を乗せる板（パネル）のこと
        // new BorderLayout()とは、部品を東西南北と中央に配置するレイアウトのこと
        JPanel panel = new JPanel(new BorderLayout());
        // パネルの背景色を設定
        panel.setBackground(AppTheme.BACKGROUND);
        // パネルの周りに余白（空白の枠線）を設定
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        // JLabelとは、文字を表示する部品のこと
        JLabel titleLabel = new JLabel("TaskNavi");
        // フォント（文字の大きさや種類）を設定
        titleLabel.setFont(AppTheme.FONT_TITLE);
        // 文字色を設定
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        // 文字を中央揃えに設定
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // サブタイトルラベルを作成
        JLabel subtitleLabel = new JLabel(AppMessages.get("main.splash.loading"));
        subtitleLabel.setFont(AppTheme.FONT_MAIN);
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // panel.addとは、パネルに部品を追加すること
        // BorderLayout.CENTERとは、中央に配置すること
        panel.add(titleLabel, BorderLayout.CENTER);
        // BorderLayout.SOUTHとは、下側に配置すること
        panel.add(subtitleLabel, BorderLayout.SOUTH);

        // splash.setContentPaneとは、ウィンドウの中身をパネルにすること
        splash.setContentPane(panel);
        // ウィンドウのサイズを幅420、高さ180に設定
        splash.setSize(420, 180);
        // setLocationRelativeTo(null)とは、画面の中央に配置すること
        splash.setLocationRelativeTo(null);
        // 作ったウィンドウを返す
        return splash;
    }

    // private static voidとは、このクラス内でだけ使える戻り値がないメソッド
    // String messageとは、エラーメッセージを表す文字列型の引数
    // Throwable throwableとは、エラーの詳細情報を表す引数
    private static void showFatalStartupError(String message, Throwable throwable) {
        // 修正: logger フィールドを使用
        logger.error(message, throwable);

        // 画面描画用スレッドでエラーダイアログを表示
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, AppMessages.get("main.startup.error.title"), JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        });
    }
}