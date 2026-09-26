package org.tasknavi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PriorityTest {

    @Test
    void testPriorityEnumConversion() {
        // 文字列からEnumへの変換確認
        assertEquals(Priority.HIGH, Priority.fromString("高"));
        assertEquals(Priority.MEDIUM, Priority.fromString("中"));
        assertEquals(Priority.LOW, Priority.fromString("低"));

        // 不正な文字列はデフォルト(MEDIUM)になるか確認
        assertEquals(Priority.MEDIUM, Priority.fromString("不明"));
    }

    @Test
    void testTaskPriorityIntegration() {
        // Taskクラス経由での動作確認
        Task task = new Task(1, "テストタスク", null, 1, 0, 0, "未着手", null, null);

        task.setPriority("高");
// getPriorityLabel() を使って文字列 "高" を検証、あるいは getPriority() が Priority Enum を返すためそのまま比較
        assertEquals("高", task.getPriorityLabel());
        assertEquals(Priority.HIGH, task.getPriority());
    }
}