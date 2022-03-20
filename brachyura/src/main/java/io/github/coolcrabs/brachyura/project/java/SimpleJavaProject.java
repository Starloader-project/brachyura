package io.github.coolcrabs.brachyura.project.java;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.compiler.java.JavaCompilation;
import io.github.coolcrabs.brachyura.compiler.java.JavaCompilationOptions;
import io.github.coolcrabs.brachyura.compiler.java.JavaCompilationResult;
import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.exception.CompilationFailure;
import io.github.coolcrabs.brachyura.ide.IdeModule;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.maven.MavenPublishing;
import io.github.coolcrabs.brachyura.processing.sinks.AtomicZipProcessingSink;
import io.github.coolcrabs.brachyura.processing.sources.DirectoryProcessingSource;
import io.github.coolcrabs.brachyura.processing.sources.ProcessingSponge;
import io.github.coolcrabs.brachyura.project.Task;
import io.github.coolcrabs.brachyura.util.Lazy;
import io.github.coolcrabs.brachyura.util.ThrowingRunnable;

public abstract class SimpleJavaProject extends BaseJavaProject {

    @NotNull
    private final JavaCompilationOptions compileOptions = new JavaCompilationOptions();

    @NotNull
    public final Lazy<@NotNull List<JavaJarDependency>> dependencies = new Lazy<>(this::getDependencies);

    public abstract MavenId getId();

    @NotNull
    public String getJarBaseName() {
        return getId().artifactId + "-" + getId().version;
    }

    @Override
    public void getTasks(@NotNull Consumer<Task> p) {
        super.getTasks(p);
        p.accept(Task.of("build", (ThrowingRunnable) this::build));
        getPublishTasks(p);
    }

    public void getPublishTasks(Consumer<Task> p) {
        createPublishTasks(p, this::build);
    }

    public static void createPublishTasks(Consumer<Task> p, BuildSupplier build) {
        p.accept(Task.of("publish", (ThrowingRunnable) () -> MavenPublishing.publish(MavenPublishing.AuthenticatedMaven.ofEnv(), build.get())));
        p.accept(Task.of("publishToMavenLocal", (ThrowingRunnable) () -> MavenPublishing.publish(MavenPublishing.AuthenticatedMaven.ofMavenLocal(), build.get())));
    }

    @Override
    @NotNull
    public IdeModule[] getIdeModules() {
        return new @NotNull IdeModule[] {
            new IdeModule.IdeModuleBuilder()
            .name(getId().artifactId)
            .root(getProjectDir())
            .javaVersion(getJavaVersion())
            .sourcePath(getSrcDir())
            .resourcePaths(getResourcesDir())
            .dependencies(dependencies.get())
            .build()
        };
    }

    @NotNull
    public JavaJarDependency build() throws CompilationFailure {
        JavaCompilationResult compilation = new JavaCompilation()
                .addSourceDir(getSrcDir())
                .addClasspath(getCompileDependencies())
                .addOptions(this.getCompileOptions().copy().setTargetVersionIfAbsent(getJavaVersion()))
                .compile();
        ProcessingSponge classes = new ProcessingSponge();
        compilation.getInputs(classes);
        Path outjar = getBuildLibsDir().resolve(getJarBaseName() + ".jar");
        Path outjarsources = getBuildLibsDir().resolve(getJarBaseName() + "-sources.jar");
        try (
            AtomicZipProcessingSink jarSink = new AtomicZipProcessingSink(outjar);
            AtomicZipProcessingSink jarSourcesSink = new AtomicZipProcessingSink(outjarsources);
        ) {
            resourcesProcessingChain().apply(jarSink, new DirectoryProcessingSource(getResourcesDir()));
            classes.getInputs(jarSink);
            // TODO bogus allocation?
            new DirectoryProcessingSource(getSrcDir()).getInputs(jarSourcesSink);
            jarSink.commit();
            jarSourcesSink.commit();
        }
        return new JavaJarDependency(outjar, outjar, getId());
    }

    @NotNull
    @Override
    public List<Path> getCompileDependencies() {
        List<JavaJarDependency> deps = dependencies.get();
        ArrayList<Path> result = new ArrayList<>(deps.size());
        for (JavaJarDependency dep : deps) {
            result.add(dep.jar);
        }
        return result;
    }

    /**
     * Obtains the options that are passed to the compiler when invoked via {@link #build()}.
     * The returned object can be mutated and will share the state used in {@link #build()}.
     * {@link #build()} uses this method and not the underlying field to obtain the compilation
     * options so overriding this method is valid, albeit potentially not viable.
     *
     * @return The compilation options
     */
    @NotNull
    @Contract(pure = true)
    public JavaCompilationOptions getCompileOptions() {
        return this.compileOptions;
    }

    @SuppressWarnings("null")
    @NotNull
    public List<JavaJarDependency> getDependencies() {
        return Collections.emptyList();
    }
}
