package org.tasknavi;

/**
 * アプリの版。配布用の JAR の manifest（pom.xml の app.version）から読む。
 * IDE やテストから動かしたときは manifest が無いので「開発版」になる。
 */
public final class AppVersion {

    /** 新しい版を置くページ。 */
    public static final String RELEASES_URL = "https://github.com/sakamotoryouhei0519-svg/TaskNavi/releases/latest";

    private AppVersion() {
    }

    public static String current() {
        String version = release();
        return version != null ? version : AppMessages.get("app.version.dev", "開発版");
    }

    /** 配布用の版。開発版なら null。 */
    public static String release() {
        String version = AppVersion.class.getPackage().getImplementationVersion();
        return version != null && !version.isBlank() ? version.trim() : null;
    }

    /**
     * 「1.10.0」と「1.9.0」のように、点で区切った数を前から比べる。足りない所は 0、数でない所は 0 とみなす。
     *
     * @return a が新しければ正、同じなら 0、古ければ負
     */
    public static int compare(String a, String b) {
        String[] left = a.split("[.\\-]");
        String[] right = b.split("[.\\-]");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int diff = Long.compare(number(left, i), number(right, i));
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    private static long number(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        String digits = parts[index].trim().replaceFirst("^(\\d{1,18}).*", "$1");
        return digits.matches("\\d+") ? Long.parseLong(digits) : 0;
    }
}
