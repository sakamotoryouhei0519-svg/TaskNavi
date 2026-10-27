package org.tasknavi.team;

import java.time.Instant;

/**
 * タスクへのコメント 1 件。書き足すだけで、直したり消したりはしない。
 */
public record TeamComment(String eventId, String author, Instant at, String text) {
}
