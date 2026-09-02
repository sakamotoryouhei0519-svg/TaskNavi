package org.example;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

/**
 * 【ログイン画面クラス】
 * JFrameを継承し、ログイン画面のウインドウ描画・入力制御・イベントハンドリングを行います。
 *
 * 【重要単語の解説】
 * - JFrame: Swingのメインウィンドウを表すクラス
 * - JTextField: 1行のテキスト入力欄
 * - JPasswordField: パスワード入力用（文字がマスクされる）テキスト欄
 * - JToggleButton: ON/OFFを切り替えるボタン
 * - BoxLayout: コンポーネントを縦または横に一列に配置するレイアウト
 *
 * 【コードの読み方】
 * 1. ウィンドウの基本設定（タイトル、サイズ、位置）
 * 2. メインパネルを作成してレイアウトを設定
 * 3. 各コンポーネント（ラベル、入力欄、ボタン）を作成
 * 4. コンポーネントをパネルに配置
 * 5. イベントリスナーを登録
 */
public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String PREF_LAST_USERNAME = "tasknavi.last.username";

    // ==========================================
    //  【GUIコンポーネント（画面パーツ）の保持フィールド】
    // ==========================================
    // 【重要】クラス内の各種メソッドからアクセスできるようにメンバー変数として宣言
    private JTextField usernameField;         // 【重要】ユーザー名を入力する1行テキストボックス
    private JPasswordField passwordField;     // 【重要】パスワード入力用（入力文字がマスクされるテキストボックス）
    private JButton loginButton;              // 【重要】ログイン処理を実行する押下用ボタン
    private JToggleButton showPasswordButton; // 【重要】パスワードのマスク表示/非表示を切り替えるトグルボタン
    private JLabel errorLabel;                // 【重要】ログイン失敗時のインラインエラー表示
    private JCheckBox rememberMeCheckBox;     // 【重要】ログイン情報を次回も記憶するかどうかの選択

    /**
     * 【コンストラクタ】
     * 画面起動時にインスタンスが生成される際、初期設定と画面構築を実行します。
     *
     * 【重要単語の解説】
     * - setTitle(): ウィンドウのタイトルを設定
     * - setSize(): ウィンドウのサイズを設定
     * - setDefaultCloseOperation(): ウィンドウを閉じた時の動作を設定
     * - setLocationRelativeTo(): ウィンドウの位置を設定
     * - setResizable(): ウィンドウのリサイズ可否を設定
     *
     * 【コードの読み方】
     * 1. ウィンドウの基本設定（タイトル、サイズ、閉じる動作、位置、リサイズ禁止）
     * 2. メインパネルを作成してレイアウトを設定
     * 3. 各コンポーネントを作成して配置
     */
    public LoginFrame() {
        // ==========================================
        //  1. ウィンドウ全体の基本プロパティ設定
        // ==========================================
        // 【重要】setTitle(): タイトルバーに表示する文字列を設定
        setTitle("TaskNavi - ログイン");
        // 【重要】setSize(): 画面の幅と高さをピクセル単位で設定
        setSize(UiConstants.WINDOW_WIDTH_LOGIN, UiConstants.WINDOW_HEIGHT_LOGIN);
        // 【重要】setDefaultCloseOperation(): ×ボタンを押した時の動作
        // EXIT_ON_CLOSE: ウィンドウを閉じるとアプリケーションも終了
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 【重要】setLocationRelativeTo(): 画面表示位置をデスクトップの中央に自動配置
        setLocationRelativeTo(null);
        // 【重要】setResizable(): ユーザーによる画面リサイズを禁止（レイアウト崩れ防止）
        setResizable(false);

        // ==========================================
        //  2. 土台となるメインパネルの構築
        // ==========================================
        JPanel mainPanel = new JPanel();
        // 【重要】BoxLayout.Y_AXIS: パーツを「上から下へ縦一列」に整列させるレイアウト
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        // 【重要】setBackground(): テーマ定義の背景色（明るいグレー）を設定
        mainPanel.setBackground(AppTheme.BACKGROUND);
        // 【重要】setBorder(): パネルの内側に余白を設定（上30, 右40, 下30, 左40px）
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        // ==========================================
        //  3. ヘッダーエリア（アプリタイトル・サブタイトル）の作成
        // ==========================================
        JLabel titleLabel = new JLabel("TaskNavi");
        // 【重要】setFont(): 見出し用フォント（太字・大）を適用
        titleLabel.setFont(AppTheme.FONT_TITLE);
        // 【重要】setForeground(): テキストカラーを設定
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        // 【重要】setAlignmentX(): BoxLayout内で水平中央に配置するフラグを設定
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("アカウントにログインしてください");
        // 【重要】標準フォントを適用
        subtitleLabel.setFont(AppTheme.FONT_MAIN);
        // 【重要】補足用の少し薄い文字色に設定
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- 4. ユーザー名入力フォームの作成 ---
        JLabel userLabel = createFormLabel("ユーザー名");
        usernameField = createStyledTextField("ユーザー名を入力");

        // --- 5. パスワード入力フォーム（トグルボタン付き）の作成 ---
        JLabel passLabel = createFormLabel("パスワード");

        // パスワード入力欄と右側の目隠しアイコンボタンを横並びにするためのレイアウト用透明パネル
        JPanel passwordWrapper = new JPanel(new BorderLayout()); // 東西南北中央で配置するBorderLayout
        passwordWrapper.setOpaque(false);                       // 背景を透明にして親パネルの背景に馴染ませる
        passwordWrapper.setMaximumSize(new Dimension(UiConstants.FIELD_WIDTH_STANDARD, UiConstants.FIELD_HEIGHT));  // 横幅320px、高さ38pxに固定
        passwordWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordField = new JPasswordField();
        styleTextField(passwordField, "パスワードを入力");       // 共通スタイル（枠線・フォーカス処理）を適用

        // パスワード表示/非表示を切り替えるトグルボタンの生成
        showPasswordButton = new JToggleButton("🙈");
        showPasswordButton.setPreferredSize(new Dimension(UiConstants.ICON_BUTTON_SIZE_LARGE, UiConstants.FIELD_HEIGHT)); // 横幅40px、高さ38pxに設定
        showPasswordButton.setFocusPainted(false);                 // 選択時の点線枠表示を無効化
        showPasswordButton.setContentAreaFilled(false);            // 背景の塗りを透過
        showPasswordButton.setBorder(new LineBorder(AppTheme.BORDER_COLOR, 1)); // 1pxの枠線を付与
        showPasswordButton.setToolTipText("パスワードを表示/非表示");  // マウスホバー時のツールチップテキスト

        // トグルボタンがクリックされた時のイベントリスナー（動作定義）
        showPasswordButton.addActionListener(e -> {
            // isSelected() はボタンが押し込まれている（ON）のときに true になります
            if (showPasswordButton.isSelected()) {
                passwordField.setEchoChar((char) 0); // マスク文字を解除し、入力文字列を可視化
                showPasswordButton.setText("🐵");     // ボタンアイコンを「開いた目」に変更
            } else {
                passwordField.setEchoChar('•');     // マスク文字を黒丸『•』に設定して非表示化
                showPasswordButton.setText("🙈");     // ボタンアイコンを「閉じた目」に変更
            }
        });

        // ラッパーパネルへのパーツ組み込み（中央に入力欄、右端に切り替えボタン）
        passwordWrapper.add(passwordField, BorderLayout.CENTER);
        passwordWrapper.add(showPasswordButton, BorderLayout.EAST);

        // --- 6. エラー表示とログイン状態保持の設定 ---
        errorLabel = new JLabel();
        errorLabel.setForeground(new Color(220, 53, 69));
        errorLabel.setFont(AppTheme.FONT_MAIN);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setVisible(false);

        rememberMeCheckBox = new JCheckBox("次回もユーザー名を保持する");
        rememberMeCheckBox.setFont(AppTheme.FONT_MAIN);
        rememberMeCheckBox.setOpaque(false);
        rememberMeCheckBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        rememberMeCheckBox.setFocusPainted(false);

        // --- 7. ログインボタンの作成 ---
        loginButton = new JButton("ログイン");
        loginButton.setFont(AppTheme.FONT_HEADER);
        loginButton.setBackground(AppTheme.PRIMARY);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setMaximumSize(new Dimension(UiConstants.FIELD_WIDTH_STANDARD, UiConstants.BUTTON_HEIGHT_LARGE));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- 8. 下部テキストリンク（画面遷移用）の作成 ---
        // 新規登録用リンク
        JLabel registerLink = createLinkButton("<html><u>アカウントをお持ちでない方はこちら（新規登録）</u></html>");
        registerLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new RegisterFrame().setVisible(true); // 新規登録画面をインスタンス化して表示
                dispose();                             // 現在のログイン画面を破棄して閉じる
            }
        });

        // パスワード再設定用リンク
        JLabel forgotPasswordLink = createLinkButton("<html><u>パスワードをお忘れの方はこちら</u></html>");
        forgotPasswordLink.setForeground(AppTheme.TEXT_MUTED); // 控えめな色に変更
        forgotPasswordLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new ResetPasswordFrame().setVisible(true); // パスワード再設定画面を表示
                dispose();                                 // 現在の画面を破棄
            }
        });

        // --- 8. コンポーネントをメインパネルへ順次追加（縦並び配置） ---
        // Box.createVerticalStrut(px) は要素間の垂直方向の隙間（スペース）を作成します
        mainPanel.add(titleLabel);
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(20)); // 20pxの余白

        mainPanel.add(userLabel);
        mainPanel.add(Box.createVerticalStrut(4));  // 4pxの余白
        mainPanel.add(usernameField);
        mainPanel.add(Box.createVerticalStrut(12)); // 12pxの余白

        mainPanel.add(passLabel);
        mainPanel.add(Box.createVerticalStrut(4));  // 4pxの余白
        mainPanel.add(passwordWrapper);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(rememberMeCheckBox);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(errorLabel);
        mainPanel.add(Box.createVerticalStrut(12));

        mainPanel.add(loginButton);
        mainPanel.add(Box.createVerticalStrut(15)); // 15pxの余白
        mainPanel.add(registerLink);
        mainPanel.add(Box.createVerticalStrut(8));  // 8pxの余白
        mainPanel.add(forgotPasswordLink);

        add(mainPanel);

        String savedUsername = loadSavedUsername();
        if (!savedUsername.isEmpty()) {
            usernameField.setText(savedUsername);
            rememberMeCheckBox.setSelected(true);
        }

        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(e -> handleLogin());
        usernameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { clearInlineError(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { clearInlineError(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { clearInlineError(); }
        });
        passwordField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { clearInlineError(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { clearInlineError(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { clearInlineError(); }
        });
    }

    /**
     * 【ヘルパーメソッド】入力フォームの見出しラベルを生成します。
     * @param text 表示テキスト
     * @return スタイル設定済みのJLabel
     */
    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER); // テキストを中央寄せ
        label.setFont(AppTheme.FONT_BOLD);
        label.setForeground(AppTheme.TEXT_PRIMARY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setMaximumSize(new Dimension(UiConstants.FIELD_WIDTH_STANDARD, 20));           // 横幅320px、高さ20pxに固定
        return label;
    }

    /**
     * 【ヘルパーメソッド】ハイパーリンク風のテキストラベルを生成します。
     * @param text HTML形式のアンダーライン付きテキスト
     * @return クリック可能なJLabel
     */
    private JLabel createLinkButton(String text) {
        JLabel link = new JLabel(text);
        link.setFont(AppTheme.FONT_MAIN);
        link.setForeground(AppTheme.PRIMARY);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));        // マウスホバー時にカーソルを指マークに変化
        link.setAlignmentX(Component.CENTER_ALIGNMENT);
        return link;
    }

    /**
     * 【ヘルパーメソッド】スタイル付き1行入力フィールドを生成します。
     */
    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField();
        styleTextField(field, placeholder);
        return field;
    }

    /**
     * 【共通スタイル適用メソッド】
     * 入力フィールドの中央配置、枠線、フォーカスアニメーション（色変化）を付与します。
     */
    private void styleTextField(JTextField field, String placeholder) {
        field.setFont(AppTheme.FONT_MAIN);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setMaximumSize(new Dimension(UiConstants.FIELD_WIDTH_STANDARD, UiConstants.FIELD_HEIGHT));
        field.setBorder(createDefaultBorder());
        field.putClientProperty("errorState", false);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (Boolean.TRUE.equals(field.getClientProperty("errorState"))) {
                    field.setBorder(createErrorBorder());
                } else {
                    field.setBorder(AppTheme.createFocusedFieldBorder());
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (Boolean.TRUE.equals(field.getClientProperty("errorState"))) {
                    field.setBorder(createErrorBorder());
                } else {
                    field.setBorder(createDefaultBorder());
                }
            }
        });
    }

    private Border createDefaultBorder() {
        return new CompoundBorder(new LineBorder(AppTheme.BORDER_COLOR, 1, true), new EmptyBorder(7, 12, 7, 12));
    }

    private Border createErrorBorder() {
        return new CompoundBorder(new LineBorder(new Color(220, 53, 69), 2, true), new EmptyBorder(6, 11, 6, 11));
    }

    private void clearInlineError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        setFieldErrorState(usernameField, false);
        setFieldErrorState(passwordField, false);
    }

    private void setFieldErrorState(JTextField field, boolean hasError) {
        if (field == null) {
            return;
        }
        field.putClientProperty("errorState", hasError);
        field.setBorder(hasError ? createErrorBorder() : (field.hasFocus() ? AppTheme.createFocusedFieldBorder() : createDefaultBorder()));
    }

    private void showInlineError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        setFieldErrorState(usernameField, usernameField.getText().trim().isEmpty());
        setFieldErrorState(passwordField, passwordField.getPassword().length == 0);
    }

    private void setLoginInProgress(boolean inProgress) {
        loginButton.setEnabled(!inProgress);
        loginButton.setText(inProgress ? "認証中..." : "ログイン");
        loginButton.setBackground(inProgress ? new Color(132, 148, 166) : AppTheme.PRIMARY);
    }

    private String loadSavedUsername() {
        Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);
        return prefs.get(PREF_LAST_USERNAME, "");
    }

    private void saveRememberedUsername(String username, boolean enabled) {
        Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);
        if (enabled && username != null && !username.isBlank()) {
            prefs.put(PREF_LAST_USERNAME, username);
        } else {
            prefs.remove(PREF_LAST_USERNAME);
        }
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        clearInlineError();

        boolean hasUsernameError = username.isEmpty();
        boolean hasPasswordError = password.isEmpty();
        if (hasUsernameError || hasPasswordError) {
            if (hasUsernameError) {
                setFieldErrorState(usernameField, true);
            }
            if (hasPasswordError) {
                setFieldErrorState(passwordField, true);
            }
            showInlineError("ユーザー名とパスワードを入力してください。");
            if (hasUsernameError) {
                usernameField.requestFocusInWindow();
            } else {
                passwordField.requestFocusInWindow();
            }
            return;
        }

        setLoginInProgress(true);

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() {
                return AuthService.authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    User authenticatedUser = get();
                    setLoginInProgress(false);

                    if (authenticatedUser != null) {
                        saveRememberedUsername(username, rememberMeCheckBox.isSelected());
                        UserSession.login(authenticatedUser);
                        MainFrame mainFrame = new MainFrame();
                        mainFrame.setVisible(true);
                        LoginFrame.this.dispose();
                    } else {
                        setFieldErrorState(usernameField, true);
                        setFieldErrorState(passwordField, true);
                        showInlineError("ユーザー名またはパスワードが正しくありません。");
                        passwordField.selectAll();
                        passwordField.requestFocusInWindow();
                    }
                } catch (Exception ex) {
                    setLoginInProgress(false);
                    setFieldErrorState(usernameField, true);
                    setFieldErrorState(passwordField, true);
                    ErrorDialogUtil.showError(LoginFrame.this, ex);
                    showInlineError("ログイン処理中にエラーが発生しました。");
                }
            }
        };

        worker.execute();
    }
}