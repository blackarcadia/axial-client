package org.example.launcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Shared by every writer of the game payload, including separate launcher processes. */
final class ClientInstallation {
    private ClientInstallation() {}

    @FunctionalInterface
    interface Operation<T> {
        T run() throws IOException;
    }

    static synchronized <T> T locked(Path gameDir, Operation<T> operation) throws IOException {
        Files.createDirectories(gameDir);
        try (FileChannel channel = FileChannel.open(gameDir.resolve(".client-install.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            return operation.run();
        }
    }

    static long minimumBuild(Path gameDir) throws IOException {
        return Math.max(recordedBuild(gameDir), GitHubReleaseUpdater.buildNumber(
                GitHubReleaseUpdater.installedClientBuild(gameDir)));
    }

    private static long recordedBuild(Path gameDir) throws IOException {
        Path record = gameDir.resolve(".client-minimum-build");
        try {
            long build = Long.parseLong(Files.readString(record).trim());
            if (build < 0) throw new NumberFormatException();
            return build;
        } catch (NoSuchFileException missing) {
            return -1;
        } catch (NumberFormatException failure) {
            throw new IOException("Client version record is damaged; refusing an unsafe fallback", failure);
        }
    }

    // Call under the installation lock. The record can only move forward.
    static void recordBuild(Path gameDir, long build) throws IOException {
        if (build < 0 || build <= recordedBuild(gameDir)) return;
        Path temp = Files.createTempFile(gameDir, ".client-build-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                ByteBuffer bytes = StandardCharsets.UTF_8.encode(build + "\n");
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            try {
                Files.move(temp, gameDir.resolve(".client-minimum-build"),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, gameDir.resolve(".client-minimum-build"), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    static void requireLaunchable(Path gameDir) throws IOException {
        long installed = GitHubReleaseUpdater.buildNumber(GitHubReleaseUpdater.installedClientBuild(gameDir));
        long minimum = recordedBuild(gameDir);
        if (installed < 0 || installed < minimum) {
            throw new IOException("Client build " + minimum
                    + " needs repair. Refusing to launch an older or unreadable client; retry the update.");
        }
        recordBuild(gameDir, installed);
    }
}
