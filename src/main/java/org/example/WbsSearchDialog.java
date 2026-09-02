package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 【検索・フィルタ用のダイアログ】
 * WBS画面で「キーワード検索」や「状態フィルタ」を使うときに開く小さな入力画面です。
 *
 * 初心者向けのポイント:
 * - JDialog は「ダイアログ型のウィンドウ」を作るクラスです。
 * - WbsPanel から処理を委譲して、検索条件を入力してもらう役割を持ちます。
 * - owner を持つことで、検索結果を元の画面へ戻せます。
 */
public class WbsSearchDialog {
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
            return value.trim().isEmpty() ? null : value.trim();
        }
    }

    // フィールドはこのクラスで保持する値です。
    // owner: ここから呼び出された元の画面（WbsPanel）
    // parent: ダイアログを表示する親ウィンドウ
    // dialog: 実際に表示される JDialog オブジェクト
    private final WbsPanel owner;
    private final Frame parent;
    private final JDialog dialog;

    // 検索キーワード入力欄と状態選択欄
    private final JTextField txtKeyword = new JTextField(12);
    private final JComboBox<String> comboStatus = new JComboBox<>(new String[]{"すべて", "未着手", "進行中", "完了"});

    // 「検索ボタンが押されたか」を記録するフラグ
    private boolean confirmed = false;

    /**
     * 【コンストラクタ】
     * ダイアログの見た目とボタンの動作をここで組み立てます。
     *
     * 初心者向けの説明:
     * - JFrame や JDialog のような Swing の部品を作るときは、まず部品を生成して配置します。
     * - add() で画面上に部品を重ねることができます。
     * - ボタンの addActionListener は「クリックしたときに何をするか」を決めます。
     */
    public WbsSearchDialog(Frame parent, WbsPanel owner, String initialKeyword, String initialStatus) {
        this.parent = parent;
        this.owner = owner;
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

        JPanel searchPopupPanel = new JPanel(new GridBagLayout());
        searchPopupPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        searchPopupPanel.setBackground(panelBg);
        searchPopupPanel.setOpaque(true);
        GridBagConstraints searchGbc = new GridBagConstraints();
        searchGbc.insets = new Insets(8, 6, 12, 6);
        searchGbc.fill = GridBagConstraints.HORIZONTAL;

        searchGbc.gridx = 0; searchGbc.gridy = 0; searchGbc.weightx = 0;
        JLabel lblKeyword = new JLabel("検索:");
        lblKeyword.setFont(AppTheme.FONT_MAIN);
        lblKeyword.setForeground(labelFg);
        lblKeyword.setOpaque(false);
        searchPopupPanel.add(lblKeyword, searchGbc);
        searchGbc.gridx = 1; searchGbc.gridy = 0; searchGbc.weightx = 1.0;
        txtKeyword.setPreferredSize(new Dimension(UiConstants.FIELD_WIDTH_COMPACT, UiConstants.FIELD_HEIGHT_COMPACT));
        txtKeyword.setBackground(fieldBg);
        txtKeyword.setForeground(fieldFg);
        txtKeyword.setCaretColor(fieldFg);
        txtKeyword.setFont(AppTheme.FONT_MAIN);
        txtKeyword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        searchPopupPanel.add(txtKeyword, searchGbc);

        searchGbc.gridx = 0; searchGbc.gridy = 1; searchGbc.weightx = 0;
        JLabel lblStatus = new JLabel("状態:");
        lblStatus.setFont(AppTheme.FONT_MAIN);
        lblStatus.setForeground(labelFg);
        lblStatus.setOpaque(false);
        searchPopupPanel.add(lblStatus, searchGbc);
        searchGbc.gridx = 1; searchGbc.gridy = 1; searchGbc.weightx = 1.0;
        comboStatus.setPreferredSize(new Dimension(UiConstants.BUTTON_WIDTH_STANDARD, UiConstants.FIELD_HEIGHT_COMPACT));
        styleComboBox(comboStatus);
        searchPopupPanel.add(comboStatus, searchGbc);

        JPanel searchActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        searchActionPanel.setOpaque(true);
        searchActionPanel.setBackground(bgColor);
        searchActionPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

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
        btnProjectFilter.addActionListener(e -> owner.handleExistingProject());

        searchActionPanel.add(btnProjectFilter);

        JPanel searchFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchFooter.setOpaque(true);
        searchFooter.setBackground(bgColor);
        searchFooter.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        JButton btnApplySearch = new JButton("検索");
        btnApplySearch.setMargin(new Insets(4, 16, 4, 16));
        btnApplySearch.setOpaque(true);
        btnApplySearch.setContentAreaFilled(true);
        btnApplySearch.setBackground(AppTheme.PRIMARY);
        btnApplySearch.setForeground(Color.WHITE);
        btnApplySearch.setFocusPainted(false);
        dialog.getRootPane().setDefaultButton(btnApplySearch);
        btnApplySearch.addActionListener(e -> {
            confirmed = true;
            dialog.dispose();
        });
        JButton btnClearFilter = IconManager.getIconButton(IconManager.IconType.CROSS, "クリア");
        btnClearFilter.setToolTipText("フィルタをクリア");
        btnClearFilter.setMargin(new Insets(4, 16, 4, 16));
        btnClearFilter.setOpaque(true);
        btnClearFilter.setContentAreaFilled(true);
        btnClearFilter.setBackground(btnOutlineBg);
        btnClearFilter.setForeground(labelFg);
        btnClearFilter.setFocusPainted(false);
        btnClearFilter.addActionListener(e -> {
            owner.handleClearFilter();
            txtKeyword.setText("");
            comboStatus.setSelectedItem("すべて");
        });
        searchFooter.add(btnApplySearch);
        searchFooter.add(btnClearFilter);

        dialog.add(searchPopupPanel, BorderLayout.CENTER);
        dialog.add(searchActionPanel, BorderLayout.NORTH);
        dialog.add(searchFooter, BorderLayout.SOUTH);

        // 画面起動時の初期値を設定
        // 例: すでに入力済みの検索ワードがあれば、そのまま表示する
        if (initialKeyword != null) txtKeyword.setText(initialKeyword);
        if (initialStatus != null) comboStatus.setSelectedItem(initialStatus);

        // 閉じるときに検索が確定していない状態に戻す
        // これで「キャンセルした」ことをわかりやすく扱えます
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmed = false;
            }
        });
    }

    /**
     * 【ダイアログ表示】
     * このメソッドを呼ぶと検索画面がユーザーに見えるようになります。
     *
     * @return 検索ボタンが押されたかどうか
     */
    public boolean showDialog() {
        // ダイアログの表示は setVisible(true) で実行されます。
        // 画面が閉じるまでここで止まるイメージです。
        dialog.setVisible(true);
        return confirmed;
    }

    public SearchCriteria getCriteria() {
        return SearchCriteria.of(txtKeyword.getText(), (String) comboStatus.getSelectedItem());
    }

    /**
     * 【検索キーワード取得】
     * 入力が空なら null を返し、検索条件がない状態を表します。
     */
    public String getKeyword() {
        return getCriteria().getKeyword();
    }

    /**
     * 【状態選択の取得】
     * 例: "未着手" や "完了" を返します。
     */
    public String getStatus() {
        return getCriteria().getStatus();
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
