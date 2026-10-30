package org.tasknavi.team;

import java.time.Instant;

/**
 * プロジェクトに参加している PC と、その表示名。members\&lt;端末ID&gt;.json 1 つ分。
 *
 * @param appVersion その PC のアプリの版。1.2.0 までの版や開発版で書いたものは null
 */
public record TeamMember(String deviceId, String name, Instant joinedAt, String appVersion) {
}
