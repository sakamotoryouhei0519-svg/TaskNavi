package org.example.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 【シンプルなロギングユーティリティ】
 * 外部ライブラリを使用せず、標準的なJava機能でログ出力を行います。
 */
public class Logger {
    
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * INFOレベルのログを出力
     */
    public static void info(String message) {
        log("INFO", message, null);
    }
    
    /**
     * ERRORレベルのログを出力
     */
    public static void error(String message) {
        log("ERROR", message, null);
    }
    
    /**
     * ERRORレベルのログを出力（例外付き）
     */
    public static void error(String message, Throwable throwable) {
        log("ERROR", message, throwable);
    }
    
    /**
     * WARNレベルのログを出力
     */
    public static void warn(String message) {
        log("WARN", message, null);
    }
    
    /**
     * DEBUGレベルのログを出力
     */
    public static void debug(String message) {
        log("DEBUG", message, null);
    }
    
    /**
     * 共通ログ出力メソッド
     */
    private static void log(String level, String message, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String logMessage = String.format("[%s] [%s] [%s] %s", 
            timestamp, 
            level, 
            Thread.currentThread().getName(), 
            message);
        
        if (level.equals("ERROR")) {
            System.err.println(logMessage);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        } else {
            System.out.println(logMessage);
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }
        }
    }
}
