package org.tasknavi.team;

import java.time.Instant;

/**
 * タスクへのコメント 1 件。直す・消すは、書いた PC からだけできる（ほかの人と同時に直して重なることがない）。
 *
 * @param eventId  コメントを書いた記録の ID。直す・消すときはこれを指す
 * @param deviceId 書いた PC。表示名は変えられるので、自分のコメントかどうかはこちらで決める
 * @param at       書いた時刻（直しても変えない）
 * @param edited   あとから直したか
 * @param deleted  消したか。消したコメントも、並び順を保つため一覧には残す
 */
public record TeamComment(String eventId, String author, String deviceId, Instant at, String text,
                          boolean edited, boolean deleted) {

    TeamComment withText(String newText) {
        return new TeamComment(eventId, author, deviceId, at, newText, true, false);
    }

    TeamComment asDeleted() {
        return new TeamComment(eventId, author, deviceId, at, "", edited, true);
    }
}
