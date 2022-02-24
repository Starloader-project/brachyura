package io.github.coolcrabs.brachyura.dependency;

import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.coolcrabs.brachyura.maven.MavenId;

public class JavaJarDependency implements Dependency {
    @NotNull
    public final Path jar;

    @Nullable
    public final Path sourcesJar;

    @Nullable
    public final MavenId mavenId;

    public JavaJarDependency(Path jar, @Nullable Path sourcesJar, @Nullable MavenId mavenId) {
        if (jar == null) {
            throw new NullPointerException("jar may not be null!");
        }
        this.jar = jar;
        this.sourcesJar = sourcesJar;
        this.mavenId = mavenId;
    }
}
