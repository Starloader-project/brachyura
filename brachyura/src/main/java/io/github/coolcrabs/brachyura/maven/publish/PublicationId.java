package io.github.coolcrabs.brachyura.maven.publish;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.maven.MavenId;

/**
 * Describes the exact nature of the file in closer detail, however a bit unlike the name of the class
 * implies, it does not describe in which repository it is located nor what the content is.
 * Technically it is just an a bit more specific {@link MavenId}.
 */
public class PublicationId {

    @NotNull
    private final String extension;

    @NotNull
    private final MavenId mavenId;

    public PublicationId(@NotNull MavenId mavenId, @NotNull String extension) {
        this.mavenId = mavenId;
        this.extension = extension;
    }

    @NotNull
    @Contract(pure = true, value = "-> !null")
    public String getExtension() {
        return extension;
    }

    @NotNull
    @Contract(pure = true, value = "-> !null")
    public MavenId getMavenId() {
        return mavenId;
    }

    /**
     * Obtains the relative path that is described by the publication id. If it is made absolute,
     * there is no guarantee that there is actually a file at that location.
     *
     * @return The path described by this instance
     */
    @NotNull
    @Contract(pure = true, value = "-> new")
    public Path toPath() {
        StringBuilder filename = new StringBuilder();
        filename.append(mavenId.artifactId.replace('.', File.separatorChar));
        filename.append('-');
        filename.append(mavenId.version);
        String classifier = mavenId.classifier;
        if (classifier != null) {
            filename.append('-');
            filename.append(classifier);
        }
        filename.append('.');
        filename.append(extension);
        return Paths.get(mavenId.groupId.replace('.', File.separatorChar),
                mavenId.artifactId.replace('.', File.separatorChar),
                mavenId.version).resolve(filename.toString());
    }

    @Override
    public String toString() {
        return this.mavenId.groupId + ":" + this.mavenId.artifactId + ":" + this.mavenId.version + "-" + this.mavenId.classifier + "." + this.extension;
    }
}
