package org.tasknavi.ui.team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 共有フォルダの読み書きを裏側の 1 本の列に並べる。ネットワークドライブが遅くても画面を止めず、
 * 書き込みの途中に読み直しが割り込まない。結果は画面のスレッドで受け取る。
 */
final class TeamBackground {

    private static final Logger logger = LoggerFactory.getLogger(TeamBackground.class);

    /** どれも画面のスレッドで呼ぶ。 */
    interface Hooks {
        /** 仕事を列に並べた（読み込み中の表示に使う）。 */
        void started();

        /** 成功した。結果を渡す前に呼ぶ。 */
        void succeeded();

        /** 成功でも失敗でも、結果を渡したあとに呼ぶ。 */
        void finished();
    }

    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "team-io");
        thread.setDaemon(true);
        return thread;
    });
    private final Hooks hooks;
    private int pending;

    TeamBackground(Hooks hooks) {
        this.hooks = hooks;
    }

    /** 結果を待っている仕事があるか。 */
    boolean busy() {
        return pending > 0;
    }

    /** 列に並べるだけ。結果の受け取りや失敗の扱いは work の中で行う。 */
    void execute(Runnable work) {
        io.execute(work);
    }

    /** 失敗はログに残してから onFailure に渡す。 */
    <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        pending++;
        hooks.started();
        io.execute(() -> {
            T result = null;
            Exception error = null;
            try {
                result = work.call();
            } catch (Exception e) {
                error = e;
            }
            T finalResult = result;
            Exception finalError = error;
            SwingUtilities.invokeLater(() -> {
                pending--;
                if (finalError == null) {
                    hooks.succeeded();
                    onSuccess.accept(finalResult);
                } else {
                    logger.warn("チームプロジェクトの読み書きに失敗しました", finalError);
                    onFailure.accept(finalError);
                }
                hooks.finished();
            });
        });
    }

    void shutdown() {
        io.shutdownNow();
    }
}
