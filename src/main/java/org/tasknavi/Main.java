package org.tasknavi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasknavi.persistence.AppDataLocation;
import org.tasknavi.persistence.Database;
import org.tasknavi.persistence.SampleDataUtil;
import org.tasknavi.service.TaskService;
import org.tasknavi.team.TeamSettings;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.NoticeBar;
import org.tasknavi.ui.main.MainFrame;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

/**
 * アプリケーション起動入口。
 * DB／サンプル準備のあと、スプラッシュを表示してメイン画面へ進む。ログインはない（PC は Windows のログインで守る）。
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int SPLASH_DELAY_MS = 1000;
    /** 描画の中の失敗などは続けて起きるので、お知らせはこの間隔に 1 回まで。 */
    private static final long UNEXPECTED_NOTICE_INTERVAL_MS = 30_000;
    /** 開くまでは null（起動の途中）。 */
    private static volatile MainFrame mainFrame;
    private static long lastUnexpectedNoticeAt;

    public static void main(String[] args) {
        logger.info("TaskNavi {} を起動します（Java {} / {} {}）", AppVersion.current(),
                System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.version"));
        TeamSettings.applyLaunchArguments(args);
        Thread.setDefaultUncaughtExceptionHandler(Main::handleUncaught);

        Database.prepareOnStartup();

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
                logger.error("Look & Feel設定エラー", e);
            }

            applyGlobalUiDefaults();

            JWindow splash = createSplashWindow();
            splash.setVisible(true);

            Timer timer = new Timer(SPLASH_DELAY_MS, e -> {
                splash.dispose();
                SampleDataUtil.insertSampleTasksIfEmpty();
                openMainFrame();
            });
            timer.setRepeats(false);
            timer.start();
        });
    }

    private static void applyGlobalUiDefaults() {
        AppTheme.applySwingDefaults();
    }

    public static void openMainFrame() {
        MainFrame frame = new MainFrame(TaskService.createDefault());
        frame.setVisible(true);
        frame.startDueReminders();
        mainFrame = frame;
    }

    /**
     * どこでも受けなかった例外。必ずログに残す。起動の途中なら続けられないので知らせて終わる。
     * メイン画面が出た後は、保存は操作ごとに済んでいるので、お知らせ帯で知らせて使い続けられるようにする。
     */
    private static void handleUncaught(Thread thread, Throwable throwable) {
        logger.error("未処理の例外が発生しました: " + thread.getName(), throwable);
        String message = AppMessages.format("main.error.unexpected",
                "予期せぬエラーが発生しました。続けておかしいときは、アプリを開き直してください。詳しくはログ: {0}",
                AppDataLocation.logDirectory());
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = mainFrame;
            if (frame == null) {
                showFatalStartupError(message);
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastUnexpectedNoticeAt >= UNEXPECTED_NOTICE_INTERVAL_MS) {
                lastUnexpectedNoticeAt = now;
                NoticeBar.error(frame, message);
            }
        });
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

    private static void showFatalStartupError(String message) {
        JOptionPane.showMessageDialog(null, message, AppMessages.get("main.startup.error.title"), JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }
}
