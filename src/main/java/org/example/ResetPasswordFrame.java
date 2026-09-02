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

/**
 * 【パスワード再設定画面クラス】
 * 登録済みアカウントのユーザー名・メールアドレスの照合と、新しいパスワードの再設定を行います。
 */
public class ResetPasswordFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    // --- コンポーネント保持用フィールド ---
    private JTextField emailField;           // メールアドレス入力欄
    private JTextField authCodeField;        // 認証コード入力欄
    private JPasswordField newPasswordField; // 新しいパスワード入力欄
    private JButton sendCodeButton;          // 認証コード送信ボタン
    private JButton verifyButton;           // 認証コード検証ボタン
    private JButton resetButton;            // 再設定実行ボタン
    private JToggleButton showPasswordButton; // パスワード表示切替ボタン

    private String currentEmail = null;     // 現在処理中のメールアドレス
    private boolean emailVerified = false;   // メール認証完了フラグ

    /**
     * コンストラクタ：画面の基本設定およびGUIパーツの構築と配置を行います。
     */
    public ResetPasswordFrame() {
        // --- 1. ウィンドウ全体の基本プロパティ設定 ---
        setTitle("TaskNavi - パスワード再設定");
        setSize(400, 480);                             // 画面サイズ設定
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ×ボタンでアプリ終了
        setLocationRelativeTo(null);                   // 画面中央に配置
        setResizable(false);                           // リサイズ固定

        // --- 2. 土台となるメインパネルの構築 ---
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS)); // 縦一列に配置
        mainPanel.setBackground(AppTheme.BACKGROUND);   // 統一背景色（明るいグレー）
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40)); // 上下30px、左右40pxの内側余白

        // --- 3. ヘッダーエリア（タイトル・サブタイトル） ---
        JLabel titleLabel = new JLabel("TaskNavi");
        titleLabel.setFont(AppTheme.FONT_TITLE);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("新しいパスワードを設定します");
        subtitleLabel.setFont(AppTheme.FONT_MAIN);
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- 4. 各入力フォームの作成 ---
        JLabel emailLabel = createFormLabel("登録メールアドレス");
        emailField = createStyledTextField("example@domain.com");

        // 認証コード送信ボタン
        sendCodeButton = new JButton("認証コードを送信");
        sendCodeButton.setFont(AppTheme.FONT_HEADER);
        sendCodeButton.setBackground(AppTheme.PRIMARY);
        sendCodeButton.setForeground(Color.WHITE);
        sendCodeButton.setFocusPainted(false);
        sendCodeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendCodeButton.setMaximumSize(new Dimension(320, 36));
        sendCodeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sendCodeButton.addActionListener(e -> handleSendAuthCode());

        JLabel authCodeLabel = createFormLabel("認証コード");
        authCodeField = createStyledTextField("6桁のコードを入力");
        authCodeField.setEnabled(false); // 最初は無効

        // 認証コード検証ボタン
        verifyButton = new JButton("認証コードを確認");
        verifyButton.setFont(AppTheme.FONT_HEADER);
        verifyButton.setBackground(AppTheme.PRIMARY);
        verifyButton.setForeground(Color.WHITE);
        verifyButton.setFocusPainted(false);
        verifyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        verifyButton.setMaximumSize(new Dimension(320, 36));
        verifyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        verifyButton.setEnabled(false); // 最初は無効
        verifyButton.addActionListener(e -> handleVerifyAuthCode());

        JLabel passLabel = createFormLabel("新しいパスワード");
        JPanel passwordWrapper = new JPanel(new BorderLayout());
        passwordWrapper.setOpaque(false);
        passwordWrapper.setMaximumSize(new Dimension(320, 36));
        passwordWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        newPasswordField = createStyledPasswordField("新しいパスワードを入力");
        newPasswordField.setEnabled(false);

        showPasswordButton = new JToggleButton("🙈");
        showPasswordButton.setPreferredSize(new Dimension(40, 36));
        showPasswordButton.setFocusPainted(false);
        showPasswordButton.setContentAreaFilled(false);
        showPasswordButton.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_COLOR, 1));
        showPasswordButton.setToolTipText("パスワードを表示/非表示");
        showPasswordButton.addActionListener(e -> {
            if (showPasswordButton.isSelected()) {
                newPasswordField.setEchoChar((char) 0);
                showPasswordButton.setText("🐵");
            } else {
                newPasswordField.setEchoChar('•');
                showPasswordButton.setText("🙈");
            }
        });
        passwordWrapper.add(newPasswordField, BorderLayout.CENTER);
        passwordWrapper.add(showPasswordButton, BorderLayout.EAST);

        // --- 5. 再設定ボタンの作成 ---
        resetButton = new JButton("パスワードを更新");
        resetButton.setFont(AppTheme.FONT_HEADER);
        resetButton.setBackground(AppTheme.PRIMARY);          // メインカラー（ブルー）
        resetButton.setForeground(Color.WHITE);               // 文字色：白
        resetButton.setFocusPainted(false);                  // フォーカス枠消去
        resetButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); // カーソル：指マーク
        resetButton.setMaximumSize(new Dimension(320, 42));    // 固定サイズ
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetButton.setEnabled(false); // 最初は無効

        // --- 6. ログイン画面への戻りリンク ---
        JLabel loginLink = new JLabel("<html><u>ログイン画面に戻る</u></html>");
        loginLink.setFont(AppTheme.FONT_MAIN);
        loginLink.setForeground(AppTheme.PRIMARY);
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLink.setAlignmentX(Component.CENTER_ALIGNMENT);

        // クリック時にログイン画面を開くイベントハンドラー
        loginLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new LoginFrame().setVisible(true); // ログイン画面を起動
                dispose();                         // 自画面（再設定画面）を閉じる
            }
        });

        // --- 7. メインパネルへパーツを順次追加（縦並び・余白調整） ---
        mainPanel.add(titleLabel);
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(18)); // 固定高の余白スペース

        mainPanel.add(emailLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(emailField);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(sendCodeButton);
        mainPanel.add(Box.createVerticalStrut(12));

        mainPanel.add(authCodeLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(authCodeField);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(verifyButton);
        mainPanel.add(Box.createVerticalStrut(12));

        mainPanel.add(passLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(passwordWrapper);
        mainPanel.add(Box.createVerticalStrut(22));

        mainPanel.add(resetButton);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(loginLink);

        add(mainPanel); // メインパネルをフレームにセット

        // --- 8. ショートカットキー＆処理アクションの紐付け ---
        resetButton.addActionListener(e -> handleResetPassword()); // ボタン押下時の実行処理を設定
    }

    /**
     * 【ヘルパーメソッド】フォーム用ラベルの作成
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
     * 【ヘルパーメソッド】1行テキスト入力フィールドの生成とスタイル適用
     */
    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField();
        applyFieldStyle(field);
        return field;
    }

    /**
     * 【ヘルパーメソッド】パスワード入力フィールドの生成とスタイル適用
     */
    private JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        applyFieldStyle(field);
        return field;
    }

    /**
     * 【スタイル共通適用メソッド】文字の中央寄せ、サイズ指定、フォーカス枠線変更を付与
     */
    private void applyFieldStyle(JTextField field) {
        field.setFont(AppTheme.FONT_MAIN);
        field.setHorizontalAlignment(JTextField.CENTER);      // 入力文字を中央寄せ
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setMaximumSize(new Dimension(320, 36));           // 高さ36pxに固定

        field.setBorder(AppTheme.createFieldBorder());          // 通常時の枠線

        // フォーカス獲得/紛失時の枠線変更アニメーション設定
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(AppTheme.createFocusedFieldBorder()); // 青枠強調
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(AppTheme.createFieldBorder());        // 通常枠線に戻す
            }
        });
    }

    /**
     * 【認証コード送信ハンドラー】
     * メールアドレスを確認し、認証コードを送信します。
     */
    private void handleSendAuthCode() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "メールアドレスを入力してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!UserDao.isEmailRegistered(email)) {
            JOptionPane.showMessageDialog(this, "このメールアドレスは登録されていません。", "エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setSendCodeButtonBusy(true);
        emailField.setEnabled(false);
        String authCode = VerificationManager.generateCode();

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return EmailUtil.sendAuthCode(email, authCode);
            }

            @Override
            protected void done() {
                try {
                    boolean emailSent = get();
                    if (emailSent) {
                        currentEmail = email;
                        JOptionPane.showMessageDialog(ResetPasswordFrame.this, "認証コードを送信しました。\nメールをご確認ください。", "送信成功", JOptionPane.INFORMATION_MESSAGE);
                        authCodeField.setEnabled(true);
                        verifyButton.setEnabled(true);
                        sendCodeButton.setEnabled(false);
                        emailField.setEnabled(false);
                    } else {
                        JOptionPane.showMessageDialog(ResetPasswordFrame.this, "メールの送信に失敗しました。\n環境変数 TASKNAVI_EMAIL / TASKNAVI_EMAIL_PASSWORD を確認してください。", "送信失敗", JOptionPane.ERROR_MESSAGE);
                        setSendCodeButtonBusy(false);
                        authCodeField.setEnabled(false);
                        verifyButton.setEnabled(false);
                        emailField.setEnabled(true);
                        VerificationManager.clearCode();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ResetPasswordFrame.this, "メール送信処理中にエラーが発生しました。", "送信失敗", JOptionPane.ERROR_MESSAGE);
                    setSendCodeButtonBusy(false);
                    authCodeField.setEnabled(false);
                    verifyButton.setEnabled(false);
                    emailField.setEnabled(true);
                    VerificationManager.clearCode();
                }
            }
        };

        worker.execute();
    }

    /**
     * 【認証コード検証ハンドラー】
     * 入力された認証コードを検証します。
     */
    private void handleVerifyAuthCode() {
       String inputCode = authCodeField.getText().trim();

       if (inputCode.isEmpty()) {
           JOptionPane.showMessageDialog(this, "認証コードを入力してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
           return;
       }

       if (VerificationManager.verifyCode(inputCode)) {
           emailVerified = true;
           JOptionPane.showMessageDialog(this, "認証が完了しました。\n新しいパスワードを入力してください。", "認証成功", JOptionPane.INFORMATION_MESSAGE);

           newPasswordField.setEnabled(true);
           resetButton.setEnabled(true);
           verifyButton.setEnabled(false);
           authCodeField.setEnabled(false);
           VerificationManager.clearCode();
       } else {
           JOptionPane.showMessageDialog(this, "認証コードが正しくないか、有効期限が切れています。\nもう一度コードを送信してください。", "認証失敗", JOptionPane.ERROR_MESSAGE);
           resetWorkflowToInitialState();
       }
    }

    /**
     * 【パスワード再設定実行ハンドラー】
     * 新しいパスワードを更新します。
     */
    private void handleResetPassword() {
       String newPassword = new String(newPasswordField.getPassword()).trim();

       if (currentEmail == null || !emailVerified) {
           JOptionPane.showMessageDialog(this, "メール認証を完了してからパスワードを再設定してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
           return;
       }

       if (newPassword.isEmpty()) {
           JOptionPane.showMessageDialog(this, "新しいパスワードを入力してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
           return;
       }

       if (newPassword.length() < 6) {
           JOptionPane.showMessageDialog(this, "パスワードは6文字以上で入力してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
           return;
       }

       setResetButtonBusy(true);
       SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
           @Override
           protected Boolean doInBackground() {
               return UserDao.updatePasswordByEmail(currentEmail, newPassword);
           }

           @Override
           protected void done() {
               try {
                   boolean success = get();
                   if (success) {
                       JOptionPane.showMessageDialog(ResetPasswordFrame.this, "パスワードの再設定が完了しました！\n新しいパスワードでログインしてください。", "更新成功", JOptionPane.INFORMATION_MESSAGE);
                       new LoginFrame().setVisible(true);
                       ResetPasswordFrame.this.dispose();
                   } else {
                       JOptionPane.showMessageDialog(ResetPasswordFrame.this, "パスワードの更新に失敗しました。", "更新失敗", JOptionPane.ERROR_MESSAGE);
                   }
               } catch (Exception ex) {
                   JOptionPane.showMessageDialog(ResetPasswordFrame.this, "パスワード更新中にエラーが発生しました。", "更新失敗", JOptionPane.ERROR_MESSAGE);
               } finally {
                   setResetButtonBusy(false);
               }
           }
       };
       worker.execute();
    }

    private void setSendCodeButtonBusy(boolean busy) {
       sendCodeButton.setEnabled(!busy);
       sendCodeButton.setText(busy ? "送信中..." : "認証コードを送信");
    }

    private void setResetButtonBusy(boolean busy) {
       resetButton.setEnabled(!busy);
       resetButton.setText(busy ? "更新中..." : "パスワードを更新");
    }

    private void resetWorkflowToInitialState() {
       currentEmail = null;
       emailVerified = false;
       emailField.setEnabled(true);
       emailField.setText("");
       authCodeField.setText("");
       authCodeField.setEnabled(false);
       verifyButton.setEnabled(false);
       newPasswordField.setText("");
       newPasswordField.setEnabled(false);
       resetButton.setEnabled(false);
       setSendCodeButtonBusy(false);
       showPasswordButton.setSelected(false);
       newPasswordField.setEchoChar('•');
       showPasswordButton.setText("🙈");
    }
}