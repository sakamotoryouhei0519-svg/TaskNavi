package org.example;

// --- 暗号化・セキュリティ関連ライブラリ ---

import org.example.util.DatabaseUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// --- データベース接続（JDBC）関連ライブラリ ---
// 独自作成の DB 接続ユーティリティ

/**
 * 【ユーザーデータアクセスオブジェクト (DAO)】
 * データベースの users テーブルに対する操作（登録、認証、検証、更新）を集約するクラスです。
 *
 * 【重要単語の解説】
 * - DAO (Data Access Object): データベース操作を抽象化するデザインパターン
 * - BCrypt: パスワードを安全にハッシュ化するライブラリ
 * - PreparedStatement: SQLインジェクション対策済みのSQL実行クラス
 * - try-with-resources: リソースを自動的にクローズする構文
 *
 * 【コードの読み方】
 * 1. パスワードをハッシュ化してセキュリティを強化
 * 2. SQL文を準備してデータベースに接続
 * 3. パラメータをセットしてSQLを実行
 * 4. 結果を返す
 */
public class UserDao {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserDao.class);
    /**
     * 【新規ユーザー登録】
     * パスワードを BCrypt でハッシュ化してデータベースに保存します。
     *
     * 【重要単語の解説】
     * - ハッシュ化: パスワードを不可逆な文字列に変換する処理
     * - ソルト: ハッシュ化に使用するランダムな文字列（セキュリティ強化）
     * - プレースホルダー (?): SQL文で値を後から埋めるための記号
     *
     * 【コードの読み方】
     * 1. パスワードをハッシュ化
     * 2. SQL文を準備
     * 3. データベースに接続
     * 4. パラメータをセット
     * 5. SQLを実行
     *
     * @param username 登録するユーザー名
     * @param password 平文のパスワード
     * @param email    メールアドレス
     * @return 登録成功なら true
     */
    public  boolean registerUser(String username, String password, String email) {
        // ==========================================
        //  1. パスワードのハッシュ化
        // ==========================================
        // 【重要】BCrypt.hashpw(): パスワードをハッシュ化するメソッド
        // 【重要】BCrypt.gensalt(): ソルト（ランダムな文字列）を生成
        // 【コードの読み方】
        // - 平文パスワードをハッシュ化して安全に保存
        // - ソルトを自動生成してセキュリティを強化
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // ==========================================
        //  2. SQL文の定義
        // ==========================================
        // 【重要】INSERT INTO: テーブルにデータを挿入するSQL
        // 【重要】?: プレースホルダー（後から値を埋める）
        // 【コードの読み方】
        // - usersテーブルにusername, password, email, roleを挿入
        // - roleは固定で'user'を設定
        String sql = "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)";

        // ==========================================
        //  3. データベース接続とSQL実行
        // ==========================================
        // 【重要】try-with-resources: 接続・ステートメントを自動的にクローズ
        // 【重要】Connection: データベース接続を表すクラス
        // 【重要】PreparedStatement: SQLインジェクション対策済みのSQL実行クラス
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 【重要】setString(): プレースホルダーに値をセット（1始まり）
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, email);
            pstmt.setString(4, UserRole.USER.getValue());

            // 【重要】executeUpdate(): INSERT/UPDATE/DELETEを実行するメソッド
            // 【重要】戻り値: 影響を受けた行数
            // 【コードの読み方】
            // - SQLを実行して、1行以上挿入されれば成功
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.error("ユーザー登録エラー", e);
            return false;
        }
    }

    /**
     * 【ユーザー認証（ログイン）】
     * 入力されたパスワードとデータベースのハッシュ化パスワードを照合します。
     *
     * 【重要単語の解説】
     * - BCrypt.checkpw(): パスワードとハッシュを照合するメソッド
     * - SELECT: データベースからデータを取得するSQL
     *
     * 【コードの読み方】
     * 1. ユーザー名でデータベースを検索
     * 2. ハッシュ化パスワードを取得
     * 3. 入力パスワードと照合
     * 4. 結果を返す
     *
     * @param username ユーザー名
     * @param password 平文のパスワード
     * @return 認証成功なら true
     */
    public  boolean authenticate(String username, String password) {
        // 【重要】SELECT: データベースからデータを取得するSQL
        String sql = "SELECT password FROM users WHERE username = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            // クエリを実行して結果セットを取得（ResultSet も自動クローズ）
            try (ResultSet rs = pstmt.executeQuery()) {
                // 該当するユーザーが存在する場合
                if (rs.next()) {
                    String storedHash = rs.getString("password");

                    // BCryptハッシュのフォーマット（$2a$, $2b$等）かチェック
                    if (storedHash != null && storedHash.startsWith("$2")) {
                        // 平文パスワードとハッシュ値を照合
                        return BCrypt.checkpw(password, storedHash);
                    }
                    // 平文保存は受け入れない（レガシー互換を廃止）
                    logger.warn("非BCryptパスワードを検出したため認証を拒否しました: username={}", username);
                }
            }
        } catch (SQLException e) {
            logger.error("認証エラー", e);
        }
        return false;
    }

    /**
     * 【パスワード再設定（メール認証コード使用）】
     * メールアドレスと認証コードで本人確認を行い、新しいパスワードを設定します。
     *
     * @param email メールアドレス
     * @param verificationCode メールで送信された認証コード
     * @param newPassword 新しいパスワード（平文）
     * @return 更新成功なら true
     */
    public  boolean resetPasswordWithVerification(String email, String verificationCode, String newPassword) {
        if (email == null || verificationCode == null || newPassword == null) {
            return false;
        }
        if (!VerificationManager.verifyCode(email, verificationCode)) {
            return false;
        }

        try {
            return DatabaseUtil.withTransaction(connection -> {
                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                String sql = "UPDATE users SET password = ? WHERE email = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, hashedPassword);
                    pstmt.setString(2, email);
                    boolean success = pstmt.executeUpdate() > 0;
                    if (success) {
                        VerificationManager.clearCode(email);
                    }
                    return success;
                }
            });
        } catch (SQLException e) {
            logger.error("パスワードリセットエラー", e);
            return false;
        }
    }

    public  User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT id, username, email, role FROM users WHERE username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("username"),
                            null
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("ユーザー取得エラー", e);
        }
        return null;
    }

    /**
     * 【ユーザー名重複チェック】
     */
    public  boolean isUsernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                // レコードが1件でも見つかれば true（存在している）
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 【メールアドレス存在チェック】
     */
    public  boolean isEmailRegistered(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

}