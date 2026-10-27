package org.tasknavi.persistence;

import java.nio.file.Path;

/**
 * データベース・バックアップ・ログを置くフォルダ。
 * 起動したフォルダに左右されないよう、ユーザーごとの決まった場所を使う。
 */
public final class AppDataLocation {
    public static final String DB_FILE_NAME = "tasknavi.db";
    private static final String BACKUP_DIR_NAME = "backups";
    private static final String LOG_DIR_NAME = "logs";

    private AppDataLocation() {
    }

    /** `-Dtasknavi.dataDir` → 環境変数 TASKNAVI_DATA_DIR → %APPDATA%\TaskNavi → ~/.tasknavi の順。 */
    public static Path dataDirectory() {
        return resolveDataDirectory(
                System.getProperty("tasknavi.dataDir"),
                System.getenv("TASKNAVI_DATA_DIR"),
                System.getenv("APPDATA"),
                System.getProperty("user.home"));
    }

    public static Path databaseFile() {
        return dataDirectory().resolve(DB_FILE_NAME);
    }

    public static Path backupDirectory() {
        return dataDirectory().resolve(BACKUP_DIR_NAME);
    }

    public static Path logDirectory() {
        return dataDirectory().resolve(LOG_DIR_NAME);
    }

    static Path resolveDataDirectory(String property, String env, String appData, String userHome) {
        if (!isBlank(property)) {
            return Path.of(property.trim()).toAbsolutePath();
        }
        if (!isBlank(env)) {
            return Path.of(env.trim()).toAbsolutePath();
        }
        if (!isBlank(appData)) {
            return Path.of(appData.trim(), "TaskNavi").toAbsolutePath();
        }
        return Path.of(userHome, ".tasknavi").toAbsolutePath();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
