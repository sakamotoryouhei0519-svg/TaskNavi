package org.tasknavi.team;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Google Drive for desktop がつないだドライブ（既定は G:）の中の、プロジェクトを置ける場所を探す。
 * 「共有ドライブ」はどこかの共有ドライブのメンバーになっていないと現れないので、無ければ「マイドライブ」を使う。
 */
public final class GoogleDriveFolders {

    static final List<String> SHARED_DRIVES = List.of("共有ドライブ", "Shared drives");
    static final List<String> MY_DRIVE = List.of("マイドライブ", "My Drive");

    private GoogleDriveFolders() {
    }

    /** 共有ドライブ → マイドライブの順に、最初に見つかった場所。 */
    public static Optional<Path> preferredLocation() {
        List<Path> roots = Arrays.stream(File.listRoots()).map(File::toPath).toList();
        return find(roots, SHARED_DRIVES).or(() -> find(roots, MY_DRIVE));
    }

    static Optional<Path> find(List<Path> roots, List<String> names) {
        for (Path root : roots) {
            for (String name : names) {
                Path candidate = root.resolve(name);
                if (Files.isDirectory(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }
}
