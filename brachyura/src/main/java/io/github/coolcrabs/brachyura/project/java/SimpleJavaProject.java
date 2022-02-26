package io.github.coolcrabs.brachyura.project.java;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import io.github.coolcrabs.brachyura.compiler.java.JavaCompilation;
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
import io.github.coolcrabs.brachyura.util.JvmUtil;
import io.github.coolcrabs.brachyura.util.Lazy;
import io.github.coolcrabs.brachyura.util.ThrowingRunnable;

import org.jetbrains.annotations.NotNull;

public abstract class SimpleJavaProject extends BaseJavaProject {
    public abstract MavenId getId();
    
    public String getJarBaseName() {
        return getId().artifactId + "-" + getId().version;
    }

    @Override
    public void getTasks(Consumer<Task> p) {
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
                .addOption(JvmUtil.compileArgs(JvmUtil.CURRENT_JAVA_VERSION, getJavaVersion()))
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
            new DirectoryProcessingSource(getSrcDir()).getInputs(jarSourcesSink);
            jarSink.commit();
            jarSourcesSink.commit();
        }
        return new JavaJarDependency(outjar, outjar, getId());
    }

    public final Lazy<@NotNull List<JavaJarDependency>> dependencies = new Lazy<>(this::getDependencies);

    @SuppressWarnings("null")
    @NotNull
    public List<JavaJarDependency> getDependencies() {
        return Collections.emptyList();
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
}
