package io.github.coolcrabs.brachyura.dependency;

import java.nio.file.Path;
import java.util.Objects;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.coolcrabs.brachyura.maven.MavenId;

public class JavaJarDependency implements Dependency, MavenDependency {
    @NotNull
    public final Path jar;

    /**
     * The base location of the javadocs that document this dependency.
     */
    @Nullable
    private final String javadocUrl;

    @NotNull
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

    /**
     * The scope of this dependency.
     */
    @NotNull
    private final MavenDependencyScope scope;

    @Nullable
    public final Path sourcesJar;

    public JavaJarDependency(@NotNull Path jar, @Nullable Path sourcesJar, @NotNull MavenId mavenId) {
        this(jar, sourcesJar, mavenId, null, null, MavenDependencyScope.COMPILE, null);
    }

    public JavaJarDependency(@NotNull Path jar, @Nullable Path sourcesJar, @NotNull MavenId mavenId,
            @Nullable Path eclipseExternalAnnotatiosn, @Nullable Path intelliJExternalAnnotations,
            @NotNull MavenDependencyScope scope, @Nullable String javadocURL) {
        this.jar = Objects.requireNonNull(jar, "jar may not be null!");
        this.sourcesJar = sourcesJar;
        this.mavenId = mavenId;
        this.eclipseExternalAnnotations = eclipseExternalAnnotatiosn;
        this.intelliJExternalAnnotations = intelliJExternalAnnotations;
        this.scope = scope;
        this.javadocUrl = javadocURL;
    }

    /**
     * Obtains the base location of the javadocs that document this dependency.
     * An example value is "https://docs.oracle.com/en/java/javase/17/docs/api/".
     * More strictly speaking appending "index.html" or "element-list" should give
     * a featchable URL. This method can return null in case of the dependency not having an online javadoc.
     *
     * <p>This value is used by the IDE to display javadocs (though most IDEs also generate javadocs from attached source files)
     * or to link to other javadoc sites while generating javadocs.
     *
     * @return A {@link String} defining the location of the (online) javadocs.
     */
    @Nullable
    @Contract(pure = true, value = "-> _")
    public String getJavadocURL() {
        return javadocUrl;
    }

    @Override
    @NotNull
    @Contract(pure = true, value = "-> !null")
    public MavenId getMavenId() {
        return this.mavenId;
    }

    @Override
    @NotNull
    @Contract(pure = true, value = "-> !null")
    public MavenDependencyScope getScope() {
        return this.scope;
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
        return new JavaJarDependency(this.jar, this.sourcesJar, this.mavenId, eclipse, intelliJ, this.scope, this.javadocUrl);
    }

    /**
     * Returns a <b>clone</b> of this {@link JavaJarDependency} with the javadoc URL set.
     * If it is already set then the value is overwritten, even if the argument of this method is null.
     * An example value is "https://docs.oracle.com/en/java/javase/17/docs/api/".
     * More strictly speaking appending "index.html" or "element-list" should give
     * a featchable URL. The value can be null in case of the dependency not having an online javadoc.
     *
     * <p>The javadoc location is used by the IDE to display javadocs (though most IDEs automatically generate javadocs
     * from attached source files) or to link to other javadoc sites while generating javadocs.
     *
     * @param javadocUrl A {@link String} defining the location of the (online) javadocs.
     * @return A clone with the javadoc URL set
     */
    @NotNull
    @Contract(pure = true, value = "_ -> new")
    public JavaJarDependency withJavadocURL(@Nullable String javadocUrl) {
        return new JavaJarDependency(this.jar, this.sourcesJar, this.mavenId,
                this.eclipseExternalAnnotations, this.intelliJExternalAnnotations, this.scope, javadocUrl);
    }
}
