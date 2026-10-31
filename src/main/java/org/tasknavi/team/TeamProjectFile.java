package org.tasknavi.team;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.tasknavi.AppMessages;

import java.io.IOException;
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
            throw new IllegalArgumentException(AppMessages.get("team.error.projectname.empty", "プロジェクト名を入力してください"));
        }
        Path file = dir.resolve(TeamProject.PROJECT_FILE);
        if (Files.exists(file)) {
            throw new IOException(AppMessages.format("team.error.project.exists", "このフォルダには既に共有プロジェクトがあります: {0}", dir));
        }
        Files.createDirectories(dir.resolve(TeamChangeLog.CHANGES_DIR));
        JsonObject json = new JsonObject();
        json.addProperty("format", FORMAT);
        json.addProperty("name", name.trim());
        json.addProperty("createdBy", author);
        json.addProperty("createdAt", Instant.now(clock).toString());
        TeamFiles.writeAtomically(file, new GsonBuilder().setPrettyPrinting().create().toJson(json));
    }

    /** @return プロジェクト名。書かれていなければフォルダ名 */
    static String readName(Path dir) throws IOException {
        Path file = dir.resolve(TeamProject.PROJECT_FILE);
        if (!Files.isRegularFile(file)) {
            throw new IOException(AppMessages.format("team.error.project.notfolder", "共有プロジェクトのフォルダではありません: {0}", dir));
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(TeamFiles.readText(file, TeamFiles.MAX_JSON_BYTES)).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException(AppMessages.format("team.error.project.unreadable", "{0} を読めません: {1}", TeamProject.PROJECT_FILE, e.getMessage()), e);
        }
        int format = json.has("format") ? json.get("format").getAsInt() : FORMAT;
        if (format > FORMAT) {
            throw new IOException(AppMessages.get("team.error.project.newer", "新しい版の TaskNavi で作られたプロジェクトです。アプリを更新してください"));
        }
        return json.has("name") ? json.get("name").getAsString() : dir.getFileName().toString();
    }
}
