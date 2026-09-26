package org.tasknavi;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class AppExceptionTest {

    @Test
    void shouldExposeErrorCodeAndUserMessage() {
        AppException ex = AppException.validation("名前が未入力です", "名前を入力してください。");

        assertEquals(AppException.ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        assertEquals("VAL001", ex.getErrorCodeString());
        assertEquals("名前を入力してください。", ex.getUserMessage());
    }

    @Test
    void shouldUseDefaultJapaneseMessageWhenUserMessageIsMissing() {
        AppException ex = AppException.requiredField("担当者");

        assertTrue(ex.getUserMessage().contains("担当者"));
        assertEquals("VAL003", ex.getErrorCodeString());
    }

    @Test
    void shouldResolveLocalizedMessageFromResourceBundle() {
        AppException ex = AppException.businessRule("条件に未満です");

        assertTrue(ex.getUserMessage().contains("処理条件"));
    }

    @Test
    void shouldUseEnglishMessagesWhenLocaleIsEnglish() {
        AppMessages.setLocale(Locale.ENGLISH);
        try {
            AppException ex = AppException.businessRule("条件に未満です");
            assertTrue(ex.getUserMessage().contains("processing conditions"));
        } finally {
            AppMessages.setLocale(Locale.JAPAN);
        }
    }

    @Test
    void shouldRejectInvalidDateRange() {
        TaskDao taskDao = new TaskDao();
        Task task = new Task(1, "範囲不正", null, 1, 0, 0, "未着手",
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 5));

        AppException ex = assertThrows(AppException.class, () -> taskDao.validateTask(task));
        assertEquals(AppException.ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
    }
}
