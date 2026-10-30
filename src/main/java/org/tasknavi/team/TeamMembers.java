package org.tasknavi.team;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.tasknavi.AppVersion;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** プロジェクトフォルダの members\。1 台につき 1 ファイルで、その PC の表示名とアプリの版を持つ。 */
final class TeamMembers {

    private TeamMembers() {
    }

    /** 名前順。読めないファイル（同期の途中）は飛ばす。 */
    static List<TeamMember> read(Path dir) throws IOException {
        Path membersDir = dir.resolve(TeamProject.MEMBERS_DIR);
        List<TeamMember> result = new ArrayList<>();
        if (!Files.isDirectory(membersDir)) {
            return result;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(membersDir, "*.json")) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                String id = fileName.substring(0, fileName.length() - ".json".length());
                if (!TeamChangeLog.isValidDeviceId(id)) {
                    continue;
                }
                try {
                    JsonObject json = JsonParser.parseString(TeamFiles.readText(file, TeamFiles.MAX_JSON_BYTES))
                            .getAsJsonObject();
                    String memberName = json.get("name").getAsString();
                    Instant joinedAt = json.has("joinedAt") ? Instant.parse(json.get("joinedAt").getAsString()) : null;
                    String version = json.has("appVersion") ? json.get("appVersion").getAsString() : null;
                    result.add(new TeamMember(id, memberName, joinedAt, version));
                } catch (RuntimeException e) {
                    // 同期の途中。次の読み直しで読める。
                } catch (TeamFiles.TooLargeException e) {
                    // 1 人分の情報がこの大きさになることはない。壊れたか細工されたファイルなので読まない。
                }
            }
        }
        result.sort(Comparator.comparing(TeamMember::name).thenComparing(TeamMember::deviceId));
        return List.copyOf(result);
    }

    /**
     * members\&lt;端末ID&gt;.json を、無いとき・名前か版が変わったときだけ書く。
     * 開発版（appVersion が null）で開いても、前に書いた版は消さない。
     *
     * @return 書いたら true
     */
    static boolean register(Path dir, List<TeamMember> members, String deviceId, String author,
                            String appVersion, Clock clock) throws IOException {
        TeamMember current = members.stream()
                .filter(member -> member.deviceId().equals(deviceId))
                .findFirst()
                .orElse(null);
        String version = appVersion != null ? appVersion : current != null ? current.appVersion() : null;
        if (current != null && current.name().equals(author) && Objects.equals(current.appVersion(), version)) {
            return false;
        }
        Instant joinedAt = current != null && current.joinedAt() != null ? current.joinedAt() : Instant.now(clock);
        JsonObject json = new JsonObject();
        json.addProperty("name", author);
        json.addProperty("joinedAt", joinedAt.toString());
        if (version != null) {
            json.addProperty("appVersion", version);
        }
        Path file = dir.resolve(TeamProject.MEMBERS_DIR).resolve(deviceId + ".json");
        Files.createDirectories(file.getParent());
        TeamFiles.writeAtomically(file, json.toString());
        return true;
    }

    /** appVersion より新しい版を使っている人のうち、いちばん新しい版の人。いなければ、または appVersion が null なら null。 */
    static TeamMember newest(List<TeamMember> members, String appVersion) {
        if (appVersion == null) {
            return null;
        }
        TeamMember newest = null;
        for (TeamMember member : members) {
            String version = member.appVersion();
            if (version != null && AppVersion.compare(version, newest != null ? newest.appVersion() : appVersion) > 0) {
                newest = member;
            }
        }
        return newest;
    }
}
