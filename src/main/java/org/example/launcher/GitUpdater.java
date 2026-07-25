package org.example.launcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public final class GitUpdater {
    private final Path repoRoot;

    public GitUpdater(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public GitUpdateResult syncIfNeeded() throws IOException, InterruptedException {
        if (!isGitRepo()) {
            return GitUpdateResult.skipped("git repository not available");
        }

        if (isDirty()) {
            return GitUpdateResult.skipped("working tree has local changes");
        }

        String branch = runGitOptional("branch", "--show-current").output().trim();
        GitCommandResult upstreamResult = runGitOptional("rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}");
        String upstream = upstreamResult.output().trim();
        if (upstream.isBlank()) {
            upstream = branch.isBlank() ? "origin/main" : "origin/" + branch;
        }

        String before = runGit("rev-parse", "HEAD").trim();
        GitCommandResult fetchResult = runGitOptional("fetch", "--prune", "origin");
        if (fetchResult.exitCode() != 0) {
            return GitUpdateResult.skipped("no upstream remote configured");
        }

        if (!refExists(upstream)) {
            if (refExists("origin/main")) {
                upstream = "origin/main";
            } else if (refExists("origin/master")) {
                upstream = "origin/master";
            } else {
                return GitUpdateResult.skipped("no upstream remote configured");
            }
        }

        String remoteHead = runGit("rev-parse", upstream).trim();
        if (before.equals(remoteHead)) {
            return GitUpdateResult.upToDate("up to date");
        }

        runGit("pull", "--ff-only");
        String after = runGit("rev-parse", "HEAD").trim();
        if (!before.equals(after)) {
            return GitUpdateResult.updated("update applied");
        }

        return GitUpdateResult.upToDate("up to date");
    }

    private boolean isGitRepo() throws IOException, InterruptedException {
        GitCommandResult result = runGitOptional("rev-parse", "--is-inside-work-tree");
        return result.exitCode() == 0 && "true".equals(result.output().trim());
    }

    private boolean isDirty() throws IOException, InterruptedException {
        GitCommandResult result = runGitOptional("status", "--porcelain");
        return result.exitCode() == 0 && !result.output().isBlank();
    }

    private boolean refExists(String ref) throws IOException, InterruptedException {
        GitCommandResult result = runGitOptional("rev-parse", "--verify", "--quiet", ref);
        return result.exitCode() == 0 && !result.output().isBlank();
    }

    private String runGit(String... args) throws IOException, InterruptedException {
        GitCommandResult result = runGitOptional(args);
        if (result.exitCode() != 0) {
            throw new IOException("git " + String.join(" ", args) + " failed: " + result.output());
        }
        return result.output();
    }

    private GitCommandResult runGitOptional(String... args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(buildCommand(args));
        pb.directory(repoRoot.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (var in = process.getInputStream()) {
            in.transferTo(out);
        }
        int exit = process.waitFor();
        String text = out.toString(StandardCharsets.UTF_8);
        return new GitCommandResult(exit, text);
    }

    private java.util.List<String> buildCommand(String... args) {
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add("git");
        java.util.Collections.addAll(command, args);
        return command;
    }

    private record GitCommandResult(int exitCode, String output) {}

    public record GitUpdateResult(boolean updated, String status) {
        static GitUpdateResult updated(String status) {
            return new GitUpdateResult(true, status);
        }

        static GitUpdateResult upToDate(String status) {
            return new GitUpdateResult(false, status);
        }

        static GitUpdateResult skipped(String status) {
            return new GitUpdateResult(false, status);
        }
    }
}
