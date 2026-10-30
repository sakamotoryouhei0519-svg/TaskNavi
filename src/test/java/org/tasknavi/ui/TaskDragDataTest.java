package org.tasknavi.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.TransferHandler;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskDragDataTest {

    @Test
    void readsTheTaskIdThatWbsAndKanbanPutInTheDrag() {
        assertEquals(OptionalInt.of(42), TaskDragData.taskId(support(new StringSelection(" 42 "))));
    }

    @Test
    void textFromOtherAppsIsNotATask() {
        assertEquals(OptionalInt.empty(), TaskDragData.taskId(support(new StringSelection("設計レビュー"))));
    }

    @Test
    void dataWithoutTextIsNotATask() {
        Transferable nothing = new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[0];
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return false;
            }

            @Override
            public Object getTransferData(DataFlavor flavor) {
                throw new AssertionError("読まない");
            }
        };
        assertEquals(OptionalInt.empty(), TaskDragData.taskId(support(nothing)));
    }

    private static TransferHandler.TransferSupport support(Transferable data) {
        return new TransferHandler.TransferSupport(new JLabel(), data);
    }
}
