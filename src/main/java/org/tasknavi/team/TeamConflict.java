package org.tasknavi.team;

import java.time.Instant;

/**
 * 互いの変更を知らずに同じタスクを変えた記録。後に並んだ方を採用し、消えた方の値をここに残す。
 * 記録を組み立てるたびに作り直されるので、「確認済み」は {@link #key()} で覚えておく。
 *
 * @param eventId   重なりを起こした記録（PARENT_DELETED では親を削除した記録）
 * @param field     SAME_FIELD のときだけ値が入る
 * @param keptValue 採用された値（SAME_FIELD のみ）
 * @param lostValue 上書きされて消えた値（SAME_FIELD のみ）
 */
public record TeamConflict(
        String eventId,
        Kind kind,
        String taskId,
        String taskName,
        TeamField field,
        String keptValue,
        String lostValue,
        String keptBy,
        String lostBy,
        Instant at) {

    public enum Kind {
        /** 同じ項目を別々に変えた。 */
        SAME_FIELD,
        /** 削除済みのタスクを、削除を知らずに変えた。変更は一覧に出ていない。 */
        EDIT_AFTER_DELETE,
        /** 他の人の変更を知らずにタスクを削除した。 */
        DELETE_OVER_EDIT,
        /** 親が削除されたので、このタスクは一覧に出ていない。 */
        PARENT_DELETED
    }

    /** 組み立て直しても変わらない識別子（設定の保存に使うので 80 文字以内）。 */
    public String key() {
        String detail = taskId + "|" + kind + "|" + (field != null ? field.key() : "");
        return eventId + ":" + Integer.toHexString(detail.hashCode());
    }
}
