package io.github.coolcrabs.brachyura.maven.publish;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.DigestInputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import io.github.coolcrabs.brachyura.util.NetUtil;
import io.github.coolcrabs.brachyura.util.PathUtil;

/**
 * Basically a port of the old AuthentificatedMaven feature.
 * No guarantees are made on whether this works in the first place, since I do not make
 * use of such maven repositories myself.
 */
public class AuthentificatedMavenPublishRepository implements PublishRepository {

    /**
     * Obtains an {@link AuthentificatedMavenPublishRepository} based on the environment variables.
     * The base URL of the repository is defined by BRACHYURA_PUBLISH_MAVEN, the username is defined by
     * BRACHYURA_PUBLISH_USERNAME and the password by BRACHYURA_PUBLISH_PASSWORD.
     *
     * @return The newly created instance
     */
    @NotNull
    @Contract(pure = true, value = "-> new")
    public static PublishRepository fromEnvironmentVariables() {
        return new AuthentificatedMavenPublishRepository(
                System.getenv("BRACHYURA_PUBLISH_MAVEN"),
                System.getenv("BRACHYURA_PUBLISH_USERNAME"),
                System.getenv("BRACHYURA_PUBLISH_PASSWORD")
        );
    }
    private final String mavenURL;
    private final String password;

    private final String username;

    public AuthentificatedMavenPublishRepository(String baseURL, String username, String password) {
        this.mavenURL = baseURL;
        this.username = username;
        this.password = password;
    }

    @Override
    public void publish(@NotNull PublicationId id, byte @NotNull [] source) throws IOException {
        publish(id, new ByteArrayInputStream(source));
    }

    @Override
    public void publish(@NotNull PublicationId id, @NotNull InputStream source) throws IOException {
        String trailSlashRepo;
        String mavenRepoUrl = mavenURL;
        if (mavenRepoUrl.codePointBefore(mavenRepoUrl.length()) == '/') {
            trailSlashRepo = mavenRepoUrl;
        } else {
            trailSlashRepo = mavenRepoUrl + '/';
        }
        String baseFileName = id.toPath().toString();
        URI basePath = null;
        try {
            trailSlashRepo = trailSlashRepo.replace(File.separatorChar, '/'); // Ensure consistency with windows
            basePath = new URI(trailSlashRepo);
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid URI: " + trailSlashRepo + ".", ex);
        }

        try (DigestInputStream digestInSha256 = new DigestInputStream(source, DigestUtils.getSha256Digest())) {
            try (DigestInputStream digestInMd5 = new DigestInputStream(digestInSha256, DigestUtils.getMd5Digest())) {
                try (DigestInputStream digestInSha1 = new DigestInputStream(digestInMd5, DigestUtils.getSha1Digest())) {
                    URL url = basePath.resolve(baseFileName).toURL();
                    Logger.info("Publishing " + url.toString());
                    NetUtil.put(url, digestInSha1, username, password);
                    byte[] checksum = Hex.encodeHexString(digestInMd5.getMessageDigest().digest()).getBytes(StandardCharsets.UTF_8);
                    url = basePath.resolve(baseFileName + ".sha1").toURL();
                    NetUtil.put(url, new ByteArrayInputStream(checksum), username, password);
                }
                byte[] checksum = Hex.encodeHexString(digestInMd5.getMessageDigest().digest()).getBytes(StandardCharsets.UTF_8);
                URL checksumURL = basePath.resolve(baseFileName + ".md5").toURL();
                NetUtil.put(checksumURL, new ByteArrayInputStream(checksum), username, password);
            }
            byte[] checksum = Hex.encodeHexString(digestInSha256.getMessageDigest().digest()).getBytes(StandardCharsets.UTF_8);
            URL checksumURL = basePath.resolve(baseFileName + ".sha256").toURL();
            NetUtil.put(checksumURL, new ByteArrayInputStream(checksum), username, password);
        }
    }

    @Override
    public void publish(@NotNull PublicationId id, @NotNull Path source) throws IOException {
        publish(id, PathUtil.inputStream(source));
    }
}
