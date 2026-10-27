package org.tasknavi.team;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * この PC でのチーム設定（端末 ID・表示名・最近開いたプロジェクト）。
 *
 * <p>端末 ID は変更記録のファイル名になる。同じ人が 2 台で使っても別のファイルになるよう、
 * PC ごとに一度だけ作って保存する。</p>
 */
public final class TeamSettings {
    static final int MAX_RECENT = 8;
    private static final String KEY_DEVICE_ID = "team.deviceId";
    private static final String KEY_DISPLAY_NAME = "team.displayName";
    private static final String KEY_RECENT = "team.recentProjects";
    private static final String KEY_LAST = "team.lastProject";
    private static final String PROFILES_NODE = "profiles";
    private static final String PROFILE_PROPERTY = "tasknavi.team.profile";
    private static final String PROFILE_ARGUMENT = "--team-profile=";

    private final Preferences prefs;

    public TeamSettings(Preferences prefs) {
        this.prefs = prefs;
    }

    /**
     * 起動オプション {@code --team-profile=名前} が付いていれば、端末 ID・表示名・最近のプロジェクトを別に持つ。
     * 1 台の PC で 2 人目として起動し、共有の動きを試すため。
     */
    public static TeamSettings userDefault() {
        Preferences root = Preferences.userNodeForPackage(TeamSettings.class);
        String profile = profile();
        return new TeamSettings(profile.isEmpty() ? root : root.node(PROFILES_NODE).node(profile));
    }

    /** 付いていなければ空文字。使えない文字が入っていれば無視する。 */
    public static String profile() {
        String profile = System.getProperty(PROFILE_PROPERTY, "").trim();
        return profile.matches("[A-Za-z0-9_-]{1,20}") ? profile : "";
    }

    /** {@code --team-profile=名前} を拾ってシステムプロパティに移す。 */
    public static void applyLaunchArguments(String[] args) {
        for (String arg : args) {
            if (arg.startsWith(PROFILE_ARGUMENT)) {
                System.setProperty(PROFILE_PROPERTY, arg.substring(PROFILE_ARGUMENT.length()));
            }
        }
    }

    public String deviceId() {
        String saved = prefs.get(KEY_DEVICE_ID, "");
        if (TeamChangeLog.isValidDeviceId(saved)) {
            return saved;
        }
        String created = newDeviceId(System.getProperty("user.name"), new SecureRandom());
        prefs.put(KEY_DEVICE_ID, created);
        return created;
    }

    /** Windows のユーザー名（使える文字だけ）＋ランダムな 6 桁。 */
    static String newDeviceId(String osUser, Random random) {
        String base = osUser == null ? "" : osUser.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        if (base.isEmpty()) {
            base = "pc";
        }
        byte[] suffix = new byte[3];
        random.nextBytes(suffix);
        return base + "-" + HexFormat.of().formatHex(suffix);
    }

    /** 未設定なら空文字。 */
    public String displayName() {
        return prefs.get(KEY_DISPLAY_NAME, "").trim();
    }

    public void setDisplayName(String name) {
        prefs.put(KEY_DISPLAY_NAME, name == null ? "" : name.trim());
    }

    public List<Path> recentProjects() {
        List<Path> result = new ArrayList<>();
        for (String line : prefs.get(KEY_RECENT, "").split("\n")) {
            if (!line.isBlank()) {
                result.add(Path.of(line));
            }
        }
        return result;
    }

    /** 先頭に移し、古いものから消す。 */
    public void remember(Path projectDir) {
        Path normalized = projectDir.toAbsolutePath().normalize();
        List<Path> recent = recentProjects();
        recent.remove(normalized);
        recent.add(0, normalized);
        while (recent.size() > MAX_RECENT) {
            recent.remove(recent.size() - 1);
        }
        saveRecent(recent);
        prefs.put(KEY_LAST, normalized.toString());
    }

    public void forget(Path projectDir) {
        Path normalized = projectDir.toAbsolutePath().normalize();
        List<Path> recent = recentProjects();
        recent.remove(normalized);
        saveRecent(recent);
        if (lastProject().filter(normalized::equals).isPresent()) {
            prefs.remove(KEY_LAST);
        }
    }

    public Optional<Path> lastProject() {
        String saved = prefs.get(KEY_LAST, "");
        return saved.isBlank() ? Optional.empty() : Optional.of(Path.of(saved));
    }

    public void clearLastProject() {
        prefs.remove(KEY_LAST);
    }

    /** 「このままでよい」とした重なりの {@link TeamConflict#key()}。この PC だけで覚える。 */
    public Set<String> acknowledgedConflicts(Path projectDir) {
        try {
            return new HashSet<>(Arrays.asList(ackNode(projectDir).keys()));
        } catch (BackingStoreException e) {
            return new HashSet<>();
        }
    }

    public void acknowledge(Path projectDir, Collection<String> conflictKeys) {
        Preferences node = ackNode(projectDir);
        for (String key : conflictKeys) {
            node.put(key, "1");
        }
    }

    private Preferences ackNode(Path projectDir) {
        String id = Integer.toHexString(projectDir.toAbsolutePath().normalize().toString().hashCode());
        return prefs.node("ack").node(id);
    }

    private void saveRecent(List<Path> recent) {
        prefs.put(KEY_RECENT, String.join("\n", recent.stream().map(Path::toString).toList()));
    }
}
