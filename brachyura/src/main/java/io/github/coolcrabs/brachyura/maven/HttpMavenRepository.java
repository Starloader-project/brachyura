package io.github.coolcrabs.brachyura.maven;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.coolcrabs.brachyura.exception.ChecksumViolationException;
import io.github.coolcrabs.brachyura.util.NetUtil;

public final class HttpMavenRepository extends MavenRepository {

    private final String repoUrl;
    private boolean checksums = true;

    public HttpMavenRepository(@NotNull String repoUrl) {
        if (repoUrl.codePointBefore(repoUrl.length()) != '/') {
            repoUrl = repoUrl + '/';
        }
        this.repoUrl = repoUrl;
    }

    public boolean isCheckingChecksums() {
        return this.checksums;
    }

    @Nullable
    private ResolvedFile resolve0(String location) {
        try (InputStream in = NetUtil.inputStream(NetUtil.url(location))) {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            byte[] cache = new byte[4096];
            for (int read = in.read(cache); read != -1; read = in.read(cache)) {
                byteOut.write(cache, 0, read);
            }
            return new ResolvedFile(this, byteOut.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Nullable
    public ResolvedFile resolve(@NotNull String folder, @NotNull String file) {
        StringBuilder locationBuilder = new StringBuilder(this.repoUrl.length() + folder.length() + file.length() + 6);
        locationBuilder.append(this.repoUrl).append(folder);
        if (folder.codePointBefore(folder.length()) != '/' && file.codePointAt(0) != '/') {
            locationBuilder.append('/');
        }
        locationBuilder.append(file);
        String originalFileLocation = locationBuilder.toString();
        ResolvedFile original = resolve0(originalFileLocation);
        if (original != null && checksums) {
            locationBuilder.append(".sha1");
            ResolvedFile checksumFile = resolve0(locationBuilder.toString());
            String dataChecksum = original.getSHA1MessageDigest();
            if (checksumFile == null) {
                throw new ChecksumViolationException(dataChecksum, null, originalFileLocation);
            }
            String actualChecksum = new String(checksumFile.getData(), StandardCharsets.UTF_8).trim().split(" ")[0];
            if (!actualChecksum.equalsIgnoreCase(dataChecksum)) {
                throw new ChecksumViolationException(dataChecksum, actualChecksum, originalFileLocation);
            }
        }
        return original;
    }

    /**
     * Sets whether this instance of the {@link HttpMavenRepository} class should check checksums and report checksum violations.
     * This may be needed in instances where the maven repository is not maintained properly, for example because it is maintained
     * by hand and thus from time to time checksums are omitted. In most cases it is better to report these issues to the maintainer
     * of the repository but this method can act as a band-aid. It should however not be a permanent one.
     *
     * @param value True to check checksums, false to not check them
     * @return Always the current instance of the class, for chaining
     */
    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public HttpMavenRepository setCheckingChecksums(boolean value) {
        this.checksums = value;
        return this;
    }
}
