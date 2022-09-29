package io.github.coolcrabs.brachyura.project;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import io.github.coolcrabs.brachyura.compiler.java.CompilationFailedException;
import io.github.coolcrabs.brachyura.compiler.java.JavaCompilation;
import io.github.coolcrabs.brachyura.compiler.java.JavaCompilationOptions;
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

    @NotNull
    private final JavaCompilationOptions compileOptions = new JavaCompilationOptions();

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
        } else if (properties.get().containsKey("name")) {
            ideBuildscriptName = "BScript-" + properties.get().getProperty("name");
        }
        return ideBuildscriptName;
    }
    // Slbrachyura end

    public final Lazy<Properties> properties = new Lazy<>(this::createProperties);

    Properties createProperties() {
        try {
            Path file = getProjectDir().resolve("buildscript.properties");
            Properties properties0 = new Properties();
            if (Files.exists(file)) {
                try (BufferedReader r = Files.newBufferedReader(file)) {
                    properties0.load(r);
                }
            } else {
                Logger.info("Didn't find buildscript.properties; autogenerating it.");
                // properties0.setProperty("name", super.getProjectDir().getFileName().toString()); // Slbrachyura: We use our own buildscript name system
                properties0.setProperty("javaVersion", "8");
                try (BufferedWriter w = Files.newBufferedWriter(file)) {
                    properties0.store(w, "Brachyura Buildscript Properties");
                }
            }
            return properties0;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    String getPropOrThrow(String property) {
        String r = properties.get().getProperty(property);
        if (r == null) throw new RuntimeException("Missing property " + property + " in buildscript.properties");
        return r;
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
        int javaVersion = Integer.parseInt(getPropOrThrow("javaVersion"));

        return new @NotNull IdeModule[] {
            new IdeModule.IdeModuleBuilder()
                .name(getProjectName())
                .root(getProjectDir())
                .sourcePath(getSrcDir())
                .dependencies(this::getIdeDependencies)
                .withTasks(tasks)
                .javaVersion(javaVersion)
            .build()
        };
        // Slbrachyura end
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
        int javaVersion = Integer.parseInt(getPropOrThrow("javaVersion"));
        try {
            JavaCompilationResult compilation = getCompileOptions().commit(new JavaCompilation()
                .addSourceDir(getSrcDir())
                .addClasspath(getCompileDependencies())
                .addOption(JvmUtil.compileArgs(JvmUtil.CURRENT_JAVA_VERSION, javaVersion)))
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
    @Override
    public JavaCompilationOptions getCompileOptions() {
        return this.compileOptions;
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
