package org.tasknavi.persistence;

import ch.qos.logback.core.PropertyDefinerBase;

/**
 * logback.xml の {@code LOG_DIR} を決める。ログは DB と同じ場所の logs フォルダに置く。
 * logback はファイル名の \ を特別な文字として読むので、/ に直して渡す。
 */
public class LogDirectoryDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        try {
            return toLogbackPath(AppDataLocation.logDirectory().toString());
        } catch (RuntimeException e) {
            return toLogbackPath(System.getProperty("java.io.tmpdir") + "/TaskNavi/logs");
        }
    }

    static String toLogbackPath(String path) {
        return path.replace('\\', '/');
    }
}
