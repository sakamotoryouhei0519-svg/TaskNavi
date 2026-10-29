package org.tasknavi.ui.wbs;

import org.junit.jupiter.api.Test;
import org.tasknavi.model.Task;

import javax.swing.JLabel;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WbsDetailFormTest {

    @Test
    void doubleClickingTheNameOpensTheEditorOnlyWhenATaskIsShown() throws Exception {
        AtomicInteger edits = new AtomicInteger();
        WbsDetailForm form = new WbsDetailForm(edits::incrementAndGet);
        JLabel name = nameLabel(form);

        click(name, 2);
        assertEquals(0, edits.get(), "何も選んでいなければ開かない");

        form.show(new Task(1, "設計", null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)), "P", "", "1.1");
        click(name, 1);
        assertEquals(0, edits.get(), "1 回のクリックでは開かない");
        click(name, 2);
        assertEquals(1, edits.get());

        form.clear();
        click(name, 2);
        assertEquals(1, edits.get());
    }

    private static void click(Component target, int count) {
        target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                InputEvent.BUTTON1_DOWN_MASK, 5, 5, count, false, MouseEvent.BUTTON1));
    }

    private static JLabel nameLabel(WbsDetailForm form) throws Exception {
        Field field = WbsDetailForm.class.getDeclaredField("nameLabel");
        field.setAccessible(true);
        return (JLabel) field.get(form);
    }
}
