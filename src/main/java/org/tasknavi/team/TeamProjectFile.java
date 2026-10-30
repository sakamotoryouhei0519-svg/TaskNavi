package org.tasknavi.team;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;

/** プロジェクトフォルダの project.json（プロジェクト名と、フォルダの形式の版）。作るときに 1 回だけ書く。 */
final class TeamProjectFile {
    private static final int FORMAT = 1;

    private TeamProjectFile() {
    }

    static void create(Path dir, String name, String author, Clock clock) throws IOException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("プロジェクト名を入力してください");
        }
        Path file = dir.resolve(TeamProject.PROJECT_FILE);
        if (Files.exists(file)) {
            throw new IOException("このフォルダには既に共有プロジェクトがあります: " + dir);
        }
        Files.createDirectories(dir.resolve(TeamChangeLog.CHANGES_DIR));
        JsonObject json = new JsonObject();
        json.addProperty("format", FORMAT);
        json.addProperty("name", name.trim());
        json.addProperty("createdBy", author);
        json.addProperty("createdAt", Instant.now(clock).toString());
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
    }

    /** @return プロジェクト名。書かれていなければフォルダ名 */
    static String readName(Path dir) throws IOException {
        Path file = dir.resolve(TeamProject.PROJECT_FILE);
        if (!Files.isRegularFile(file)) {
            throw new IOException("共有プロジェクトのフォルダではありません: " + dir);
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException(TeamProject.PROJECT_FILE + " を読めません: " + e.getMessage(), e);
        }
        int format = json.has("format") ? json.get("format").getAsInt() : FORMAT;
        if (format > FORMAT) {
            throw new IOException("新しい版の TaskNavi で作られたプロジェクトです。アプリを更新してください");
        }
        return json.has("name") ? json.get("name").getAsString() : dir.getFileName().toString();
    }
}
