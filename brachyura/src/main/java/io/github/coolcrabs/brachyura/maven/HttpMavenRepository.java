package io.github.coolcrabs.brachyura.maven;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import io.github.coolcrabs.brachyura.exception.ChecksumViolationException;

public final class HttpMavenRepository extends MavenRepository {

    private static final HttpClient client = HttpClientBuilder
            .create()
            .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)
            .setConnectionManagerShared(true)
            .build();

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
        HttpGet request = new HttpGet(location);
        try {
            HttpResponse response = HttpMavenRepository.client.execute(request);
            if ((response.getStatusLine().getStatusCode() / 100) != 2) {
                request.abort();
                return null;
            }
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            response.getEntity().writeTo(byteOut);
            Logger.info("Downloaded " + location);
            return new ResolvedFile(this, byteOut.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    @Nullable
    public ResolvedFile resolve(@NotNull String folder, @NotNull String file) {
        StringBuilder locationBuilder = new StringBuilder(this.repoUrl.length() + folder.length() + file.length() + 6);
        locationBuilder.append(this.repoUrl).append(folder);
        if (folder.codePointBefore(folder.length()) != '/') {
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
            String actualChecksum = new String(checksumFile.data, StandardCharsets.UTF_8);
            if (!actualChecksum.equalsIgnoreCase(dataChecksum)) {
                throw new ChecksumViolationException(dataChecksum, actualChecksum, originalFileLocation);
            }
        }
        return original;
    }

    public void setCheckingChecksums(boolean value) {
        this.checksums = value;
    }
}
