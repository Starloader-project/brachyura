package io.github.coolcrabs.brachyura.project;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import io.github.coolcrabs.brachyura.compiler.java.CompilationFailedException;
import io.github.coolcrabs.brachyura.compiler.java.JavaCompilation;
import io.github.coolcrabs.brachyura.compiler.java.JavaCompilationResult;
import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.ide.IdeModule;
import io.github.coolcrabs.brachyura.maven.MavenId;
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

    // Slbrachyura start: Allow to rename the name of the IDE project generated by the buildscript project
    protected String getProjectName() {
        String ideBuildscriptName = "Buildscript";
        Optional<Project> buildscriptInstance = project.get();
        if (buildscriptInstance.isPresent()) {
            Project concreteBuildscriptInstance = buildscriptInstance.get();
            if (concreteBuildscriptInstance instanceof DescriptiveBuildscriptName) {
                ideBuildscriptName = ((DescriptiveBuildscriptName) concreteBuildscriptInstance).getBuildscriptName();
            }
        }
        return ideBuildscriptName;
    }

    @Override
    @NotNull
    public IdeModule[] getIdeModules() {
        List<@NotNull Task> tasks = new ArrayList<>();
        Path runDirectory = getProjectDir().resolve("run");
        PathUtil.createDirectories(runDirectory);
        Optional<Project> buildscriptInstance = project.get();
        if (buildscriptInstance.isPresent()) {
            tasks.addAll(buildscriptInstance.get().getTasks());
        }
        // Slbrachyura end

        return new @NotNull IdeModule[] {
            new IdeModule.IdeModuleBuilder()
                .name(getProjectName())
                .root(getProjectDir())
                .sourcePath(getSrcDir())
                .dependencies(this::getIdeDependencies)
                .withTasks(tasks)
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
                Constructor c = projectclass.getDeclaredConstructor(); // Slbrachyura: Allow arbitrary visibility modifiers in buildscript file
                c.setAccessible(true);
                return Optional.of((Project) c.newInstance());
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
        try {
            JavaCompilationResult compilation = new JavaCompilation()
                .addSourceDir(getSrcDir())
                .addClasspath(getCompileDependencies())
                .addOption(JvmUtil.compileArgs(JvmUtil.CURRENT_JAVA_VERSION, 8)) // TODO: Make configurable - somehow
                .compile();
            BuildscriptClassloader r = new BuildscriptClassloader(BuildscriptProject.class.getClassLoader());
            compilation.getInputs(r);
            return r;
        } catch (CompilationFailedException e) {
            Logger.warn("Buildscript compilation failed!");
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
            // Not the most ideal solution, but it ought to do
            result.add(new JavaJarDependency(p, source, new MavenId("unknown", p.toFile().getName().split("\\.")[0], "undefined")));
        }
        return result;
    }

    @NotNull
    public List<Path> getCompileDependencies() {
        return EntryGlobals.getCompileDependencies(false);
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
