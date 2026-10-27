package org.tasknavi.ui.taskdialog;

import org.junit.jupiter.api.Test;
import org.tasknavi.DatabaseTestConfig;
import org.tasknavi.model.EntryType;
import org.tasknavi.model.User;
import org.tasknavi.service.TaskService;
import org.tasknavi.service.UserSession;

import javax.swing.*;
import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * タスクの窓を開いた直後に、入力欄の初期値が合っているか。
 */
class TaskDialogUiTest {

    @org.junit.jupiter.api.BeforeAll
    static void setUpDatabase() {
        DatabaseTestConfig.useIsolatedDatabase();
    }

    @Test
    void dialogShouldStartWithExpectedDefaultValues() throws Exception {
        UserSession.login(new User(1, "dialog", "dialog@example.com", "USER", "dialog", null));
        try {
            SwingUtilities.invokeAndWait(() -> {
                TaskDialog dialog = new TaskDialog(null, TaskService.createDefault());

                JComboBox<?> typeCombo = getField(dialog, "typeCombo", JComboBox.class);
                JTextField startDateField = getField(dialog, "startDateField", JTextField.class);
                JTextField endDateField = getField(dialog, "endDateField", JTextField.class);

                assertEquals(EntryType.PHASE.name(), typeCombo.getSelectedItem());
                assertEquals(LocalDate.now().toString(), startDateField.getText());
                assertEquals(LocalDate.now().plusDays(7).toString(), endDateField.getText());
            });
        } finally {
            UserSession.logout();
        }
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
