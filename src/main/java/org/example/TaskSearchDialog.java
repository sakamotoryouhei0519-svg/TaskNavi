package org.example;

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
            this.keyword = normalizeKeyword(keyword);
            this.status = normalizeStatus(status);
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

        private static String normalizeKeyword(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        private static String normalizeStatus(String value) {
            if (value == null || "すべて".equals(value)) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }

    private final Frame parent;
    private final JDialog dialog;
    private final JTextField txtKeyword = new JTextField(12);
    private final JComboBox<String> comboStatus = new JComboBox<>(new String[]{"すべて", "未着手", "進行中", "完了"});
    private final Runnable projectAction;
    private final Runnable clearAction;
    private boolean confirmed = false;

    public TaskSearchDialog(Frame parent, String initialKeyword, String initialStatus) {
        this(parent, initialKeyword, initialStatus, null, null);
    }

    public TaskSearchDialog(Frame parent, String initialKeyword, String initialStatus, Runnable projectAction, Runnable clearAction) {
        this.parent = parent;
        this.projectAction = projectAction;
        this.clearAction = clearAction;
        dialog = new JDialog(parent, "検索／フィルタ", true);
        dialog.setLayout(new BorderLayout(8, 8));
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

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actionPanel.setOpaque(true);
        actionPanel.setBackground(bgColor);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        JButton btnProjectFilter = IconManager.getIconButton(IconManager.IconType.FOLDER, "プロジェクト");
        btnProjectFilter.setToolTipText("プロジェクトでフィルタ");
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
            if (projectAction != null) {
                projectAction.run();
            }
        });

        actionPanel.add(btnProjectFilter);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contentPanel.setBackground(panelBg);
        contentPanel.setOpaque(true);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 12, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel lblKeyword = new JLabel("検索:");
        lblKeyword.setFont(AppTheme.FONT_MAIN);
        lblKeyword.setForeground(labelFg);
        lblKeyword.setOpaque(false);
        contentPanel.add(lblKeyword, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        txtKeyword.setPreferredSize(new Dimension(UiConstants.FIELD_WIDTH_COMPACT, UiConstants.FIELD_HEIGHT_COMPACT));
        txtKeyword.setBackground(fieldBg);
        txtKeyword.setForeground(fieldFg);
        txtKeyword.setCaretColor(fieldFg);
        txtKeyword.setFont(AppTheme.FONT_MAIN);
        txtKeyword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        contentPanel.add(txtKeyword, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel lblStatus = new JLabel("状態:");
        lblStatus.setFont(AppTheme.FONT_MAIN);
        lblStatus.setForeground(labelFg);
        lblStatus.setOpaque(false);
        contentPanel.add(lblStatus, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        comboStatus.setPreferredSize(new Dimension(UiConstants.BUTTON_WIDTH_STANDARD, UiConstants.FIELD_HEIGHT_COMPACT));
        styleComboBox(comboStatus);
        contentPanel.add(comboStatus, gbc);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(true);
        footer.setBackground(bgColor);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        JButton btnApply = new JButton("検索");
        btnApply.setMargin(new Insets(4, 16, 4, 16));
        btnApply.setOpaque(true);
        btnApply.setContentAreaFilled(true);
        btnApply.setBackground(AppTheme.PRIMARY);
        btnApply.setForeground(Color.WHITE);
        btnApply.setFocusPainted(false);
        btnApply.addActionListener(e -> {
            confirmed = true;
            dialog.dispose();
        });
        dialog.getRootPane().setDefaultButton(btnApply);

        JButton btnClearFilter = IconManager.getIconButton(IconManager.IconType.CROSS, "クリア");
        btnClearFilter.setToolTipText("フィルタをクリア");
        btnClearFilter.setMargin(new Insets(4, 16, 4, 16));
        btnClearFilter.setOpaque(true);
        btnClearFilter.setContentAreaFilled(true);
        btnClearFilter.setBackground(btnOutlineBg);
        btnClearFilter.setForeground(labelFg);
        btnClearFilter.setFocusPainted(false);
        btnClearFilter.addActionListener(e -> {
            if (clearAction != null) {
                clearAction.run();
            }
            txtKeyword.setText("");
            comboStatus.setSelectedItem("すべて");
        });

        footer.add(btnApply);
        footer.add(btnClearFilter);

        dialog.add(actionPanel, BorderLayout.NORTH);
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);

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
