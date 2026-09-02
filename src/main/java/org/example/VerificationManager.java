package org.example;

import java.security.SecureRandom;

/**
 * 【メール認証コードの管理クラス】
 * このクラスは、ログインやユーザー登録時に使う 6 桁の認証コードを
 * 生成し、照合し、最後に破棄する役割を担います。
 *
 * 実際のアプリでは「今どのコードが有効か」を変数として保持しておく必要があり、
 * その状態管理をこのクラスでまとめている構造です。
 */
public class VerificationManager {
    private static final Object SESSION_LOCK = new Object();
    private static final long CODE_VALIDITY_MILLIS = 5 * 60 * 1000L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // 現在有効な 6 桁コード。未発行時は null です。
    private static volatile String currentCode = null;
    private static volatile long generatedAtMillis = -1L;

    private VerificationManager() {
       throw new AssertionError("Utility class");
    }

    /**
     * 【認証コードを発行する】
     * 100000～999999 の範囲で乱数を生成し、文字列に変換して保持します。
     *
     * @return 生成された 6 桁の認証コード
     */
    public static String generateCode() {
       synchronized (SESSION_LOCK) {
           int code = 100000 + SECURE_RANDOM.nextInt(900000);
           currentCode = String.valueOf(code);
           generatedAtMillis = System.currentTimeMillis();
           return currentCode;
       }
    }

    /**
     * 【入力コードが正しいかを検証する】
     * 画面で入力されたコードと、現在保持している認証コードを比較します。
     *
     * @param inputCode ユーザーが入力したコード
     * @return 一致していたら true
     */
    public static boolean verifyCode(String inputCode) {
       synchronized (SESSION_LOCK) {
           if (currentCode == null || inputCode == null || inputCode.trim().isEmpty()) {
               return false;
           }

           if (System.currentTimeMillis() - generatedAtMillis > CODE_VALIDITY_MILLIS) {
               clearCode();
               return false;
           }

           return currentCode.equals(inputCode.trim());
       }
    }

    /**
     * 【認証コードをクリアする】
     * 認証完了後やキャンセル時に使い、古いコードを破棄します。
     */
    public static void clearCode() {
       synchronized (SESSION_LOCK) {
           currentCode = null;
           generatedAtMillis = -1L;
       }
    }
}