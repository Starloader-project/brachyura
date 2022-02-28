package io.github.coolcrabs.brachyura.dependency;

import java.nio.file.Path;

import org.jetbrains.annotations.Contract;
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

    /**
     * Path to the archive used for EEA. This in turn is used by eclipse to evaluate
     * the nullness of things.
     */
    @Nullable
    public final Path eclipseExternalAnnotations;

    /**
     * The path to the archive containing IntelliJ's external annotations.
     * This in turn is used by the IDE to evaluate the nullness of things.
     */
    @Nullable
    public final Path intelliJExternalAnnotations;

    public JavaJarDependency(Path jar, @Nullable Path sourcesJar, @Nullable MavenId mavenId,
            @Nullable Path eclipseExternalAnnotatiosn, @Nullable Path intelliJExternalAnnotations) {
        if (jar == null) {
            throw new NullPointerException("jar may not be null!");
        }
        this.jar = jar;
        this.sourcesJar = sourcesJar;
        this.mavenId = mavenId;
        this.eclipseExternalAnnotations = eclipseExternalAnnotatiosn;
        this.intelliJExternalAnnotations = intelliJExternalAnnotations;
    }

    public JavaJarDependency(Path jar, @Nullable Path sourcesJar, @Nullable MavenId mavenId) {
        this(jar, sourcesJar, mavenId, null, null);
    }

    /**
     * Returns a <b>clone</b> of this {@link JavaJarDependency} with {@link #eclipseExternalAnnotations}
     * and {@link #intelliJExternalAnnotations} set. If they are already set then they are overwritten,
     * even if the arguments of this method is null.
     *
     * @param eclipse The path to the archive containing the EEA files
     * @param intelliJ The path to the archive containing IntelliJ's external annotations
     * @return A clone with the external annotation field set
     */
    @NotNull
    @Contract(pure = true, value = "_, _ -> new")
    public JavaJarDependency withExternalAnnotations(@Nullable Path eclipse, @Nullable Path intelliJ) {
        return new JavaJarDependency(this.jar, this.sourcesJar, this.mavenId, eclipse, intelliJ);
    }
}
