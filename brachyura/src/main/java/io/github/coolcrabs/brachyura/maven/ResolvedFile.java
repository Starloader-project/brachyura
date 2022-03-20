package io.github.coolcrabs.brachyura.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.coolcrabs.brachyura.dependency.FileDependency;
import io.github.coolcrabs.brachyura.util.MessageDigestUtil;

public class ResolvedFile {

    @Nullable // Null demonstrates cache repository
    public final MavenRepository repo;

    private byte @Nullable[] data;

    @Nullable
    private Path cachePath;

    public ResolvedFile(@Nullable MavenRepository repo, byte[] data) {
        this.repo = repo;
        this.data = data;
    }

    public ResolvedFile(@Nullable MavenRepository repo, @NotNull Path cachePath) {
        this.repo = repo;
        this.cachePath = cachePath;
    }

    void setCachePath(Path cachePath) {
        this.cachePath = cachePath;
    }

    @Nullable
    Path getCachePath() {
        return this.cachePath;
    }

    public byte @NotNull[] getData() {
        byte[] data = this.data;
        if (data != null) {
            return data;
        }
        Path cachePath = this.cachePath;
        if (cachePath == null) {
            throw new NullPointerException();
        }
        try {
            this.data = data = Files.readAllBytes(cachePath);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read data.");
        }
        if (data == null) {
            throw new NullPointerException();
        }
        return data;
    }

    /**
     * Obtains the {@link MessageDigestUtil#SHA1 SHA1 Message Digest} of the content data of this object.
     * It represents it as a hexadecimal string and is not cached.
     *
     * @return The sha1 hash of the data array of this object.
     */
    @NotNull
    public String getSHA1MessageDigest() {
        return MessageDigestUtil.toHexHash(MessageDigestUtil.messageDigest(MessageDigestUtil.SHA1).digest(getData()));
    }

    /**
     * Converts the resolved file to a {@link FileDependency}. As this relies on a path being provided,
     * this method may throw an {@link IllegalStateException} if this object was obtained from a class other than the
     * {@link MavenResolver}.
     */
    @NotNull
    public FileDependency asFileDependency() {
        Path cachePath = this.cachePath;
        if (cachePath == null) {
            throw new IllegalStateException("Path not known.");
        }
        return new FileDependency(cachePath);
    }
}
