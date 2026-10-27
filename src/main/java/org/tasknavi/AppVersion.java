package org.tasknavi;

/**
 * アプリの版。配布用の JAR の manifest（pom.xml の app.version）から読む。
 * IDE やテストから動かしたときは manifest が無いので「開発版」になる。
 */
public final class AppVersion {

    private AppVersion() {
    }

    public static String current() {
        String version = AppVersion.class.getPackage().getImplementationVersion();
        return version != null && !version.isBlank() ? version : AppMessages.get("app.version.dev", "開発版");
    }
}
