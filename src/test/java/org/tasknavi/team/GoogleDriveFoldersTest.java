package org.tasknavi.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoogleDriveFoldersTest {

    @TempDir
    Path root;

    @Test
    void findsMyDriveWhenTheAccountHasNoSharedDrives() throws Exception {
        Path c = Files.createDirectories(root.resolve("c"));
        Path g = Files.createDirectories(root.resolve("g"));
        Path myDrive = Files.createDirectories(g.resolve("マイドライブ"));
        List<Path> roots = List.of(c, g);

        assertEquals(Optional.empty(), GoogleDriveFolders.find(roots, GoogleDriveFolders.SHARED_DRIVES));
        assertEquals(Optional.of(myDrive), GoogleDriveFolders.find(roots, GoogleDriveFolders.MY_DRIVE));

        Path shared = Files.createDirectories(g.resolve("Shared drives"));
        assertEquals(Optional.of(shared), GoogleDriveFolders.find(roots, GoogleDriveFolders.SHARED_DRIVES));
    }
}
