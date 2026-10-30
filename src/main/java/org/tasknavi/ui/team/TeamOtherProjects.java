package org.tasknavi.ui.team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasknavi.team.TeamProject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 最近開いた共有プロジェクトのうち、今開いていないもの（最近開いた順）。
 * 「今日」と期限の通知のために読むだけで、画面の切り替えには使わない。画面のスレッドから使う。
 */
final class TeamOtherProjects {

    private static final Logger logger = LoggerFactory.getLogger(TeamOtherProjects.class);

    private final Map<Path, TeamProject> byDir = new LinkedHashMap<>();

    /** 同じフォルダを別の書き方で指していても、同じものとして扱う。 */
    static Path key(Path dir) {
        return dir.toAbsolutePath().normalize();
    }

    /** 読み込み済みなら返す。無ければ null。 */
    TeamProject get(Path dir) {
        return byDir.get(key(dir));
    }

    /** @return 外したら true */
    boolean remove(Path dir) {
        return byDir.remove(key(dir)) != null;
    }

    List<TeamProject> list() {
        return List.copyOf(byDir.values());
    }

    /** 閉じた・切り替えたプロジェクトも、最近開いたものなら持ち続ける。 */
    void keep(TeamProject previous, List<Path> recent) {
        if (previous == null) {
            return;
        }
        Path key = key(previous.directory());
        if (recent.contains(key)) {
            byDir.put(key, previous);
        }
    }

    /**
     * 裏側で開いたものを足す。今開いているものと、すでにあるものは除く。
     *
     * @return 足したら true
     */
    boolean addLoaded(List<TeamProject> loaded, TeamProject current) {
        Path currentKey = current != null ? key(current.directory()) : null;
        boolean added = false;
        for (TeamProject opened : loaded) {
            Path key = key(opened.directory());
            if (!key.equals(currentKey) && !byDir.containsKey(key)) {
                byDir.put(key, opened);
                added = true;
            }
        }
        return added;
    }

    /** 裏側のスレッドで呼ぶ。開けないもの（ドライブの準備中など）は飛ばす。 */
    static List<TeamProject> openAll(List<Path> dirs, String deviceId, String author) {
        List<TeamProject> loaded = new ArrayList<>();
        for (Path dir : dirs) {
            try {
                loaded.add(TeamProject.open(dir, deviceId, author));
            } catch (IOException | RuntimeException e) {
                logger.info("最近のプロジェクトを開けませんでした（ドライブの準備中かもしれません）: {}", dir, e);
            }
        }
        return loaded;
    }
}
