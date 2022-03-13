package io.github.coolcrabs.brachyura.project;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import io.github.coolcrabs.brachyura.compiler.java.JavaCompilation;
import io.github.coolcrabs.brachyura.compiler.java.JavaCompilationResult;
import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.exception.CompilationFailure;
import io.github.coolcrabs.brachyura.ide.IdeModule;
import io.github.coolcrabs.brachyura.ide.IdeModule.RunConfigBuilder;
import io.github.coolcrabs.brachyura.processing.ProcessingId;
import io.github.coolcrabs.brachyura.processing.ProcessingSink;
import io.github.coolcrabs.brachyura.project.java.BaseJavaProject;
import io.github.coolcrabs.brachyura.util.JvmUtil;
import io.github.coolcrabs.brachyura.util.Lazy;
import io.github.coolcrabs.brachyura.util.PathUtil;
import io.github.coolcrabs.brachyura.util.StreamUtil;
import io.github.coolcrabs.brachyura.util.Util;

class BuildscriptProject extends BaseJavaProject {
    @Override
    @NotNull
    public Path getProjectDir() {
        return super.getProjectDir().resolve("buildscript");
    }

    @Override
    public void getRunConfigTasks(Consumer<Task> p) {
        //noop
    }

    @Override
    @NotNull
    public IdeModule[] getIdeModules() {
        Tasks builtProjectTasks = new Tasks();
        String ideBuildscriptName = "Buildscript";
        Optional<Project> buildscriptInstance = project.get();
        if (buildscriptInstance.isPresent()) {
            Project concreteBuildscriptInstance = buildscriptInstance.get();
            concreteBuildscriptInstance.getTasks(builtProjectTasks);
            if (concreteBuildscriptInstance instanceof DescriptiveBuildscriptName) {
                ideBuildscriptName = ((DescriptiveBuildscriptName) concreteBuildscriptInstance).getBuildscriptName();
            }
        }
        ArrayList<RunConfigBuilder> runConfigs = new ArrayList<>(builtProjectTasks.getAllTasks().size());
        Path cwd = getProjectDir().resolve("run");
        PathUtil.createDirectories(cwd);
        for (Map.Entry<String, Task> e : builtProjectTasks.getAllTasks().entrySet()) {
            String projectDir = super.getProjectDir().toString(); // eclipe's null evaluation can sometimes be a bit strange when generics are at play
            runConfigs.add(
                new RunConfigBuilder()
                    .name(e.getKey())
                    .cwd(cwd)
                    .mainClass("io.github.coolcrabs.brachyura.project.BuildscriptDevEntry")
                    .classpath(getCompileDependencies())
                    .args(
                        () -> Arrays.asList(
                            projectDir,
                            getCompileDependencies().stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator)),
                            e.getKey()
                        )
                    )
            );
        }

        return new @NotNull IdeModule[] {
            new IdeModule.IdeModuleBuilder()
                .name(ideBuildscriptName)
                .root(getProjectDir())
                .sourcePath(getSrcDir())
                .dependencies(this::getIdeDependencies)
                .runConfigs(runConfigs)
            .build()
        };
    }

    public final Lazy<Optional<Project>> project = new Lazy<>(this::createProject);
    @SuppressWarnings("all")
    public Optional<Project> createProject() {
        try {
            ClassLoader b = getBuildscriptClassLoader();
            if (b == null) return Optional.empty();
            Class projectclass = Class.forName("Buildscript", true, b);
            if (Project.class.isAssignableFrom(projectclass)) {
                return Optional.of((Project) projectclass.getDeclaredConstructor().newInstance());
            } else {
                Logger.warn("Buildscript must be instance of Project");
                return Optional.empty();
            }
        } catch (Exception e) {
            Logger.warn("Error getting project:");
            Logger.warn(e);
            return Optional.empty();
        }
    }

    public ClassLoader getBuildscriptClassLoader() {
        JavaCompilationResult compilation;
        try {
            compilation = new JavaCompilation()
                .addSourceDir(getSrcDir())
                .addClasspath(getCompileDependencies())
                .addOption(JvmUtil.compileArgs(JvmUtil.CURRENT_JAVA_VERSION, 8))
                .compile();
            BuildscriptClassloader r = new BuildscriptClassloader(BuildscriptProject.class.getClassLoader());
            compilation.getInputs(r);
            return r;
        } catch (CompilationFailure e) {
            Logger.warn("Buildscript compilation failed!");
            e.printStackTrace();
            return null;
        }
    }

    @NotNull
    public List<JavaJarDependency> getIdeDependencies() {
        List<Path> compileDeps = getCompileDependencies();
        ArrayList<JavaJarDependency> result = new ArrayList<>(compileDeps.size());
        for (Path p : compileDeps) {
            Path source = p.resolveSibling(p.getFileName().toString().replace(".jar", "-sources.jar"));
            if (!Files.exists(source)) source = null;
            result.add(new JavaJarDependency(p, source, null));
        }
        return result;
    }

    @Override
    @NotNull
    public List<Path> getCompileDependencies() {
        List<Path> buildscriptClasspath = EntryGlobals.buildscriptClasspath;
        if (buildscriptClasspath == null) {
            throw new IllegalStateException("The buildscript classpath was not set. Did you call BuildscriptDevEntry#main or BrachyuraEntry#main?");
        }
        return buildscriptClasspath;
    }

    static class BuildscriptClassloader extends ClassLoader implements ProcessingSink {
        HashMap<String, byte[]> classes = new HashMap<>();

        BuildscriptClassloader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public void sink(Supplier<InputStream> in, ProcessingId id) {
            try {
                if (id.path.endsWith(".class")) {
                    try (InputStream i = in.get()) {
                        classes.put(id.path.substring(0, id.path.length() - 6).replace("/", "."), StreamUtil.readFullyAsBytes(i));
                    }
                }
            } catch (Exception e) {
                throw Util.sneak(e);
            }
        }

        @Override
        protected Class<?> findClass(@Nullable String name) throws ClassNotFoundException {
            byte[] data = classes.get(name);
            if (data == null) {
                return super.findClass(name);
            }
            return defineClass(name, data, 0, data.length);
        }
    }
}
