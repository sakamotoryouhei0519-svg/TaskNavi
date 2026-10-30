package org.tasknavi.ui.team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasknavi.team.TeamNotice;
import org.tasknavi.team.TeamProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1 回分の確認の結果。裏側のスレッドで {@link #check} して、画面のスレッドで受け取る。
 *
 * @param failure       開いているプロジェクトを読めなかったときの失敗。読めたら null
 * @param othersChanged 開いていないプロジェクトのどれかを読み直した
 * @param notices       自分あての知らせ。キーはプロジェクト名（開いているものが先）
 */
record TeamPoll(Exception failure, boolean othersChanged, Map<String, List<TeamNotice>> notices) {

    private static final Logger logger = LoggerFactory.getLogger(TeamPoll.class);

    /** ファイルが変わっていたものだけ読み直し、届いた知らせを受け取る。 */
    static TeamPoll check(TeamProject opened, List<TeamProject> others) {
        Exception failure = null;
        if (opened != null) {
            try {
                opened.reloadIfChanged();
            } catch (IOException | RuntimeException e) {
                failure = e;
            }
        }
        boolean othersChanged = false;
        for (TeamProject other : others) {
            try {
                othersChanged |= other.reloadIfChanged();
            } catch (IOException | RuntimeException e) {
                logger.debug("共有フォルダの確認に失敗しました: {}", other.directory(), e);
            }
        }
        Map<String, List<TeamNotice>> notices = new LinkedHashMap<>();
        List<TeamProject> all = new ArrayList<>(others);
        if (opened != null) {
            all.add(0, opened);
        }
        for (TeamProject each : all) {
            List<TeamNotice> taken = each.takeNotices();
            if (!taken.isEmpty()) {
                notices.put(each.name(), taken);
            }
        }
        return new TeamPoll(failure, othersChanged, notices);
    }
}
