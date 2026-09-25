package org.example;

import org.example.ui.taskdialog.TaskDialog;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 【UI テスト】
 * Swing のダイアログ初期化が期待どおりかを確認します。
 * 初心者向けの解説:
 * - UI は画面が開かれたときに初期値が合っているか確認するのが大切です。
 * - ここではダイアログ生成直後の状態を検証し、入力フォームの不具合を早く見つけられます。
 */
class TaskDialogUiTest {

    @org.junit.jupiter.api.BeforeAll
    static void setUpDatabase() {
        DatabaseTestConfig.useIsolatedDatabase();
    }

    @Test
    void dialogShouldStartWithExpectedDefaultValues() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TaskDialog dialog = new TaskDialog(null, TaskService.createDefault());

            JComboBox<?> typeCombo = getField(dialog, "typeCombo", JComboBox.class);
            JTextField startDateField = getField(dialog, "startDateField", JTextField.class);
            JTextField endDateField = getField(dialog, "endDateField", JTextField.class);

            assertEquals(EntryType.PHASE.name(), typeCombo.getSelectedItem());
            assertEquals(LocalDate.now().toString(), startDateField.getText());
            assertEquals(LocalDate.now().plusDays(7).toString(), endDateField.getText());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String fieldName, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new AssertionError("Field not found: " + fieldName, e);
        }
    }
}
