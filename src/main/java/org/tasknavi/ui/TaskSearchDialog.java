package org.tasknavi.ui;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.AppTheme;
import org.tasknavi.IconManager;
import org.tasknavi.SearchablePanel;
import org.tasknavi.Task;
import org.tasknavi.UiConstants;
import org.tasknavi.UiLabels;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 共通検索ダイアログ。
 * WBS / カンバン / ガントで使用できるよう、キーワード検索とステータス絞り込みを共通化する。
 */
public class TaskSearchDialog {
    public static final class SearchCriteria {
        private final String keyword;
        private final String status;

        private SearchCriteria(String keyword, String status) {
            this.keyword = org.tasknavi.util.SearchFilterUtil.normalizeKeyword(keyword);
            this.status = org.tasknavi.util.SearchFilterUtil.normalizeStatus(status);
        }

        public static SearchCriteria of(String keyword, String status) {
            return new SearchCriteria(keyword, status);
        }

        public String getKeyword() {
            return keyword;
        }

        public String getStatus() {
            return status;
        }

        public boolean isEmpty() {
            return (keyword == null || keyword.isEmpty()) && (status == null || status.isEmpty());
        }
    }

    private final Frame parent;
    private final JDialog dialog;
    private final JTextField txtKeyword = new JTextField(12);
    private final JComboBox<String> comboStatus = new JComboBox<>(new String[]{
            AppMessages.FILTER_ALL,
            Task.STATUS_NOT_STARTED,
            Task.STATUS_IN_PROGRESS,
            Task.STATUS_COMPLETED
    });
    private final SearchablePanel owner;
    private boolean confirmed = false;

    public TaskSearchDialog(Frame parent, SearchablePanel owner) {
        this(parent, owner, null, null);
    }

