package org.example;

public class AppException extends RuntimeException {
    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }

    public static AppException user(String message) {
        return new AppException(message);
    }
}
