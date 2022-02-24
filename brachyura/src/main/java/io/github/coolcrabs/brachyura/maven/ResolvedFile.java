package io.github.coolcrabs.brachyura.maven;

import java.nio.file.Path;

import org.jetbrains.annotations.Nullable;

public class ResolvedFile {

    @Nullable // Null demonstrates cache repository
    public final MavenRepository repo;

    public final byte[] data;

    @Nullable
    private Path cachePath;

    public ResolvedFile(@Nullable MavenRepository repo, byte[] data) {
        this.repo = repo;
        this.data = data;
    }

    void setCachePath(Path cachePath) {
        this.cachePath = cachePath;
    }

    @Nullable
    Path getCachePath() {
        return this.cachePath;
    }
}
