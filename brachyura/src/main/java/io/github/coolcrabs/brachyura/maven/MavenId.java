package io.github.coolcrabs.brachyura.maven;

import java.security.InvalidParameterException;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public final class MavenId {
    @NotNull
    public final String groupId;
    @NotNull
    public final String artifactId;
    @NotNull
    public final String version;

    public MavenId(@NotNull String maven) {
        String[] a = maven.split(":");
        if (a.length != 3) {
            throw new InvalidParameterException("Bad maven id " + maven);
        }
        String groupId = a[0];
        String artifactId = a[1];
        String version = a[2];
        if (groupId == null || artifactId == null || version == null) {
            throw new InternalError("Array elements returned by String#split returned null.");
        }
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public MavenId(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof MavenId) {
            MavenId mavenId = (MavenId)obj;
            return this.groupId.equals(mavenId.groupId) && this.artifactId.equals(mavenId.artifactId) && this.version.equals(mavenId.version);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
