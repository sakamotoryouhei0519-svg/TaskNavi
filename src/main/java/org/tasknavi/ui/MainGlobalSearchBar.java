package org.tasknavi.ui;

import org.tasknavi.AppMessages;
import org.tasknavi.AppTheme;
import org.tasknavi.SearchablePanel;
import org.tasknavi.Task;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

/**
 * MainFrame ツールバー右のグローバル検索・ステータスフィルタ。
 */
public final class MainGlobalSearchBar {

    private final Supplier<Component> activeViewSupplier;

    private JTextField txtGlobalSearch;
    private JButton btnClearSearch;
    private JButton btnGlobalFilter;
    private JPanel searchContainerPanel;
    private JLabel filterSummaryLabel;
    private String currentGlobalKeyword = null;
    private String currentGlobalStatus = AppMessages.FILTER_ALL;

    public MainGlobalSearchBar(Supplier<Component> activeViewSupplier) {
        this.activeViewSupplier = activeViewSupplier;
    }

    public JPanel searchContainer() {
        return searchContainerPanel;
    }

    public JTextField searchField() {
        return txtGlobalSearch;
    }

    public JButton filterButton() {
        return btnGlobalFilter;
    }

    public String keyword() {
        return currentGlobalKeyword;
    }

    public String status() {
        return currentGlobalStatus;
    }

