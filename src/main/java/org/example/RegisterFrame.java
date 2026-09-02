package org.example;

// --- 画面 UI (Swing) 関連ライブラリ ---
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// --- レイアウト・イベント関連ライブラリ ---
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

/**
 * 【新規ユーザー登録画面クラス】
 * 新しいアカウント情報（ユーザー名、メールアドレス、パスワード）を入力し登録を行います。
 *
 * 【重要単語の解説】
 * - JTextField: 1行のテキスト入力欄
 * - JPasswordField: パスワード入力用（文字がマスクされる）テキスト欄
 * - BoxLayout: コンポーネントを縦または横に一列に配置するレイアウト
 *
 * 【コードの読み方】
 * 1. ウィンドウの基本設定（タイトル、サイズ、位置）
 * 2. メインパネルを作成してレイアウトを設定
 * 3. 各入力欄とボタンを作成して配置
 * 4. 登録処理を実装
 */
public class RegisterFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // ==========================================
    //  【コンポーネント保持用フィールド】
    // ==========================================
    private JTextField usernameField;        // 【重要】ユーザー名入力欄
    private JTextField emailField;           // 【重要】メールアドレス入力欄
    private JPasswordField passwordField;    // 【重要】パスワード入力欄
    private JPasswordField confirmPasswordField; // 【重要】パスワード確認用入力欄
    private JButton registerButton;          // 【重要】登録実行ボタン
    private JLabel errorLabel;               // 【重要】インラインエラー表示

    private static final int PASSWORD_FIELD_WIDTH = 320;

    /**
     * 【コンストラクタ】
     * 画面の基本設定およびGUIパーツの構築と配置を行います。
     *
     * 【重要単語の解説】
     * - setTitle(): ウィンドウのタイトルを設定
     * - setSize(): ウィンドウのサイズを設定
     * - setDefaultCloseOperation(): ウィンドウを閉じた時の動作を設定
     *
     * 【コードの読み方】
     * 1. ウィンドウの基本設定（タイトル、サイズ、閉じる動作、位置、リサイズ禁止）
     * 2. メインパネルを作成してレイアウトを設定
     * 3. 各入力欄とボタンを作成して配置
     */
    public RegisterFrame() {
        // ==========================================
        //  1. ウィンドウ全体の基本プロパティ設定
        // ==========================================
        // 【重要】setTitle(): タイトルバーに表示する文字列を設定
        setTitle("TaskNavi - 新規ユーザー登録");
        // 【重要】setSize(): 画面の幅と高さをピクセル単位で設定
        setSize(400, 520);
        // 【重要】setDefaultCloseOperation(): ×ボタンを押した時の動作
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 【重要】setLocationRelativeTo(): 画面表示位置をデスクトップの中央に自動配置
        setLocationRelativeTo(null);
        // 【重要】setResizable(): ユーザーによる画面リサイズを禁止
        setResizable(false);

        // ==========================================
        //  2. 土台となるメインパネルの構築
        // ==========================================
        JPanel mainPanel = new JPanel();
        // 【重要】BoxLayout.Y_AXIS: パーツを「上から下へ縦一列」に整列させるレイアウト
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        // 【重要】setBackground(): 統一背景色を設定
        mainPanel.setBackground(AppTheme.BACKGROUND);
        // 【重要】setBorder(): 上下25px、左右40pxの余白を設定
        mainPanel.setBorder(new EmptyBorder(25, 40, 25, 40));

        // --- 3. ヘッダーエリア（タイトル・サブタイトル） ---
        JLabel titleLabel = new JLabel("TaskNavi");
        titleLabel.setFont(AppTheme.FONT_TITLE);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("新規アカウントを作成します");
        subtitleLabel.setFont(AppTheme.FONT_MAIN);
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- 4. 各入力フォームの作成 ---
        JLabel userLabel = createFormLabel("ユーザー名");
        usernameField = createStyledTextField();

        JLabel emailLabel = createFormLabel("メールアドレス");
        emailField = createStyledTextField();

        JLabel passLabel = createFormLabel("パスワード");
        passwordField = createStyledPasswordField();
        JPanel passwordWrapper = createPasswordInputPanel(passwordField);

        JLabel confirmPassLabel = createFormLabel("パスワード (確認用)");
        confirmPasswordField = createStyledPasswordField();
        JPanel confirmPasswordWrapper = createPasswordInputPanel(confirmPasswordField);

        errorLabel = new JLabel();
        errorLabel.setFont(AppTheme.FONT_MAIN);
        errorLabel.setForeground(new Color(220, 53, 69));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setVisible(false);

        // --- 5. 登録ボタンの作成 ---
        registerButton = new JButton("アカウント登録");
        registerButton.setFont(AppTheme.FONT_HEADER);
        registerButton.setBackground(AppTheme.PRIMARY);          // メインカラー
        registerButton.setForeground(Color.WHITE);               // 文字色：白
        registerButton.setFocusPainted(false);                  // フォーカス枠消去
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); // 指マーク
        registerButton.setMaximumSize(new Dimension(320, 42));    // 固定サイズ
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- 6. ログイン画面への戻りリンク ---
        JLabel loginLink = new JLabel("<html><u>すでにアカウントをお持ちの方（ログイン）</u></html>");
        loginLink.setFont(AppTheme.FONT_MAIN);
        loginLink.setForeground(AppTheme.PRIMARY);
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLink.setAlignmentX(Component.CENTER_ALIGNMENT);

        // クリック時にログイン画面を開くイベント処理
        loginLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new LoginFrame().setVisible(true); // ログイン画面を起動
                dispose();                         // 登録画面を閉じる
            }
        });

        // --- 7. メインパネルへパーツを順次追加 ---
        mainPanel.add(titleLabel);
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(15));

        mainPanel.add(userLabel);
        mainPanel.add(Box.createVerticalStrut(2));
        mainPanel.add(usernameField);
        mainPanel.add(Box.createVerticalStrut(8));

        mainPanel.add(emailLabel);
        mainPanel.add(Box.createVerticalStrut(2));
        mainPanel.add(emailField);
        mainPanel.add(Box.createVerticalStrut(8));

        mainPanel.add(passLabel);
        mainPanel.add(Box.createVerticalStrut(2));
        mainPanel.add(passwordWrapper);
        mainPanel.add(Box.createVerticalStrut(8));

        mainPanel.add(confirmPassLabel);
        mainPanel.add(Box.createVerticalStrut(2));
        mainPanel.add(confirmPasswordWrapper);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(errorLabel);
        mainPanel.add(Box.createVerticalStrut(12));

        mainPanel.add(registerButton);
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(loginLink);

        add(mainPanel); // フレームへ配置

        // --- 8. イベント紐付け ---
        getRootPane().setDefaultButton(registerButton);             // Enterキーで実行
        registerButton.addActionListener(e -> handleRegister());    // ボタン押下時処理
    }

    /**
     * 【ヘルパーメソッド】フォーム用ラベル作成
     */
    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(AppTheme.FONT_BOLD);
        label.setForeground(AppTheme.TEXT_PRIMARY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setMaximumSize(new Dimension(320, 18));
        return label;
    }

    /**
     * 【ヘルパーメソッド】テキストフィールド生成
     */
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        applyFieldStyle(field);
        return field;
    }

    /**
     * 【ヘルパーメソッド】パスワードフィールド生成
     */
    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField();
        applyFieldStyle(field);
        return field;
    }

    private JPanel createPasswordInputPanel(JPasswordField field) {
        JPanel wrapper = new JPanel(new BorderLayout(6, 0));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(PASSWORD_FIELD_WIDTH, 34));

        JToggleButton toggleButton = new JToggleButton("🙈");
        toggleButton.setPreferredSize(new Dimension(UiConstants.ICON_BUTTON_SIZE_LARGE, UiConstants.FIELD_HEIGHT));
        toggleButton.setFocusPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_COLOR, 1));
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleButton.setToolTipText("パスワードを表示/非表示");
        toggleButton.addActionListener(e -> {
            if (toggleButton.isSelected()) {
                field.setEchoChar((char) 0);
                toggleButton.setText("🐵");
            } else {
                field.setEchoChar('•');
                toggleButton.setText("🙈");
            }
        });

        wrapper.add(field, BorderLayout.CENTER);
        wrapper.add(toggleButton, BorderLayout.EAST);
        return wrapper;
    }

    /**
     * 【スタイル共通適用メソッド】
     */
    private void applyFieldStyle(JTextField field) {
        field.setFont(AppTheme.FONT_MAIN);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setMaximumSize(new Dimension(320, 34));
        field.setBorder(AppTheme.createFieldBorder());

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(AppTheme.createFocusedFieldBorder());
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(AppTheme.createFieldBorder());
            }
        });
    }

    /**
     * 【登録処理実行ハンドラー】
     * 入力検証を行い、UserDao経由でデータベースへ登録します。
     */
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

       clearErrorState();

       String validationMessage = validateInput(username, email, password, confirmPassword);
       if (validationMessage != null) {
           showInlineError(validationMessage);
           return;
       }

       setRegisterButtonBusy(true);

       new SwingWorker<Boolean, Void>() {
           @Override
           protected Boolean doInBackground() {
               return UserDao.registerUser(username, password, email);
           }

           @Override
           protected void done() {
               try {
                   boolean success = get();
                   if (success) {
                       JOptionPane.showMessageDialog(RegisterFrame.this,
                               "ユーザー登録が完了しました！\nログイン画面へ戻ります。",
                               "登録成功",
                               JOptionPane.INFORMATION_MESSAGE);
                       new LoginFrame().setVisible(true);
                       RegisterFrame.this.dispose();
                   } else {
                       showInlineError("登録に失敗しました。時間をおいて再試行してください。");
                   }
               } catch (Exception ex) {
                   showInlineError("登録処理中にエラーが発生しました。時間をおいて再試行してください。");
                   org.example.util.Logger.error("ユーザー登録処理失敗", ex);
               } finally {
                   setRegisterButtonBusy(false);
               }
           }
       }.execute();
    }

    private String validateInput(String username, String email, String password, String confirmPassword) {
       if (username.isEmpty()) {
           highlightField(usernameField, true);
           return "ユーザー名を入力してください。";
       }

       if (email.isEmpty()) {
           highlightField(emailField, true);
           return "メールアドレスを入力してください。";
       }

       if (!EMAIL_PATTERN.matcher(email).matches()) {
           highlightField(emailField, true);
           return "メールアドレスの形式が正しくありません。";
       }

       if (password.isEmpty()) {
           highlightField(passwordField, true);
           return "パスワードを入力してください。";
       }

       if (!password.equals(confirmPassword)) {
           highlightField(passwordField, true);
           highlightField(confirmPasswordField, true);
           return "パスワードと確認用パスワードが一致しません。";
       }

       if (!isStrongPassword(password)) {
           highlightField(passwordField, true);
           highlightField(confirmPasswordField, true);
           return "パスワードは8文字以上で、英字・数字・記号を含めてください。";
       }

       if (UserDao.isUsernameExists(username)) {
           highlightField(usernameField, true);
           return "このユーザー名は既に使用されています。";
       }

       if (UserDao.isEmailRegistered(email)) {
           highlightField(emailField, true);
           return "このメールアドレスは既に登録されています。";
       }

       return null;
    }

    private boolean isStrongPassword(String password) {
       if (password == null || password.length() < 8) {
           return false;
       }

       boolean hasLetter = password.matches(".*[A-Za-z].*");
       boolean hasDigit = password.matches(".*[0-9].*");
       boolean hasSymbol = password.matches(".*[^A-Za-z0-9].*");
       return hasLetter && hasDigit && hasSymbol;
    }

    private void setRegisterButtonBusy(boolean busy) {
       registerButton.setEnabled(!busy);
       registerButton.setText(busy ? "登録中..." : "アカウント登録");
       registerButton.setCursor(busy ? Cursor.getDefaultCursor() : new Cursor(Cursor.HAND_CURSOR));
    }

    private void showInlineError(String message) {
       errorLabel.setText(message);
       errorLabel.setVisible(true);
    }

    private void clearErrorState() {
       errorLabel.setText("");
       errorLabel.setVisible(false);
       highlightField(usernameField, false);
       highlightField(emailField, false);
       highlightField(passwordField, false);
       highlightField(confirmPasswordField, false);
    }

    private void highlightField(JTextField field, boolean invalid) {
       if (field == null) {
           return;
       }
       field.setBorder(invalid ? BorderFactory.createLineBorder(new Color(220, 53, 69), 2) : AppTheme.createFieldBorder());
    }
}