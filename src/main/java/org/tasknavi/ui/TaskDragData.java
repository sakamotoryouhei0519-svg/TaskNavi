package org.tasknavi.ui;

import javax.swing.TransferHandler;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.IOException;
import java.util.OptionalInt;

/**
 * ドラッグ中のタスクの ID。WBS とカンバンは、ドラッグするタスクの ID を文字列で渡す。
 */
public final class TaskDragData {

    private TaskDragData() {
    }

    /** ほかのアプリからの文字など、タスクの ID でないものなら空。 */
    public static OptionalInt taskId(TransferHandler.TransferSupport support) {
        if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return OptionalInt.empty();
        }
        try {
            Object data = support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            return data instanceof String text ? OptionalInt.of(Integer.parseInt(text.trim())) : OptionalInt.empty();
        } catch (UnsupportedFlavorException | IOException | InvalidDnDOperationException | NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
