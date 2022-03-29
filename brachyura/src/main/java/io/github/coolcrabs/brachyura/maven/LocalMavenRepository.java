package io.github.coolcrabs.brachyura.maven;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import io.github.coolcrabs.brachyura.maven.publish.PublicationId;
import io.github.coolcrabs.brachyura.maven.publish.PublishRepository;

/**
 * A simple implementation of the {@link MavenRepository} class that makes use of flatfile storage
 * to store artifacts. To conserve disk space artifacts resolved via this class are not copied over
 * to the resolver's cache folder and the resolver will symlink the cache path to the resolved file.
 *
 * <p>In environments where symbolic links are not supported, the file will be copied over either way however.
 * <p>Furthermore this implementation does not check for checksums, i. e. all files are ultimately trusted.
 * <p>This class can also act as a {@link PublishRepository}.
 * For {@link PublishRepository#publish(io.github.coolcrabs.brachyura.maven.publish.PublicationId, Path)}
 * the file is copied over using {@link Files#copy(Path, Path, java.nio.file.CopyOption...)}. Symlinks are not
 * used for publications. For all publishing tasks, already existing files are overwritten.
 */
public class LocalMavenRepository extends MavenRepository implements PublishRepository {

    @NotNull
    private final Path root;

    public LocalMavenRepository(@NotNull Path root) {
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Root is not a folder!");
        }
        this.root = root;
    }

    @Override
    public void publish(@NotNull PublicationId id, byte @NotNull [] source) throws IOException {
        Path target = root.resolve(id.toPath());
        Logger.info("Publishing " + id.toString() + " to " + target.toAbsolutePath().toString());
        Files.write(target, source);
    }

    @Override
    public void publish(@NotNull PublicationId id, @NotNull InputStream source) throws IOException {
        Path target = root.resolve(id.toPath());
        Logger.info("Publishing " + id.toString() + " to " + target.toAbsolutePath().toString());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void publish(@NotNull PublicationId id, @NotNull Path source) throws IOException {
        Path target = root.resolve(id.toPath());
        Logger.info("Installing " + id.toString() + " (" + source.toAbsolutePath().toString() + ") to " + target.toAbsolutePath().toString());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
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
