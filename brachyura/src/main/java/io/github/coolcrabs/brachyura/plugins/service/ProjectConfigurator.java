package io.github.coolcrabs.brachyura.plugins.service;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.maven.HttpMavenRepository;
import io.github.coolcrabs.brachyura.maven.LocalMavenRepository;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.maven.MavenResolver;
import io.github.coolcrabs.brachyura.project.EntryGlobals;
import io.github.coolcrabs.brachyura.project.Project;
import io.github.coolcrabs.brachyura.util.NetUtil;

public interface ProjectConfigurator<T extends Project> {

    public static enum DependencyActionType {
        SHADE,
        /**
         * Include the dependency jar within the compiled jar.
         * This concept is only sensical in fabric/fabric-like world and is as such not supported
         * for other platforms.
         *
         * Using this to define a dependency in a simple java project will cause an exception to be thrown.
         */
        JAR_IN_JAR,
        COMPILETIME_ONLY;
    }

    /**
     * Creates the instance of the Project configured to correspond to the current state of the configurator.
     * Callers of this method should consider the invocation of this method to result in the invalidation
     * of the configurator. Reconfiguring the configurator after invoking this method may lead to unspecified behaviour
     * that should be avoided at all cost.
     *
     * @return The configured {@link Project} instance.
     */
    @NotNull
    @Contract(pure = true, value = "-> new")
    public T build();

    /**
     * Configures the java version and returns the current instance.
     *
     * @param version The java version to use to compile the project with.
     * @return The current {@link ProjectConfigurator} instance
     */
    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public ProjectConfigurator<T> javaVersion(int version);

    @SuppressWarnings("null")
    @NotNull
    @Contract(mutates = "this", pure = false, value = "_, _, _, _, _ -> this")
    public default ProjectConfigurator<T> withDependencies(boolean transitive, DependencyActionType action, boolean searchLocally, MavenId[] deps, String[] repositories) {
        MavenResolver resolver = new MavenResolver(EntryGlobals.getProjectDir().resolve(".brachyura/.cache"));
        if (searchLocally) {
            resolver.addRepository(new LocalMavenRepository(MavenResolver.MAVEN_LOCAL));
        }
        for (String repo : repositories) {
            if (repo.startsWith("https://") || repo.startsWith("http://")) {
                resolver.addRepository(new HttpMavenRepository(repo));
            } else if (repo.startsWith("file://")) {
                resolver.addRepository(new LocalMavenRepository(Paths.get(NetUtil.uri(repo))));
            } else {
                throw new IllegalStateException("Repository \"" + repo + "\" uses unknown/unsupported protocol!");
            }
        }

        if (transitive) {
            for (MavenId mavenId : deps) {
                withDependencies(action, resolver.getTransitiveDependencies(mavenId));
            }
        } else {
            for (MavenId mavenId : deps) {
                withDependencies(action, resolver.getJarDepend(mavenId));
            }
        }

        return this;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_, _, _ -> this")
    public default ProjectConfigurator<T> withDependencies(boolean transitive, DependencyActionType action, MavenId[] deps) {
        return withDependencies(transitive, action, true, deps, new String[] {MavenResolver.MAVEN_CENTRAL});
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_, _, _ -> this")
    public ProjectConfigurator<T> withDependencies(DependencyActionType action, Collection<JavaJarDependency> deps);

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_, _, _ -> this")
    public default ProjectConfigurator<T> withDependencies(DependencyActionType action, JavaJarDependency... deps) {
        return withDependencies(action, Arrays.asList(deps));
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_, _, _ -> this")
    public default ProjectConfigurator<T> withDependencies(JavaJarDependency... deps) {
        return withDependencies(DependencyActionType.COMPILETIME_ONLY, deps);
    }
}
