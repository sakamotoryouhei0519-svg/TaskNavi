// packageとは、このファイルがどのグループ（パッケージ）に属しているかを宣言するもの
package org.example;

// public class AppException extends RuntimeExceptionとは、AppExceptionというクラスを宣言していること
// extends RuntimeExceptionとは、RuntimeException（実行時例外）を継承すること
// RuntimeExceptionとは、プログラム実行中に発生する例外（エラー）の種類
public class AppException extends RuntimeException {
    // ==========================================
    //  【エラーコード列挙型】
    // ==========================================
    // 【改善点】
    // エラーの種類をコードで管理できるようにしました。
    // これにより、エラーの分類やハンドリングが容易になります。
    // 
    // 【初心者向け解説】
    // Enum（列挙型）とは、決まった値の集合を定義する型です。
    // 例えば、「曜日」は月・火・水・木・金・土・日の7種類しかないので、
    // Enumで定義すると、間違った値（「木曜日」を「木」ではなく「Wood」と書くなど）を防げます。
    // 同様に、エラーコードも決まった種類しかないので、Enumで管理するのが適切です。
    public enum ErrorCode {
        // データベース関連のエラー
        DATABASE_CONNECTION_FAILED("DB001", "データベース接続に失敗しました"),
        DATABASE_QUERY_FAILED("DB002", "データベースクエリの実行に失敗しました"),
        DATABASE_TRANSACTION_FAILED("DB003", "データベーストランザクションに失敗しました"),

        // 認証・認可関連のエラー
        AUTHENTICATION_FAILED("AUTH001", "認証に失敗しました"),
        AUTHORIZATION_FAILED("AUTH002", "権限がありません"),
        TOKEN_INVALID("AUTH003", "トークンが無効です"),

        // バリデーション関連のエラー
        VALIDATION_FAILED("VAL001", "入力値の検証に失敗しました"),
        INVALID_INPUT("VAL002", "無効な入力値です"),
        REQUIRED_FIELD_MISSING("VAL003", "必須項目が入力されていません"),
        INVALID_DATE_RANGE("VAL004", "日付の範囲が不正です"),

        // ビジネスロジック関連のエラー
        BUSINESS_RULE_VIOLATION("BIZ001", "ビジネスルール違反です"),
        DUPLICATE_RESOURCE("BIZ002", "重複するリソースが存在します"),
        RESOURCE_NOT_FOUND("BIZ003", "リソースが見つかりません"),
        DATA_INTEGRITY_ERROR("BIZ004", "データの整合性が破損しています"),

        // ファイルI/O関連のエラー
        FILE_NOT_FOUND("FILE001", "ファイルが見つかりません"),
        FILE_READ_FAILED("FILE002", "ファイルの読み込みに失敗しました"),
        FILE_WRITE_FAILED("FILE003", "ファイルの書き込みに失敗しました"),

        // 取り込み・移行関連のエラー
        IMPORT_FAILED("IMP001", "データの取り込みに失敗しました"),
        MIGRATION_FAILED("IMP002", "データベース移行に失敗しました"),

        // その他のエラー
        UNKNOWN_ERROR("ERR999", "不明なエラーが発生しました");

        private final String code;
        private final String defaultMessage;

        ErrorCode(String code, String defaultMessage) {
            this.code = code;
            this.defaultMessage = defaultMessage;
        }

        public String getCode() {
            return code;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

    private final ErrorCode errorCode;
    private final String userMessage;

    public AppException(String message) {
        super(message);
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
        this.userMessage = resolveUserMessage(this.errorCode, null);
    }

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null, null);
    }

    public AppException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public AppException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause);
    }

    public AppException(ErrorCode errorCode, String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = resolveUserMessage(errorCode, userMessage);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
        this.userMessage = resolveUserMessage(this.errorCode, null);
    }

    private static String resolveUserMessage(ErrorCode errorCode, String explicitUserMessage) {
        if (explicitUserMessage != null && !explicitUserMessage.isBlank()) {
            return explicitUserMessage;
        }
        return AppMessages.getUserMessage(errorCode);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        if (userMessage != null && !userMessage.isEmpty()) {
            return userMessage;
        }
        return errorCode.getDefaultMessage();
    }

    public String getErrorCodeString() {
        return errorCode.getCode();
    }

    public static AppException from(Throwable throwable) {
        if (throwable instanceof AppException appException) {
            return appException;
        }
        String message = throwable == null ? "不明なエラーが発生しました" : throwable.getMessage();
        return new AppException(ErrorCode.UNKNOWN_ERROR, message, null, throwable);
    }

    public static AppException database(String message) {
        return new AppException(ErrorCode.DATABASE_QUERY_FAILED, message, null, null);
    }

    public static AppException database(String message, Throwable cause) {
        return new AppException(ErrorCode.DATABASE_QUERY_FAILED, message, null, cause);
    }

    public static AppException authentication(String message) {
        return new AppException(ErrorCode.AUTHENTICATION_FAILED, message, null, null);
    }

    public static AppException authorization(String message) {
        return new AppException(ErrorCode.AUTHORIZATION_FAILED, message, null, null);
    }

    public static AppException validation(String message) {
        return new AppException(ErrorCode.VALIDATION_FAILED, message, null, null);
    }

    public static AppException validation(String message, String userMessage) {
        return new AppException(ErrorCode.VALIDATION_FAILED, message, userMessage, null);
    }

    public static AppException requiredField(String fieldName) {
        String message = fieldName + (AppMessages.getLocale().getLanguage().equals("ja") ? "は必須項目です" : " is required.");
        return new AppException(ErrorCode.REQUIRED_FIELD_MISSING, message, message, null);
    }

    public static AppException businessRule(String message) {
        return new AppException(ErrorCode.BUSINESS_RULE_VIOLATION, message, null, null);
    }

    public static AppException businessRule(String message, String userMessage) {
        return new AppException(ErrorCode.BUSINESS_RULE_VIOLATION, message, userMessage, null);
    }

    public static AppException notFound(String resourceName) {
        String message = resourceName + (AppMessages.getLocale().getLanguage().equals("ja") ? "が見つかりません" : " was not found.");
        return new AppException(ErrorCode.RESOURCE_NOT_FOUND, message, message, null);
    }

    public static AppException user(String message) {
        return new AppException(ErrorCode.UNKNOWN_ERROR, message, null, null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AppException{");
        sb.append("errorCode=").append(errorCode);
        sb.append(", code='").append(errorCode.getCode()).append("'");
        sb.append(", message='").append(getMessage()).append("'");
        if (userMessage != null) {
            sb.append(", userMessage='").append(userMessage).append("'");
        }
        if (getCause() != null) {
            sb.append(", cause=").append(getCause());
        }
        sb.append("}");
        return sb.toString();
    }
}
