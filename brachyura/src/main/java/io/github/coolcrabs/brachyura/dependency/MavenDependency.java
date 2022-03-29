package io.github.coolcrabs.brachyura.dependency;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.maven.MavenId;

public interface MavenDependency extends Dependency {

    /**
     * Obtains the {@link MavenId} that represents from which maven artifact this dependency comes from.
     * Due to backwards compatibility, this method may throw a {@link NullPointerException} if the class is
     * {@link JavaJarDependency} and if the maven id was not properly declared there.
     *
     * @return The maven id of the scope
     */
    @NotNull
    @Contract(pure = true, value = "-> !null")
    public MavenId getMavenId();

    /**
     * Obtains the scope of the dependency.
     * The returned value is by default not used by the compiler - only by the publish task.
     * This means that {@link MavenDependencyScope#COMPILE} will not shade the dependency into the resulting jar,
     * unless external APIs are at play. Unlike maven or gradle, this includes the {@link MavenDependencyScope#TEST},
     * which is also added to the compile and runtime classpath.
     *
     * <p>This behaviour may change in a later revision.
     *
     * @return The scope of this maven artifact
     */
    @NotNull
    @Contract(pure = true, value = "-> !null")
    public MavenDependencyScope getScope();
}
