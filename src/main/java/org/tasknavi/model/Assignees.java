package org.tasknavi.model;

/**
 * 担当者名の比べ方。担当者はただの文字なので、前後の空白と大文字・小文字の違いでは別人にしない。
 */
public final class Assignees {

    private Assignees() {
    }

    /** 同じ人を指すか。どちらかが空なら同じとはみなさない。 */
    public static boolean same(String a, String b) {
        return a != null && b != null && !a.isBlank() && !b.isBlank() && a.trim().equalsIgnoreCase(b.trim());
    }
}
