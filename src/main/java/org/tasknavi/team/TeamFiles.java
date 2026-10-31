package org.tasknavi.team;

import org.tasknavi.AppMessages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** 共有フォルダのファイルの読み書き。他の PC が置いたファイルは中身も大きさも信用しない。 */
final class TeamFiles {
    /** changes\ の 1 ファイルの上限。1 回の変更は数百バイトなので、20 万回分ほど。 */
    static final long MAX_LOG_BYTES = 64L * 1024 * 1024;
    /** project.json と members\*.json の上限。普段は数百バイト。 */
    static final long MAX_JSON_BYTES = 1024 * 1024;

    /** 上限より大きくて読まなかった。 */
    static final class TooLargeException extends IOException {
        TooLargeException(Path file, long maxBytes) {
            super(AppMessages.format("team.error.file.too.large", "ファイルが大きすぎるので読みません（上限 {0} KB）: {1}",
                    String.valueOf(maxBytes / 1024), file.getFileName()));
        }
    }

    private TeamFiles() {
    }

    /** UTF-8 で読む。maxBytes を超えていたら途中までも返さず TooLargeException。 */
    static String readText(Path file, long maxBytes) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] bytes = in.readNBytes((int) Math.min(maxBytes + 1, Integer.MAX_VALUE - 8));
            if (bytes.length > maxBytes) {
                throw new TooLargeException(file, maxBytes);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    /**
     * 同じフォルダの一時ファイルに書き切ってから置き換える。
     * 途中で落ちても、元のファイルか新しいファイルのどちらかが丸ごと残る。
     */
    static void writeAtomically(Path file, String text) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer out = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
                while (out.hasRemaining()) {
                    channel.write(out);
                }
                channel.force(true);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
