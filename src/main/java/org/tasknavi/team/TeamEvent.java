package org.tasknavi.team;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * 変更記録の 1 行。作られたあとは書き換えない。
 *
 * <p>clock は「書いた人がそれまでに見た記録の clock の最大値 + 1」。
 * PC の時計がずれていても、相手の変更を見てから行った変更は必ず後ろに並ぶ。</p>
 *
 * <p>base は更新した項目ごとに「書いた人が見ていた値を最後に書いた記録の ID」。
 * 適用時にそれが今の値と食い違えば、相手の変更を知らずに上書きした（＝重なった）と分かる。</p>
 */
public record TeamEvent(
        String eventId,
        String taskId,
        Op op,
        long clock,
        String deviceId,
        String author,
        Instant at,
        Map<TeamField, String> values,
        Map<TeamField, String> base,
        String baseEventId) {

    /**
     * RESTORE は削除したタスクを戻す。COMMENT はコメントを書き足す（値は変えないので重ならない）。
     * COMMENT_EDIT / COMMENT_DELETE は、baseEventId で指したコメントを直す・消す。
     */
    public enum Op { CREATE, UPDATE, DELETE, RESTORE, COMMENT, COMMENT_EDIT, COMMENT_DELETE }

    /** 全員の PC で同じ順番になる並べ方。 */
    public static final Comparator<TeamEvent> ORDER = Comparator
            .comparingLong(TeamEvent::clock)
            .thenComparing(TeamEvent::deviceId)
            .thenComparing(TeamEvent::eventId);

    public TeamEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(op, "op");
        Objects.requireNonNull(deviceId, "deviceId");
        author = author != null ? author : deviceId;
        at = at != null ? at : Instant.EPOCH;
        values = freeze(values);
        base = freeze(base);
    }

    /** 値に null（項目を空にした）を入れられるよう Map.copyOf は使わない。 */
    private static Map<TeamField, String> freeze(Map<TeamField, String> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new EnumMap<>(map));
    }
}
