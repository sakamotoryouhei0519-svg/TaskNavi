package org.tasknavi.ui.calendar;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.AppTheme;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;

/**
 * カレンダー上部ナビ（前／今日／次・期間タイトル・月／週切替）。
 */
public final class CalendarNavToolbar {

    public interface Actions {
        boolean isMonthView();

        void onPrevious();

        void onNext();

        void onToday();

        void onMonthView();

        void onWeekView();
    }

    public record Result(JPanel panel, JLabel periodTitle, JButton monthButton, JButton weekButton) {
        public void updateViewSwitchStyle(boolean monthActive) {
            applyViewSwitchStyle(monthButton, monthActive);
            applyViewSwitchStyle(weekButton, !monthActive);
        }
    }

    private CalendarNavToolbar() {
    }

    public static Result create(Actions actions) {
        JPanel panel = new JPanel(new MigLayout("fillx, insets 4 2 6 2, gap 12", "[left][grow,center][right]"));
        panel.setBackground(AppTheme.BACKGROUND);

        JPanel moveGroup = new JPanel(new MigLayout("insets 2, gap 2", "[][][]"));
        moveGroup.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : new Color(241, 245, 249));
        moveGroup.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 10, 1, 1, 1, 1));

        JButton btnPrev = createNavButton(AppMessages.get("calendar.button.prev", "＜"));
        btnPrev.setToolTipText(AppMessages.get("calendar.tooltip.prev", "前の期間へ"));
        btnPrev.addActionListener(e -> actions.onPrevious());

        JButton btnToday = createNavButton(AppMessages.get("calendar.nav.today", "今日"));
        btnToday.setToolTipText(AppMessages.get("calendar.tooltip.today", "本日の日付にジャンプ"));
        btnToday.addActionListener(e -> actions.onToday());

        JButton btnNext = createNavButton(AppMessages.get("calendar.button.next", "＞"));
        btnNext.setToolTipText(AppMessages.get("calendar.tooltip.next", "次の期間へ"));
        btnNext.addActionListener(e -> actions.onNext());

        moveGroup.add(btnPrev);
        moveGroup.add(btnToday);
        moveGroup.add(btnNext);

        JLabel lblPeriodTitle = new JLabel("", SwingConstants.CENTER);
        lblPeriodTitle.setFont(AppTheme.FONT_TITLE.deriveFont(Font.BOLD, 18f));
        lblPeriodTitle.setForeground(AppTheme.TEXT_PRIMARY);

        JPanel viewSwitchGroup = new JPanel(new MigLayout("insets 2, gap 2", "[][]"));
        viewSwitchGroup.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : new Color(241, 245, 249));
        viewSwitchGroup.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 10, 1, 1, 1, 1));

        boolean month = actions.isMonthView();
        JButton btnMonthView = createViewSwitchButton(
                AppMessages.get("calendar.view.month", "月表示"), month);
        JButton btnWeekView = createViewSwitchButton(
                AppMessages.get("calendar.view.week", "週表示"), !month);

        btnMonthView.addActionListener(e -> actions.onMonthView());
        btnWeekView.addActionListener(e -> actions.onWeekView());

        viewSwitchGroup.add(btnMonthView);
        viewSwitchGroup.add(btnWeekView);

        panel.add(moveGroup, "left");
        panel.add(lblPeriodTitle, "center");
        panel.add(viewSwitchGroup, "right");

        return new Result(panel, lblPeriodTitle, btnMonthView, btnWeekView);
    }

    private static JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(AppTheme.FONT_MAIN.deriveFont(Font.BOLD, 12f));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBackground(AppTheme.isDarkMode() ? new Color(42, 50, 61) : Color.WHITE);
        btn.setForeground(AppTheme.TEXT_PRIMARY);
        btn.setBorder(null);
        btn.setMargin(new Insets(4, 10, 4, 10));
        return btn;
    }

    private static JButton createViewSwitchButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(AppTheme.FONT_MAIN.deriveFont(Font.BOLD, 12f));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(5, 14, 5, 14));
        applyViewSwitchStyle(btn, active);
        return btn;
    }

    static void applyViewSwitchStyle(JButton btn, boolean active) {
        if (btn == null) {
            return;
        }
        if (active) {
            btn.setBackground(AppTheme.PRIMARY);
            btn.setForeground(Color.WHITE);
            btn.setBorder(AppTheme.createRoundedBorder(AppTheme.PRIMARY_DARK, 6, 1, 1, 1, 1));
        } else {
            btn.setBackground(AppTheme.isDarkMode() ? new Color(42, 50, 61) : Color.WHITE);
            btn.setForeground(AppTheme.TEXT_MUTED);
            btn.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 6, 1, 1, 1, 1));
        }
    }
}
