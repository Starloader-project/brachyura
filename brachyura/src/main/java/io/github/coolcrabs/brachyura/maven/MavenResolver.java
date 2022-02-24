package io.github.coolcrabs.brachyura.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;

public class MavenResolver {

    @NotNull
    private final List<MavenRepository> repositories = new ArrayList<>();
    @NotNull
    private final Path cacheFolder;

    public MavenResolver(@NotNull Path cacheFolder) {
        this.cacheFolder = cacheFolder;
    }

    public void addRepository(@NotNull MavenRepository repository) {
        this.repositories.add(repository);
    }

    public void addRepositories(@NotNull Collection<MavenRepository> repositories) {
        this.repositories.addAll(repositories);
    }

    @NotNull
    public ResolvedFile resolveArtifact(@NotNull MavenId artifact, @NotNull String classifier, @NotNull String extension) throws IOException {
        String folder = artifact.groupId.replace('.', '/') + '/' + artifact.artifactId + '/' + artifact.version + '/';
        String nameString;
        if (classifier.isEmpty()) {
            nameString = artifact.artifactId + '-' + artifact.version + '.' + extension;
        } else {
            nameString = artifact.artifactId + '-' + artifact.version + '-' + classifier + '.' + extension;
        }
        ResolvedFile directFile = resolveFileContents(folder, nameString);
        if (directFile != null) {
            return directFile;
        }

        ResolvedFile mavenMeta = resolveFileContents(folder, "maven-metadata.xml");
        if (mavenMeta != null) {
            // TODO evaluate using the maven meta
            throw new UnsupportedOperationException("Not yet supported!");
        } else {
            throw new IOException("Unable to resolve artifact: maven-metadata.xml is missing and the file"
                    + " was not able to be fetched directly.");
        }
    }

    @Nullable
    private ResolvedFile resolveFileContents(@NotNull String folder, @NotNull String file) {
        Path cacheFile = cacheFolder.resolve(folder).resolve(file);
        if (Files.exists(cacheFile)) {
            try {
                ResolvedFile f = new ResolvedFile(null, Files.readAllBytes(cacheFile));
                f.setCachePath(cacheFile);
                return f;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (MavenRepository repo : repositories) {
            ResolvedFile resolved = repo.resolve(folder, file);
            if (resolved != null) {
                try {
                    Files.write(cacheFile, resolved.data, StandardOpenOption.CREATE_NEW);
                    resolved.setCachePath(cacheFile);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to write to cache", e);
                }
                return resolved;
            }
        }
        return null;
    }

    @Nullable
    public JavaJarDependency getJarDepend(@NotNull MavenId artifact) {
        Path noSourcesCacheFile;
        {
            String folder = artifact.groupId.replace('.', '/') + '/' + artifact.artifactId + '/' + artifact.version + '/';
            String file = artifact.artifactId + '-' + artifact.version + "nosources";
            noSourcesCacheFile = cacheFolder.resolve(folder).resolve(file);
        }
        ResolvedFile sources = null;
        if (Files.notExists(noSourcesCacheFile)) {
            try {
                sources = resolveArtifact(artifact, "sources", "jar");
            } catch (Exception exception1) {
                try {
                    Files.createFile(noSourcesCacheFile);
                } catch (IOException exception2) {
                    IllegalStateException ex = new IllegalStateException("Cannot cache sources", exception2);
                    ex.addSuppressed(exception1);
                    throw ex;
                }
            }
        }
        try {
            Path sourcesPath = null;
            if (sources != null) {
                sourcesPath = sources.getCachePath();
            }
            ResolvedFile resolvedJar = resolveArtifact(artifact, "", "jar");
            Path jarPath = resolvedJar.getCachePath();
            if (jarPath == null) {
                return null;
            }
            return new JavaJarDependency(jarPath, sourcesPath, artifact);
        } catch (Exception e) {
            return null;
        }
    }
}