    public JPanel createSearchBox() {
        JPanel box = new JPanel(new BorderLayout(4, 0));
        box.setBackground(AppTheme.PANEL_BG);
        box.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 7, 1, 1, 1, 1));
        box.setPreferredSize(new Dimension(240, 34));
        box.setMinimumSize(new Dimension(180, 34));
        searchContainerPanel = box;

        JLabel searchIconLabel = new JLabel("🔍");
        searchIconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        searchIconLabel.setForeground(AppTheme.TEXT_MUTED);
        searchIconLabel.setBorder(new EmptyBorder(0, 8, 0, 2));

        txtGlobalSearch = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !(isFocusOwner())) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    try {
                        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        g2.setColor(AppTheme.TEXT_MUTED);
                        g2.setFont(AppTheme.FONT_MAIN.deriveFont(Font.ITALIC, 12f));
                        FontMetrics fm = g2.getFontMetrics();
                        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                        g2.drawString(AppMessages.get("mainframe.search.placeholder"), 2, y);
                    } finally {
                        g2.dispose();
                    }
                }
            }
        };
        txtGlobalSearch.setFont(AppTheme.FONT_MAIN);
        txtGlobalSearch.setBorder(null);
        txtGlobalSearch.setOpaque(false);
        txtGlobalSearch.setForeground(AppTheme.TEXT_PRIMARY);
        txtGlobalSearch.setCaretColor(AppTheme.TEXT_PRIMARY);

        btnClearSearch = new JButton("✕");
        btnClearSearch.setFont(AppTheme.FONT_SMALL.deriveFont(Font.BOLD, 10f));
        btnClearSearch.setForeground(AppTheme.TEXT_MUTED);
        btnClearSearch.setOpaque(false);
        btnClearSearch.setContentAreaFilled(false);
        btnClearSearch.setBorderPainted(false);
        btnClearSearch.setFocusPainted(false);
        btnClearSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearSearch.setVisible(false);
        btnClearSearch.addActionListener(e -> {
            txtGlobalSearch.setText("");
            applyToActive();
            txtGlobalSearch.requestFocusInWindow();
        });

        txtGlobalSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onSearchTextChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onSearchTextChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onSearchTextChange();
            }

            private void onSearchTextChange() {
                btnClearSearch.setVisible(!txtGlobalSearch.getText().isEmpty());
                applyToActive();
            }
        });

        txtGlobalSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    txtGlobalSearch.setText("");
                    applyToActive();
                }
            }
        });

        box.add(searchIconLabel, BorderLayout.WEST);
        box.add(txtGlobalSearch, BorderLayout.CENTER);
        box.add(btnClearSearch, BorderLayout.EAST);
        return box;
    }

    public JButton createFilterButton() {
        JButton btn = new JButton(AppMessages.get("mainframe.button.filter"));
        btn.setFont(AppTheme.FONT_HEADER.deriveFont(12f));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(4, 12, 4, 12));
        btn.setPreferredSize(new Dimension(110, 34));
        btn.setMinimumSize(new Dimension(100, 34));
        updateFilterButtonStyle(btn);
        btn.addActionListener(e -> showFilterMenu(btn));
        btnGlobalFilter = btn;
        return btn;
    }

    public void applyToActive() {
        currentGlobalKeyword = txtGlobalSearch != null ? txtGlobalSearch.getText().trim() : null;
        if (currentGlobalKeyword != null && currentGlobalKeyword.isEmpty()) {
            currentGlobalKeyword = null;
        }
        Component selected = activeViewSupplier.get();
        if (selected instanceof SearchablePanel target) {
            target.applySearchFilter(currentGlobalKeyword, currentGlobalStatus);
        }
        refreshFilterSummary();
    }

    public void syncActive() {
        Component selected = activeViewSupplier.get();
        if (selected instanceof SearchablePanel target) {
            target.applySearchFilter(currentGlobalKeyword, currentGlobalStatus);
        }
        refreshFilterSummary();
    }

    public void updateTheme() {
        if (searchContainerPanel != null) {
            searchContainerPanel.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
            searchContainerPanel.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1));
        }
        if (txtGlobalSearch != null) {
            txtGlobalSearch.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
            txtGlobalSearch.setForeground(AppTheme.TEXT_PRIMARY);
            txtGlobalSearch.setCaretColor(AppTheme.TEXT_PRIMARY);
        }
        if (btnClearSearch != null) {
            btnClearSearch.setForeground(AppTheme.TEXT_MUTED);
        }
        if (btnGlobalFilter != null) {
            updateFilterButtonStyle(btnGlobalFilter);
        }
        if (filterSummaryLabel != null) {
            refreshFilterSummary();
        }
    }

    /** 適用中のキーワード／ステータスを短いラベルで返す（エクスポート確認など）。 */
    public String describeActiveFilters() {
        return describeFilters(currentGlobalKeyword, currentGlobalStatus);
    }

    public static String describeFilters(String keyword, String status) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasStatus = status != null && !AppMessages.isFilterAll(status);
        if (!hasKeyword && !hasStatus) {
            return AppMessages.get("mainframe.filter.summary.none", "絞り込みなし（全件）");
        }
        StringBuilder sb = new StringBuilder();
        if (hasKeyword) {
            sb.append(AppMessages.format("mainframe.filter.summary.keyword", "キーワード: {0}", keyword));
        }
        if (hasStatus) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(AppMessages.format(
                    "mainframe.filter.summary.status",
                    "状態: {0}",
                    AppMessages.statusDisplay(status)));
        }
        return sb.toString();
    }

    public JLabel createFilterSummaryLabel() {
        filterSummaryLabel = new JLabel(" ");
        filterSummaryLabel.setFont(AppTheme.FONT_SMALL.deriveFont(11f));
        filterSummaryLabel.setForeground(AppTheme.TEXT_MUTED);
        refreshFilterSummary();
        return filterSummaryLabel;
    }

    private void refreshFilterSummary() {
        if (filterSummaryLabel == null) {
            return;
        }
        String text = describeActiveFilters();
        boolean active = currentGlobalKeyword != null && !currentGlobalKeyword.isBlank()
                || (currentGlobalStatus != null && !AppMessages.isFilterAll(currentGlobalStatus));
        filterSummaryLabel.setText(active ? text : " ");
        filterSummaryLabel.setForeground(active
                ? (AppTheme.isDarkMode() ? new Color(96, 165, 250) : AppTheme.PRIMARY)
                : AppTheme.TEXT_MUTED);
        filterSummaryLabel.setToolTipText(active ? text : null);
    }

    private void updateFilterButtonStyle(JButton btn) {
        boolean isFilterActive = currentGlobalStatus != null && !AppMessages.isFilterAll(currentGlobalStatus);
        if (isFilterActive) {
            btn.setText(AppMessages.format(
                    "mainframe.button.filter.active",
                    "フィルター ({0}) ▼",
                    AppMessages.statusDisplay(currentGlobalStatus)));
            btn.setBackground(AppTheme.PRIMARY);
            btn.setForeground(Color.WHITE);
            btn.setBorder(AppTheme.createRoundedBorder(AppTheme.PRIMARY_DARK, 8, 2, 2, 2, 2));
        } else {
            btn.setText(AppMessages.get("mainframe.button.filter"));
            btn.setBackground(AppTheme.PANEL_BG);
            btn.setForeground(AppTheme.TEXT_PRIMARY);
            btn.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 2, 2, 2, 2));
        }
    }

    private void showFilterMenu(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(AppTheme.PANEL_BG);
        menu.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1));

        JLabel titleItem = new JLabel(" " + AppMessages.get("mainframe.filter.menu.status"));
        titleItem.setFont(AppTheme.FONT_SMALL.deriveFont(Font.BOLD, 10f));
        titleItem.setForeground(AppTheme.TEXT_MUTED);
        titleItem.setBorder(new EmptyBorder(6, 12, 4, 12));
        menu.add(titleItem);

        String[] statuses = {
                AppMessages.FILTER_ALL,
                Task.STATUS_NOT_STARTED,
                Task.STATUS_IN_PROGRESS,
                Task.STATUS_COMPLETED
        };
        ButtonGroup bg = new ButtonGroup();
        for (String status : statuses) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(
                    AppMessages.statusDisplay(status),
                    status.equals(currentGlobalStatus));
            item.setFont(AppTheme.FONT_MAIN.deriveFont(12f));
            item.setForeground(AppTheme.TEXT_PRIMARY);
            item.setBackground(AppTheme.PANEL_BG);
            item.setOpaque(true);
            bg.add(item);
            item.addActionListener(e -> {
                currentGlobalStatus = status;
                updateFilterButtonStyle(btnGlobalFilter);
                applyToActive();
            });
            menu.add(item);
        }

        menu.addSeparator();

        JMenuItem projectItem = new JMenuItem(AppMessages.get("mainframe.filter.menu.project"));
        projectItem.setFont(AppTheme.FONT_MAIN.deriveFont(12f));
        projectItem.setForeground(AppTheme.TEXT_PRIMARY);
        projectItem.setBackground(AppTheme.PANEL_BG);
        projectItem.setOpaque(true);
        projectItem.addActionListener(e -> {
            Component selected = activeViewSupplier.get();
            if (selected instanceof SearchablePanel target) {
                target.handleExistingProject();
            }
        });
        menu.add(projectItem);

        menu.addSeparator();

        JMenuItem clearItem = new JMenuItem(AppMessages.get("mainframe.filter.menu.clear"));
        clearItem.setFont(AppTheme.FONT_MAIN.deriveFont(12f));
        clearItem.setForeground(new Color(220, 38, 38));
        clearItem.setBackground(AppTheme.PANEL_BG);
        clearItem.setOpaque(true);
        clearItem.addActionListener(e -> {
            currentGlobalStatus = AppMessages.FILTER_ALL;
            if (txtGlobalSearch != null) {
                txtGlobalSearch.setText("");
            }
            updateFilterButtonStyle(btnGlobalFilter);
            Component selected = activeViewSupplier.get();
            if (selected instanceof SearchablePanel target) {
                target.handleClearFilter();
            }
            applyToActive();
            refreshFilterSummary();
        });
        menu.add(clearItem);

        menu.show(invoker, 0, invoker.getHeight() + 4);
    }
}
