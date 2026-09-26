package org.tasknavi;

import org.tasknavi.ui.auth.LoginFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.time.LocalDateTime;

/**
 * アプリケーション起動入口。
 * DB／サンプル準備のあと、スプラッシュを表示しログインまたはメイン画面へ進む。
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int SPLASH_DELAY_MS = 1000;

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logger.error("未処理の例外が発生しました: " + thread.getName(), throwable);
            if (SwingUtilities.isEventDispatchThread()) {
                showFatalStartupError(
                        "アプリケーションで予期せぬエラーが発生しました。\n詳細はログを確認してください。",
                        throwable);
            } else {
                SwingUtilities.invokeLater(() -> showFatalStartupError(
                        "アプリケーションで予期せぬエラーが発生しました。\n詳細はログを確認してください。",
                        throwable));
            }
        });

        try {
            SampleDataUtil.insertSampleTasksIfEmpty();
        } catch (Exception e) {
            logger.error("サンプルデータ初期化エラー", e);
            showFatalStartupError("初期データの準備に失敗しました。\nアプリケーションを再起動してください。", e);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                logger.error("Look & Feel設定エラー", e);
            }

            applyGlobalUiDefaults();

            JWindow splash = createSplashWindow();
            splash.setVisible(true);

            Timer timer = new Timer(SPLASH_DELAY_MS, e -> {
                splash.dispose();
                if (shouldSkipLogin()) {
                    autoLoginForTest();
                    openMainFrame();
                    return;
                }
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
            timer.setRepeats(false);
            timer.start();
        });
    }

    /** `-Dtasknavi.skipLogin=true` または環境変数 {@code TASKNAVI_SKIP_LOGIN} でログインを省略。 */
    public static boolean shouldSkipLogin() {
        String value = System.getProperty("tasknavi.skipLogin");
        if (value == null || value.isBlank()) {
            value = System.getenv("TASKNAVI_SKIP_LOGIN");
        }
        return Boolean.parseBoolean(value);
    }

    /** テスト／デモ用の自動ログイン。skipLogin が有効なときだけ動作する。 */
    public static void autoLoginForTest() {
        if (!shouldSkipLogin()) {
            return;
        }
        String username = System.getProperty("tasknavi.autoLoginUser", "testuser");
        String email = System.getProperty("tasknavi.autoLoginEmail", username + "@example.com");
        UserSession.login(new User(0, username, email, "USER", username, LocalDateTime.now()));
    }

    private static void applyGlobalUiDefaults() {
        UIManager.put("Button.background", Color.WHITE);
        UIManager.put("Button.foreground", AppTheme.TEXT_PRIMARY);
        UIManager.put("Button.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER_COLOR, 1, true),
                new EmptyBorder(4, 12, 4, 12)
        ));
        UIManager.put("ToggleButton.background", Color.WHITE);
        UIManager.put("ToggleButton.foreground", AppTheme.TEXT_PRIMARY);
        UIManager.put("Panel.background", AppTheme.PANEL_BG);
        UIManager.put("Label.foreground", AppTheme.TEXT_PRIMARY);
    }

    public static void openMainFrame() {
        MainFrame mainFrame = new MainFrame(TaskService.createDefault());
        mainFrame.setVisible(true);
        UiDebugUtil.dumpComponentTree("MainFrame", mainFrame);
    }

    private static JWindow createSplashWindow() {
        JWindow splash = new JWindow();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppTheme.BACKGROUND);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel titleLabel = new JLabel("TaskNavi");
        titleLabel.setFont(AppTheme.FONT_TITLE);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitleLabel = new JLabel(AppMessages.get("main.splash.loading"));
        subtitleLabel.setFont(AppTheme.FONT_MAIN);
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(titleLabel, BorderLayout.CENTER);
        panel.add(subtitleLabel, BorderLayout.SOUTH);

        splash.setContentPane(panel);
        splash.setSize(420, 180);
        splash.setLocationRelativeTo(null);
        return splash;
    }

    private static void showFatalStartupError(String message, Throwable throwable) {
        logger.error(message, throwable);
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    null,
                    message,
                    AppMessages.get("main.startup.error.title"),
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        });
    }
}
