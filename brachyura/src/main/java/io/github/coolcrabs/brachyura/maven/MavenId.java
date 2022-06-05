package io.github.coolcrabs.brachyura.maven;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MavenId {
    @NotNull
    public final String groupId;
    @NotNull
    public final String artifactId;
    @NotNull
    public final String version;
    public final @Nullable String classifier;

    public MavenId(@NotNull String maven) {
        String[] a = maven.split(":");
        if (a.length < 3 || a.length > 4) { // Slbrachyura: Style change
            throw new IllegalArgumentException("Bad maven id " + maven); // Slbrachyura: Let's not use random classes for exceptions
        }

        String groupId = a[0];
        String artifactId = a[1];
        String version = a[2];
        if (groupId == null || artifactId == null || version == null) {
            throw new IllegalStateException("Array elements returned by String#split returned null.");
        }
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;

        this.classifier = a.length == 4 ? a[3] : null;
    }

    public MavenId(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) { // Slbrachyura: Nullability
        this(groupId, artifactId, version, null);
    }

    public MavenId(@NotNull String groupId, @NotNull String artifactId, @NotNull String version, @Nullable String classifier) { // Slbrachyura: Nullability
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, classifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof MavenId) {
            MavenId mavenId = (MavenId)obj;
            return this.groupId.equals(mavenId.groupId) && this.artifactId.equals(mavenId.artifactId) && this.version.equals(mavenId.version) && Objects.equals(this.classifier, mavenId.classifier);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        if (classifier != null) {
            return groupId + ":" + artifactId + ":" + version + ":" + classifier;
        }
        return groupId + ":" + artifactId + ":" + version;
    }
}
