package org.tasknavi;

/**
 * アプリの中で起きたエラー。ログ用の詳しいメッセージと、画面に出す短いメッセージを別に持つ。
 * 画面では {@code ui/ErrorDialogUtil} が {@link #getUserMessage()} を表示する。
 */
public class AppException extends RuntimeException {
    /** エラーの種類。画面の文言は messages の {@code error.*} から引く。 */
    public enum ErrorCode {
        DATABASE_QUERY_FAILED("DB002", "データベースクエリの実行に失敗しました"),
        VALIDATION_FAILED("VAL001", "入力値の検証に失敗しました"),
        REQUIRED_FIELD_MISSING("VAL003", "必須項目が入力されていません"),
        BUSINESS_RULE_VIOLATION("BIZ001", "ビジネスルール違反です"),
        RESOURCE_NOT_FOUND("BIZ003", "リソースが見つかりません"),
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

    private AppException(ErrorCode errorCode, String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage != null && !userMessage.isBlank()
                ? userMessage
                : AppMessages.getUserMessage(errorCode);
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

    public static AppException database(String message) {
        return new AppException(ErrorCode.DATABASE_QUERY_FAILED, message, null, null);
    }

    public static AppException database(String message, Throwable cause) {
        return new AppException(ErrorCode.DATABASE_QUERY_FAILED, message, null, cause);
    }

    public static AppException validation(String message) {
        return new AppException(ErrorCode.VALIDATION_FAILED, message, null, null);
    }

    public static AppException validation(String message, String userMessage) {
        return new AppException(ErrorCode.VALIDATION_FAILED, message, userMessage, null);
    }

    public static AppException requiredField(String fieldName) {
        String message = AppMessages.format("error.required.named", "{0}は必須項目です", fieldName);
        return new AppException(ErrorCode.REQUIRED_FIELD_MISSING, message, message, null);
    }

    public static AppException businessRule(String message) {
        return new AppException(ErrorCode.BUSINESS_RULE_VIOLATION, message, null, null);
    }

    public static AppException businessRule(String message, String userMessage) {
        return new AppException(ErrorCode.BUSINESS_RULE_VIOLATION, message, userMessage, null);
    }

    public static AppException notFound(String resourceName) {
        String message = AppMessages.format("error.notfound.named", "{0}が見つかりません", resourceName);
        return new AppException(ErrorCode.RESOURCE_NOT_FOUND, message, message, null);
    }

    /** ID で探したタスクが無い。 */
    public static AppException taskNotFound(int taskId) {
        return notFound(AppMessages.format("error.task.id", "タスクID: {0}", String.valueOf(taskId)));
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
