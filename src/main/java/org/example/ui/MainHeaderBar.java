package org.example.ui;

import org.example.AppMessages;
import org.example.AppTheme;
import org.example.IconManager;
import org.example.User;
import org.example.UserSession;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import net.miginfocom.swing.MigLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

/**
 * MainFrame 上部ヘッダー（ロゴ・ユーザー・I/O・テーマ・ログアウト）。
 */
public final class MainHeaderBar {

    public record Result(JPanel panel, JLabel userStatusLabel) {
    }

    public interface Actions {
        void onImport();

        void onExport();

        void onLogout();
    }

    private MainHeaderBar() {
    }

    public static Result create(JToggleButton themeToggleButton, Actions actions) {
        JPanel panel = new JPanel(new MigLayout("fillx, insets 0, gap 10", "[grow][]"));
        panel.setBackground(AppTheme.PANEL_BG);
        panel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR),
                new EmptyBorder(8, 18, 8, 18)
        ));

        JPanel titleArea = new JPanel(new MigLayout("insets 0, gap 10, align left", "[][][]"));
        titleArea.setOpaque(false);

        JLabel logoIcon = IconManager.getIconLabel(IconManager.IconType.LOGO);
        logoIcon.setFont(new Font("SansSerif", Font.PLAIN, 24));

        JLabel logoLabel = new JLabel(AppMessages.get("mainframe.title"));
        logoLabel.setFont(AppTheme.FONT_HEADER.deriveFont(Font.BOLD, 17f));
        logoLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel(AppMessages.get("mainframe.tagline"));
        subtitleLabel.setFont(AppTheme.FONT_MAIN);
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);

        titleArea.add(logoIcon);
        titleArea.add(logoLabel);
        titleArea.add(subtitleLabel);

        User currentUser = UserSession.getCurrentUser();
        String currentUsername = currentUser != null
                ? currentUser.getDisplayNameOrUsername()
                : AppMessages.get("mainframe.user.guest");

        JLabel userStatusLabel = new JLabel(IconManager.getIcon(IconManager.IconType.USER) + " " + currentUsername);
        userStatusLabel.setFont(AppTheme.FONT_MAIN);
        userStatusLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JButton logoutButton = createHeaderIconButton(AppMessages.get("mainframe.button.logout"), IconManager.IconType.LOGOUT);
        logoutButton.addActionListener(e -> actions.onLogout());

        JButton exportButton = createHeaderIconButton(AppMessages.get("mainframe.button.export"), IconManager.IconType.EXPORT);
        exportButton.addActionListener(e -> actions.onExport());

        JButton importButton = createHeaderIconButton(AppMessages.get("mainframe.button.import"), IconManager.IconType.IMPORT);
        importButton.addActionListener(e -> actions.onImport());

        Object themeIcon = IconManager.getIcon(IconManager.IconType.THEME);
        if (themeIcon instanceof String) {
            themeToggleButton.setText((String) themeIcon);
        } else {
            themeToggleButton.setIcon((ImageIcon) themeIcon);
        }
        themeToggleButton.setBackground(Color.WHITE);
        themeToggleButton.setForeground(AppTheme.TEXT_PRIMARY);
        themeToggleButton.setContentAreaFilled(false);
        themeToggleButton.setOpaque(false);
        themeToggleButton.setFocusPainted(false);
        themeToggleButton.setBorderPainted(false);
        themeToggleButton.setBorder(new EmptyBorder(4, 6, 4, 6));
        themeToggleButton.setPreferredSize(new Dimension(40, 36));
        themeToggleButton.setMinimumSize(new Dimension(40, 36));
        themeToggleButton.setToolTipText(AppMessages.get("mainframe.button.theme"));

        JPanel rightUserArea = new JPanel(new MigLayout("insets 0, gap 10, align right", "[][][][][]"));
        rightUserArea.setOpaque(false);
        rightUserArea.add(userStatusLabel);
        rightUserArea.add(importButton);
        rightUserArea.add(exportButton);
        rightUserArea.add(themeToggleButton);
        rightUserArea.add(logoutButton);

        panel.add(titleArea, "growx");
        panel.add(rightUserArea, "right");
        return new Result(panel, userStatusLabel);
    }

    private static JButton createHeaderIconButton(String text, IconManager.IconType iconType) {
        JButton button = IconManager.getIconButton(iconType, text);
        button.putClientProperty("app.button.variant", "header");
        button.setFont(AppTheme.FONT_MAIN);
        button.setBackground(AppTheme.PANEL_BG);
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 7, 1, 1, 1, 1));
        button.setMargin(new Insets(4, 10, 4, 10));
        return button;
    }
}
