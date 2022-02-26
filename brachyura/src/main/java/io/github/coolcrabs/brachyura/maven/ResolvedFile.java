package io.github.coolcrabs.brachyura.maven;

import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.coolcrabs.brachyura.util.MessageDigestUtil;

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

    /**
     * Obtains the {@link MessageDigestUtil#SHA1 SHA1 Message Digest} of the content data of this object.
     * It represents it as a hexadecimal string and is not cached.
     *
     * @return The sha1 hash of the data array of this object.
     */
    @NotNull
    public String getSHA1MessageDigest() {
        return MessageDigestUtil.toHexHash(MessageDigestUtil.messageDigest(MessageDigestUtil.SHA1).digest(data));
    }
}
