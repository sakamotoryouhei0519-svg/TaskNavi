package org.tasknavi.ui.main;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.model.User;
import org.tasknavi.service.UserSession;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.IconManager;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

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

    public static Result create(JToggleButton themeToggleButton, JComponent projectArea, Actions actions) {
        JPanel panel = new JPanel(new MigLayout("fillx, insets 0, gap 10", "[grow][]"));
        panel.setBackground(AppTheme.PANEL_BG);
        panel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR),
                new EmptyBorder(8, 18, 8, 18)
        ));

        JPanel titleArea = new JPanel(new MigLayout("insets 0, gap 10, align left", "[][][][]"));
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
        titleArea.add(projectArea, "gapleft 8");
        titleArea.add(subtitleLabel, "wmin 0, gapleft 4");

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

        styleThemeToggle(themeToggleButton);

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
        Object icon = IconManager.getIcon(iconType);
        JButton button;
        if (icon instanceof ImageIcon imageIcon) {
            button = new AppTheme.RoundedFillButton(text, 8);
            button.setIcon(imageIcon);
            button.setHorizontalTextPosition(SwingConstants.RIGHT);
        } else {
            button = new AppTheme.RoundedFillButton(icon + "  " + text, 8);
        }
        button.putClientProperty("app.button.variant", "header");
        button.setFont(AppTheme.FONT_HEADER.deriveFont(Font.BOLD, 13f));
        button.setIconTextGap(6);
        button.setBackground(AppTheme.controlBackground());
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorderPainted(true);
        button.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 2, 2, 2, 2));
        button.setRolloverEnabled(true);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        int contentWidth = button.getFontMetrics(button.getFont()).stringWidth(button.getText());
        if (button.getIcon() != null) {
            contentWidth += button.getIcon().getIconWidth() + button.getIconTextGap();
        }
        Dimension size = new Dimension(contentWidth + 28, 32);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        return button;
    }

    static void styleThemeToggle(JToggleButton themeToggleButton) {
        Object themeIcon = IconManager.getIcon(IconManager.IconType.THEME);
        if (themeIcon instanceof String) {
            themeToggleButton.setText((String) themeIcon);
            themeToggleButton.setIcon(null);
        } else {
            themeToggleButton.setText("");
            themeToggleButton.setIcon((ImageIcon) themeIcon);
        }
        themeToggleButton.setBackground(Color.WHITE);
        themeToggleButton.setForeground(AppTheme.TEXT_PRIMARY);
        themeToggleButton.setContentAreaFilled(false);
        themeToggleButton.setOpaque(false);
        themeToggleButton.setFocusPainted(false);
        themeToggleButton.setBorderPainted(false);
        themeToggleButton.setBorder(new EmptyBorder(4, 6, 4, 6));
        themeToggleButton.setPreferredSize(new Dimension(36, 32));
        themeToggleButton.setMinimumSize(new Dimension(36, 32));
        themeToggleButton.setToolTipText(AppMessages.get("mainframe.button.theme"));
    }
}
