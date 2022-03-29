package io.github.coolcrabs.brachyura.dependency;

import io.github.coolcrabs.brachyura.maven.MavenResolver;

/**
 * An enumeration of the valid dependency scopes that can be declared in a project pom.
 */
public enum MavenDependencyScope {

    /**
     * The compile scope. Roughly equivalent to gradle's "api" scope.
     *
     * <p> In theory this means that the jar will be put in the compile and runtime classpath.
     */
    COMPILE,

    /**
     * Not a scope defined officially by maven, however this scope is present in gradle as "compileOnly".
     * This scope is normally only used by the publish task and means that the dependency should not be included
     * in the pom. Useful if the dependency is not present in any repositories.
     *
     * <p> The jar will be present in the compile classpath.
     */
    COMPILE_ONLY,

    /**
     * The provided scope. Roughly equivalent to gradle's "compileOnlyApi" scope.
     *
     * <p> In theory this means that the jar will be put in the compile classpath but will not be present in the runtime
     * classpath because the dependency was already provided by something else. By default the {@link MavenResolver} does not
     * resolve transitive dependencies when this scope is used for that dependency in the pom.xml.
     */
    PROVIDED,

    /**
     * The runtime scope.
     *
     * <p> In theory this means that the jar will not be present in the compile classpath but will be present in the runtime
     * classpath.
     */
    RUNTIME,

    /**
     * The system scope. A useful alternative to this scope is {@link MavenDependencyScope#COMPILE_ONLY}.
     *
     * @deprecated Using the system scope creates headaches and is generally not the right solution.
     * Maven defines this scope as deprecated.
     */
    @Deprecated
    SYSTEM,

    /**
     * The test scope.
     *
     * <p> In theory this means that the jar will be put in neither compile nor runtime classpath, but will be present in the
     * test classpath.
     */
    TEST;
}
