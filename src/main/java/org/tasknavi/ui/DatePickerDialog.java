package org.tasknavi.ui;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 月のカレンダーから日付を選ぶ窓。選んだ日を YYYY-MM-DD で入力欄に入れて閉じる。
 */
public class DatePickerDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    /** 表示中の月。ボタンのラムダから書き換えるので配列に入れる。 */
    private final LocalDate[] selectedDate;
    private final JTextField targetField;

    public DatePickerDialog(Frame parent, JTextField targetField) {
        super(parent, AppMessages.get("datepicker.title", "日付を選択"), true);
        this.targetField = targetField;
        this.selectedDate = new LocalDate[]{LocalDate.now()};

        setLocationRelativeTo(getParent());
        setLayout(new MigLayout("fill, insets 5, gap 5", "[grow]", "[][grow]"));
        setBackground(AppTheme.PANEL_BG);
        buildCalendarUI();
        DialogFit.showAll(this);
        DialogKeys.install(this, null, this::dispose);
        setVisible(true);
    }

    private void buildCalendarUI() {
        JPanel headerPanel = new JPanel(new MigLayout("insets 0, gap 10, align center", "[][][]"));
        headerPanel.setBackground(AppTheme.PANEL_BG);

        JButton btnPrev = new JButton(AppMessages.get("calendar.button.prev", "＜"));
        AppTheme.styleDialogSecondaryButton(btnPrev);

        JLabel lblYearMonth = new JLabel("", SwingConstants.CENTER);
        lblYearMonth.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblYearMonth.setForeground(AppTheme.TEXT_PRIMARY);

        JButton btnNext = new JButton(AppMessages.get("calendar.button.next", "＞"));
        AppTheme.styleDialogSecondaryButton(btnNext);

        headerPanel.add(btnPrev);
        headerPanel.add(lblYearMonth);
        headerPanel.add(btnNext);

        JPanel calendarPanel = new JPanel(new MigLayout("fill, insets 0, gap 2, wrap", "[][][][][][][]"));
        calendarPanel.setBackground(AppTheme.PANEL_BG);

        Runnable updateCalendar = () -> {
            calendarPanel.removeAll();
            lblYearMonth.setText(selectedDate[0].format(DateTimeFormatter.ofPattern(
                    AppMessages.get("datepicker.date.format.yearmonth", "yyyy年 M月"))));

            String[] days = UiLabels.weekdaysSundayFirst();
            for (int i = 0; i < 7; i++) {
                JLabel lblDay = new JLabel(days[i], SwingConstants.CENTER);
                lblDay.setFont(new Font("SansSerif", Font.BOLD, 12));
                if (i == 0) {
                    lblDay.setForeground(AppTheme.sundayText());
                } else if (i == 6) {
                    lblDay.setForeground(AppTheme.saturdayText());
                } else {
                    lblDay.setForeground(AppTheme.TEXT_PRIMARY);
                }
                calendarPanel.add(lblDay);
            }

            LocalDate firstOfMonth = selectedDate[0].withDayOfMonth(1);
            // 日曜始まりなので、日曜を 0 にする（getValue は月曜 1〜日曜 7）
            int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
            for (int i = 0; i < startDayOfWeek; i++) {
                calendarPanel.add(new JLabel(""));
            }

            int lengthOfMonth = selectedDate[0].lengthOfMonth();
            for (int day = 1; day <= lengthOfMonth; day++) {
                final int selectedDay = day;
                JButton btnDay = new JButton(String.valueOf(day));
                if (selectedDate[0].getDayOfMonth() == day) {
                    AppTheme.styleDialogPrimaryButton(btnDay);
                } else {
                    AppTheme.styleDialogSecondaryButton(btnDay);
                }
                btnDay.setMargin(new Insets(4, 8, 4, 8));
                btnDay.addActionListener(e -> {
                    targetField.setText(selectedDate[0].withDayOfMonth(selectedDay).toString());
                    dispose();
                });
                calendarPanel.add(btnDay);
            }

            calendarPanel.revalidate();
            calendarPanel.repaint();
        };

        btnPrev.addActionListener(e -> {
            selectedDate[0] = selectedDate[0].minusMonths(1);
            updateCalendar.run();
        });
        btnNext.addActionListener(e -> {
            selectedDate[0] = selectedDate[0].plusMonths(1);
            updateCalendar.run();
        });
        updateCalendar.run();

        add(headerPanel, "grow, wrap");
        add(calendarPanel, "grow");
    }
}
