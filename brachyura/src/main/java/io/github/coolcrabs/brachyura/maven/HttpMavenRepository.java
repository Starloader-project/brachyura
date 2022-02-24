package io.github.coolcrabs.brachyura.maven;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HttpMavenRepository extends MavenRepository {

    private static final HttpClient client = HttpClientBuilder.create().build();
    private final String repoUrl;

    public HttpMavenRepository(@NotNull String repoUrl) {
        if (repoUrl.codePointBefore(repoUrl.length()) != '/') {
            repoUrl = repoUrl + '/';
        }
        this.repoUrl = repoUrl;
    }

    @Override
    @Nullable
    public ResolvedFile resolve(@NotNull String folder, @NotNull String file) {
        StringBuilder locationBuilder = new StringBuilder(this.repoUrl.length() + folder.length() + file.length() + 1);
        locationBuilder.append(this.repoUrl).append(folder);
        if (folder.codePointBefore(folder.length()) != '/') {
            locationBuilder.append('/');
        }
        locationBuilder.append(file);
        HttpGet request = new HttpGet(locationBuilder.toString());
        try {
            HttpResponse response = HttpMavenRepository.client.execute(request);
            if ((response.getStatusLine().getStatusCode() / 100) != 2) {
                return null;
            }
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            response.getEntity().writeTo(byteOut);
            return new ResolvedFile(this, byteOut.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

}