    public TaskSearchDialog(Frame parent, SearchablePanel owner, String initialKeyword, String initialStatus) {
        this.parent = parent;
        this.owner = owner;
        dialog = new JDialog(parent, AppMessages.get("search.title", "検索／フィルタ"), true);
        dialog.setLayout(new MigLayout("fill, insets 8, gap 8", "[grow]", "[][grow][]"));
        dialog.setMinimumSize(new Dimension(400, 280));
        dialog.setSize(420, 290);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.setBackground(AppTheme.BACKGROUND);
        dialog.getContentPane().setBackground(AppTheme.BACKGROUND);

        // ダーク/ライト両対応の色定数
        Color labelFg = AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY;
        Color fieldBg = AppTheme.getCurrentTheme().textFieldBackground();
        Color fieldFg = AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY;
        Color panelBg = AppTheme.PANEL_BG;
        Color bgColor  = AppTheme.BACKGROUND;
        Color btnOutlineBg = AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE;

        JPanel actionPanel = new JPanel(new MigLayout("insets 12 12 8 12, gap 8, align left", "[]"));
        actionPanel.setOpaque(true);
        actionPanel.setBackground(bgColor);

        JButton btnProjectFilter = IconManager.getIconButton(IconManager.IconType.FOLDER,
                AppMessages.get("search.tooltip.project", "プロジェクト"));
        btnProjectFilter.setToolTipText(AppMessages.get("search.tooltip.project.filter", "プロジェクトでフィルタ"));
        btnProjectFilter.setMargin(new Insets(3, 10, 3, 10));
        btnProjectFilter.setFocusPainted(false);
        btnProjectFilter.setOpaque(true);
        btnProjectFilter.setContentAreaFilled(true);
        btnProjectFilter.setBackground(btnOutlineBg);
        btnProjectFilter.setForeground(labelFg);
        btnProjectFilter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        btnProjectFilter.addActionListener(e -> {
            if (owner != null) {
                owner.handleExistingProject();
            }
        });

        actionPanel.add(btnProjectFilter);

        JPanel contentPanel = new JPanel(new MigLayout("fill, insets 16 16 16 16, wrap", "[right][grow,fill]", "[]12"));
        contentPanel.setBackground(panelBg);
        contentPanel.setOpaque(true);

        JLabel lblSearch = new JLabel(AppMessages.get("search.label.keyword", "検索") + ":");
        lblSearch.setForeground(AppTheme.TEXT_PRIMARY); // 文字色を明示的に設定
        contentPanel.add(lblSearch, "align right");

        txtKeyword.setPreferredSize(new Dimension(UiConstants.FIELD_WIDTH_COMPACT, UiConstants.FIELD_HEIGHT_COMPACT));
        txtKeyword.setBackground(fieldBg);
        txtKeyword.setForeground(fieldFg);
        txtKeyword.setCaretColor(fieldFg);
        txtKeyword.setFont(AppTheme.FONT_MAIN);
        txtKeyword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        contentPanel.add(txtKeyword, "wrap");

        JLabel lblStatus = new JLabel(AppMessages.get("search.label.status", "状態") + ":");
        lblStatus.setForeground(AppTheme.TEXT_PRIMARY); // 文字色を明示的に設定
        contentPanel.add(lblStatus, "align right");
        comboStatus.setPreferredSize(new Dimension(UiConstants.BUTTON_WIDTH_STANDARD, UiConstants.FIELD_HEIGHT_COMPACT));
        styleComboBox(comboStatus);
        UiLabels.installStatusRenderer(comboStatus);
        contentPanel.add(comboStatus, "wrap");

        JPanel footer = new JPanel(new MigLayout("insets 8 12 12 12, gap 8, align right", "[][]"));
        footer.setOpaque(true);
        footer.setBackground(bgColor);

        JButton btnApply = new JButton(AppMessages.get("common.button.search", "検索"));
        btnApply.setMargin(new Insets(4, 16, 4, 16));
        btnApply.setOpaque(true);
        btnApply.setContentAreaFilled(true);
        btnApply.setBackground(AppTheme.PRIMARY);
        btnApply.setForeground(Color.WHITE);
        btnApply.setFocusPainted(false);
        btnApply.addActionListener(e -> {
            if (owner != null) {
                SearchCriteria criteria = getCriteria();
                owner.applySearchFilter(criteria.getKeyword(), criteria.getStatus());
            }
            confirmed = true;
            dialog.dispose();
        });
        dialog.getRootPane().setDefaultButton(btnApply);

        JButton btnClearFilter = IconManager.getIconButton(IconManager.IconType.CROSS,
                AppMessages.get("common.button.clear", "クリア"));
        btnClearFilter.setToolTipText(AppMessages.get("search.tooltip.clear.filter", "フィルタをクリア"));
        btnClearFilter.setMargin(new Insets(4, 16, 4, 16));
        btnClearFilter.setOpaque(true);
        btnClearFilter.setContentAreaFilled(true);
        btnClearFilter.setBackground(btnOutlineBg);
        btnClearFilter.setForeground(labelFg);
        btnClearFilter.setFocusPainted(false);
        btnClearFilter.addActionListener(e -> {
            if (owner != null) {
                owner.handleClearFilter();
            }
            txtKeyword.setText("");
            comboStatus.setSelectedItem(AppMessages.FILTER_ALL);
        });

        footer.add(btnApply);
        footer.add(btnClearFilter);

        dialog.add(actionPanel, "grow, wrap");
        dialog.add(contentPanel, "grow, wrap");
        dialog.add(footer, "grow");

        if (initialKeyword != null) {
            txtKeyword.setText(initialKeyword);
        }
        if (initialStatus != null) {
            comboStatus.setSelectedItem(initialStatus);
        }

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmed = false;
            }
        });
    }

    public boolean showDialog() {
        dialog.setVisible(true);
        return confirmed;
    }

    public SearchCriteria getCriteria() {
        return SearchCriteria.of(txtKeyword.getText(), (String) comboStatus.getSelectedItem());
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setFont(AppTheme.FONT_MAIN);
        combo.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        combo.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String statusValue) {
                    setText(AppMessages.statusDisplay(statusValue));
                }
                if (AppTheme.isDarkMode()) {
                    if (isSelected) {
                        c.setBackground(new Color(96, 165, 250));
                        c.setForeground(Color.WHITE);
                    } else {
                        c.setBackground(new Color(30, 41, 59));
                        c.setForeground(new Color(226, 232, 240));
                    }
                }
                return c;
            }
        });
    }
}
