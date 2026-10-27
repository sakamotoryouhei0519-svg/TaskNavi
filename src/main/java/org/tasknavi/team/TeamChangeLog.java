package org.tasknavi.team;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * プロジェクトフォルダの changes\&lt;端末ID&gt;.jsonl を読み書きする。
 *
 * <p>各 PC は自分の端末 ID のファイルにだけ追記する。1 つのファイルを複数の PC が書かないので、
 * 同期フォルダでも競合コピーが生まれない。</p>
 */
public final class TeamChangeLog {
    public static final String CHANGES_DIR = "changes";
    public static final String EXTENSION = ".jsonl";
    private static final Pattern DEVICE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    /**
     * @param unreadableLines 同期途中や壊れていて読めなかった行の数
     */
    public record ReadResult(List<TeamEvent> events, int unreadableLines) {
        public ReadResult {
            events = List.copyOf(events);
        }
    }

    private TeamChangeLog() {
    }

    public static boolean isValidDeviceId(String deviceId) {
        return deviceId != null && DEVICE_ID.matcher(deviceId).matches();
    }

    public static Path logFile(Path projectDir, String deviceId) {
        if (!isValidDeviceId(deviceId)) {
            throw new IllegalArgumentException("端末 ID に使えない文字があります: " + deviceId);
        }
        return projectDir.resolve(CHANGES_DIR).resolve(deviceId + EXTENSION);
    }

    /** changes フォルダの全 .jsonl を読む。同期ツールが作った複製ファイルも読み、重複は組み立て時に除く。 */
    public static ReadResult readAll(Path projectDir) throws IOException {
        Path dir = projectDir.resolve(CHANGES_DIR);
        List<TeamEvent> events = new ArrayList<>();
        int unreadable = 0;
        if (!Files.isDirectory(dir)) {
            return new ReadResult(events, 0);
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*" + EXTENSION)) {
            for (Path file : files) {
                unreadable += readFile(file, events);
            }
        }
        return new ReadResult(events, unreadable);
    }

    /**
     * changes と members の各ファイルの名前・大きさ・更新時刻をつないだ文字列。
     * 中身を読まずに「誰かが書いたか」を安く調べるために使う。
     */
    public static String fingerprint(Path projectDir) throws IOException {
        List<String> parts = new ArrayList<>();
        for (String sub : List.of(CHANGES_DIR, TeamProject.MEMBERS_DIR)) {
            Path dir = projectDir.resolve(sub);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dir)) {
                for (Path file : files) {
                    parts.add(sub + "/" + file.getFileName() + ":" + Files.size(file) + ":"
                            + Files.getLastModifiedTime(file).toMillis());
                }
            }
        }
        parts.sort(null);
        return String.join("\n", parts);
    }

    private static int readFile(Path file, List<TeamEvent> out) throws IOException {
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        int unreadable = 0;
        for (String line : text.split("\r?\n")) {
            if (line.isBlank()) {
                continue;
            }
            try {
                out.add(TeamEventCodec.fromLine(line));
            } catch (IllegalArgumentException e) {
                unreadable++;
            }
        }
        return unreadable;
    }

    /**
     * 自分のファイルの末尾に 1 行足し、ディスクへ書き切ってから戻る。
     * 前回の書き込みが途中で切れていたら、次の行とつながらないよう改行を補う。
     */
    public static void append(Path projectDir, String deviceId, TeamEvent event) throws IOException {
        appendAll(projectDir, deviceId, List.of(event));
    }

    /**
     * 複数の記録をまとめて 1 回で書く。共有ドライブはファイルを開いて書き切るたびに時間がかかるので、
     * 取り込みのように数百件を書くときは 1 件ずつより大幅に速い。
     */
    public static void appendAll(Path projectDir, String deviceId, List<TeamEvent> events) throws IOException {
        if (events.isEmpty()) {
            return;
        }
        StringBuilder text = new StringBuilder();
        for (TeamEvent event : events) {
            if (!deviceId.equals(event.deviceId())) {
                throw new IllegalArgumentException("他の端末の記録は書けません: " + event.deviceId());
            }
            text.append(TeamEventCodec.toLine(event)).append('\n');
        }
        Path file = logFile(projectDir, deviceId);
        Files.createDirectories(file.getParent());
        byte[] line = text.toString().getBytes(StandardCharsets.UTF_8);
        synchronized (TeamChangeLog.class) {
            try (FileChannel lock = FileChannel.open(localLockFile(deviceId),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = lock.lock()) {
                appendLine(file, line);
            }
        }
    }

    /**
     * 同じ PC で TaskNavi を 2 つ起動すると、同じファイルに同時に書きに行く。
     * 共有ドライブ上のファイルはロックが効くとは限らないので、ロックは PC 内のファイルで取る。
     */
    private static Path localLockFile(String deviceId) {
        return Path.of(System.getProperty("java.io.tmpdir"), "tasknavi-team-" + deviceId + ".lock");
    }

    private static void appendLine(Path file, byte[] line) throws IOException {
        try (FileChannel channel = FileChannel.open(file,
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            long size = channel.size();
            ByteBuffer out;
            if (size > 0 && !endsWithNewline(channel, size)) {
                out = ByteBuffer.allocate(line.length + 1).put((byte) '\n').put(line);
                out.flip();
            } else {
                out = ByteBuffer.wrap(line);
            }
            channel.position(size);
            while (out.hasRemaining()) {
                channel.write(out);
            }
            channel.force(true);
        }
    }

    private static boolean endsWithNewline(FileChannel channel, long size) throws IOException {
        ByteBuffer last = ByteBuffer.allocate(1);
        channel.read(last, size - 1);
        return last.get(0) == '\n';
    }
}
