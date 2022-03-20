package io.github.coolcrabs.brachyura.maven;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A simple implementation of the {@link MavenRepository} class that makes use of flatfile storage
 * to store artifacts. To conserve disk space artifacts resolved via this class are not copied over
 * to the resolver's cache folder and the resolver will symlink the cache path to the resolved file.
 *
 * <p>In environments where symbolic links are not supported, the file will be copied over either way however.
 * <p>Furthermore this implementation does not check for checksums, i. e. all files are ultimately trusted.
 */
public class LocalMavenRepository extends MavenRepository {

    @NotNull
    private final Path root;

    public LocalMavenRepository(@NotNull Path root) {
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Root is not a folder!");
        }
        this.root = root;
    }

    @Override
    @Nullable
    public ResolvedFile resolve(@NotNull String folder, @NotNull String file) {
        Path resolvedFile = root.resolve(folder).resolve(file);
        if (!Files.exists(resolvedFile)) {
            return null;
        }
        return new ResolvedFile(this, resolvedFile);
    }
}
