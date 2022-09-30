package io.github.coolcrabs.brachyura.maven;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import io.github.coolcrabs.brachyura.maven.publish.PublicationId;
import io.github.coolcrabs.brachyura.maven.publish.PublishRepository;
import io.github.coolcrabs.brachyura.util.MessageDigestUtil;
import io.github.coolcrabs.brachyura.util.PathUtil;

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
            try {
                if (!Files.notExists(root) || !Files.exists(Files.createDirectories(root))) {
                    throw new IllegalStateException("Root (" + root.toAbsolutePath() + ") is not a folder!");
                }
            } catch (IOException e) {
                throw new IllegalStateException("Root (" + root.toAbsolutePath() + ") is not a folder!", e);
            }
        }
        this.root = root;
    }

    @Override
    public void publish(@NotNull PublicationId id, byte @NotNull [] source) throws IOException {
        publish(id, new ByteArrayInputStream(source));
    }

    @Override
    public void publish(@NotNull PublicationId id, @NotNull InputStream source) throws IOException {
        Path target = root.resolve(id.toPath());
        Files.createDirectories(target.getParent());
        Logger.info("Publishing " + id.toString() + " to " + target.toAbsolutePath().toString());
        try (DigestInputStream digestInSha256 = new DigestInputStream(source, MessageDigestUtil.messageDigest("SHA-256"))) {
            try (DigestInputStream digestInMd5 = new DigestInputStream(digestInSha256,  MessageDigestUtil.messageDigest("MD5"))) {
                try (DigestInputStream digestInSha1 = new DigestInputStream(digestInMd5,  MessageDigestUtil.messageDigest("SHA-1"))) {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    
                    byte[] checksum = MessageDigestUtil.toHexHash(digestInMd5.getMessageDigest().digest()).getBytes(StandardCharsets.UTF_8);
                    Files.write(target.resolveSibling(target.getFileName() + ".sha1"), checksum);
                }
                byte[] checksum = MessageDigestUtil.toHexHash(digestInMd5.getMessageDigest().digest()).getBytes(StandardCharsets.UTF_8);
                Files.write(target.resolveSibling(target.getFileName() + ".md5"), checksum);
            }
            byte[] checksum = MessageDigestUtil.toHexHash(digestInSha256.getMessageDigest().digest()).getBytes(StandardCharsets.UTF_8);
            Files.write(target.resolveSibling(target.getFileName() + ".sha256"), checksum);
        }
    }

    @Override
    public void publish(@NotNull PublicationId id, @NotNull Path source) throws IOException {
        publish(id, PathUtil.inputStream(source));
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
