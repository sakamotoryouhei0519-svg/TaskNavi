package org.tasknavi.ui.team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasknavi.AppMessages;
import org.tasknavi.AppVersion;
import org.tasknavi.team.TeamMember;
import org.tasknavi.team.TeamProject;
import org.tasknavi.ui.AppDialog;
import org.tasknavi.ui.ErrorDialogUtil;

import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/** 新しい版のアプリを使っている人がいるときに出す、アプリの入れ替え方。 */
final class TeamUpdateHelp {

    private static final Logger logger = LoggerFactory.getLogger(TeamUpdateHelp.class);

    private TeamUpdateHelp() {
    }

    static void show(Component owner, TeamProject opened) {
        TeamMember newer = opened.newerAppMember();
        String who = newer != null
                ? AppMessages.format("team.update.who", "{0} さんが TaskNavi {1} を使っています。この PC は {2} です。",
                        newer.name(), newer.appVersion(), AppVersion.current())
                : AppMessages.format("team.update.who.unknown",
                        "新しい版の TaskNavi で書かれた記録があります。この PC は {0} です。", AppVersion.current());
        String steps = AppMessages.format("team.update.steps",
                "入れ替え方:\n1. TaskNavi を閉じる\n2. 新しい zip を受け取る（配った人から、またはリリースのページから）\n"
                        + "3. 今のアプリのフォルダ（{0}）を消して、同じ場所に zip を展開する\n4. TaskNavi.exe を起動する\n\n"
                        + "タスクのデータは別の場所にあるので消えません。", installFolder());
        int choice = AppDialog.choose(owner, AppMessages.get("team.update.title", "アプリの更新"), who + "\n\n" + steps,
                AppMessages.get("team.update.open", "リリースのページを開く"),
                AppMessages.get("team.conflict.close", "閉じる"));
        if (choice == 0) {
            openReleases(owner);
        }
    }

    /** exe から起動していればそのフォルダ。開発中は例を出す。 */
    private static String installFolder() {
        String appPath = System.getProperty("jpackage.app-path");
        return appPath != null && Path.of(appPath).getParent() != null
                ? Path.of(appPath).getParent().toString()
                : AppMessages.get("team.update.folder.default", "C:\\TaskNavi\\TaskNavi など");
    }

    private static void openReleases(Component owner) {
        try {
            Desktop.getDesktop().browse(URI.create(AppVersion.RELEASES_URL));
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            logger.warn("リリースのページを開けませんでした", e);
            ErrorDialogUtil.showError(owner, AppMessages.format("team.update.open.failed",
                    "ブラウザを開けませんでした。次のアドレスを開いてください: {0}", AppVersion.RELEASES_URL));
        }
    }
}
