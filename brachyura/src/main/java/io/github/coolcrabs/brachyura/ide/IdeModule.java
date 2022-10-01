package io.github.coolcrabs.brachyura.ide;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.project.Task;
import io.github.coolcrabs.brachyura.project.TaskBuilder;
import io.github.coolcrabs.brachyura.util.Lazy;

public final class IdeModule {
    @NotNull
    public final String name;
    @NotNull
    public final Path root;
    public final Lazy<@NotNull List<JavaJarDependency>> dependencies;
    public final List<IdeModule> dependencyModules;
    public final List<@NotNull Task> tasks; // Slbrachyura: Improved task system
    public final List<@NotNull Path> sourcePaths;
    public final List<@NotNull Path> resourcePaths;
    public final List<@NotNull Path> testSourcePaths;
    public final List<@NotNull Path> testResourcePaths;
    public final int javaVersion;

    IdeModule(@NotNull String name, @NotNull Path root, Supplier<@NotNull List<JavaJarDependency>> dependencies, List<IdeModule> dependencyModules, List<@NotNull Path> sourcePaths, List<@NotNull Path> resourcePaths, List<@NotNull Path> testSourcePaths, List<@NotNull Path> testResourcePaths, int javaVersion, List<@NotNull Task> tasks) {
        this.name = name;
        this.root = root;
        this.dependencies = new Lazy<>(dependencies);
        this.dependencyModules = dependencyModules;
        this.sourcePaths = sourcePaths;
        this.resourcePaths = resourcePaths;
        this.testSourcePaths = testSourcePaths;
        this.testResourcePaths = testResourcePaths;
        this.javaVersion = javaVersion;
        this.tasks = tasks;
    }

    public static class IdeModuleBuilder {
        private String name;
        private Path root;
        @SuppressWarnings("null")
        private Supplier<@NotNull List<JavaJarDependency>> dependencies = Collections::emptyList;
        private List<IdeModule> dependencyModules = Collections.emptyList();
        @Deprecated // Slbrachyura: Improved tasks system
        private List<RunConfigBuilder> runConfigs = Collections.emptyList();
        private List<@NotNull Task> tasks = Collections.emptyList();
        private List<@NotNull Path> sourcePaths = Collections.emptyList();
        private List<@NotNull Path> resourcePaths = Collections.emptyList();
        private List<@NotNull Path> testSourcePaths = Collections.emptyList();
        private List<@NotNull Path> testResourcePaths = Collections.emptyList();
        private int javaVersion = 8;

        @Contract(pure = false, mutates = "this", value = "_ -> this")
        @NotNull
        public IdeModuleBuilder addTask(@NotNull Task task) {
            if (tasks.getClass() != ArrayList.class) {
                tasks = new ArrayList<>(tasks); // Ensure that it is mutable
            }
            tasks.add(task);
            return this;
        }

        public IdeModuleBuilder name(String name) {
            this.name = name;
            return this;
        }

        public IdeModuleBuilder root(Path root) {
            this.root = root;
            return this;
        }

        public IdeModuleBuilder dependencies(Supplier<@NotNull List<JavaJarDependency>> dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        public IdeModuleBuilder dependencies(@NotNull List<JavaJarDependency> dependencies) {
            this.dependencies = () -> dependencies;
            return this;
        }

        @SuppressWarnings("null")
        public IdeModuleBuilder dependencies(JavaJarDependency... dependencies) {
            this.dependencies = () -> Arrays.asList(dependencies);
            return this;
        }

        public IdeModuleBuilder dependencyModules(List<IdeModule> dependencyModules) {
            this.dependencyModules = dependencyModules;
            return this;
        }

        public IdeModuleBuilder dependencyModules(IdeModule... dependencyModules) {
            this.dependencyModules = Arrays.asList(dependencyModules);
            return this;
        }

        @Deprecated // Slbrachyura: Improved tasks system
        public IdeModuleBuilder runConfigs(List<RunConfigBuilder> runConfigs) {
            this.runConfigs = runConfigs;
            return this;
        }

        @Deprecated // Slbrachyura: Improved tasks system
        public IdeModuleBuilder runConfigs(RunConfigBuilder... runConfigs) {
            this.runConfigs = Arrays.asList(runConfigs);
            return this;
        }

        @Contract(pure = false, mutates = "this", value = "_ -> this")
        @NotNull
        public IdeModuleBuilder withTasks(List<@NotNull Task> tasks) {
            this.tasks = tasks;
            return this;
        }

        public IdeModuleBuilder sourcePaths(List<@NotNull Path> sourcePaths) {
            this.sourcePaths = sourcePaths;
            return this;
        }

        public IdeModuleBuilder sourcePaths(@NotNull Path... sourcePaths) {
            this.sourcePaths = Arrays.asList(sourcePaths);
            return this; 
        }

        public IdeModuleBuilder sourcePath(@NotNull Path sourcePath) {
            this.sourcePaths = new ArrayList<>();
            sourcePaths.add(sourcePath);
            return this;
        }

        public IdeModuleBuilder resourcePaths(List<@NotNull Path> resourcePaths) {
            this.resourcePaths = resourcePaths;
            return this;
        }

        public IdeModuleBuilder resourcePaths(@NotNull Path... resourcePaths) {
            this.resourcePaths = Arrays.asList(resourcePaths);
            return this;
        }

        public IdeModuleBuilder testSourcePaths(@NotNull Path... testSourcePaths) {
            this.testSourcePaths = Arrays.asList(testSourcePaths);
            return this; 
        }

        public IdeModuleBuilder testSourcePath(@NotNull Path testSourcePath) {
            this.testSourcePaths = new ArrayList<>();
            testSourcePaths.add(testSourcePath);
            return this;
        }

        public IdeModuleBuilder testResourcePaths(List<@NotNull Path> testResourcePaths) {
            this.testResourcePaths = testResourcePaths;
            return this;
        }

        public IdeModuleBuilder testResourcePaths(@NotNull Path... testResourcePaths) {
            this.testResourcePaths = Arrays.asList(testResourcePaths);
            return this;
        }

        public IdeModuleBuilder testResourcePath(@NotNull Path testResourcePath) {
            this.testResourcePaths = new ArrayList<>();
            testResourcePaths.add(testResourcePath);
            return this;
        }

        public IdeModuleBuilder javaVersion(int javaVersion) {
            this.javaVersion = javaVersion;
            return this;
        }

        @SuppressWarnings("null") // Generics are strange
        @NotNull
        public IdeModule build() {
            Objects.requireNonNull(name, "IdeModule missing name");
            Objects.requireNonNull(root, "IdeModule missing root");
            List<@NotNull Task> tasks = new ArrayList<>(this.tasks);
            this.runConfigs.forEach(rcBuilder -> {
                tasks.add(rcBuilder.build());
            });
            return new IdeModule(name, root, dependencies, dependencyModules, sourcePaths, resourcePaths, testSourcePaths, testResourcePaths, javaVersion, tasks);
        }
    }

    @Deprecated // Slbrachyura: Improved tasks system
    public static class RunConfigBuilder {
        private String name;
        private String mainClass;
        private Path cwd;
        private Supplier<List<String>> vmArgs = Collections::emptyList;
        private Supplier<List<String>> args = Collections::emptyList;
        private Supplier<List<Path>> classpath = Collections::emptyList;
        @Deprecated @SuppressWarnings("unused") // Slbrachyura: Does anyone know what this does? I do not
        private List<IdeModule> additionalModulesClasspath = Collections.emptyList();
        private List<Path> resourcePaths = Collections.emptyList();

        public RunConfigBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RunConfigBuilder mainClass(String mainClass) {
            this.mainClass = mainClass;
            return this;
        }

        public RunConfigBuilder cwd(Path cwd) {
            this.cwd = cwd;
            return this;
        }

        public RunConfigBuilder vmArgs(Supplier<List<String>> vmArgs) {
            this.vmArgs = vmArgs;
            return this;
        }

        public RunConfigBuilder vmArgs(List<String> vmArgs) {
            this.vmArgs = () -> vmArgs;
            return this;
        }

        public RunConfigBuilder vmArgs(String... vmArgs) {
            this.vmArgs = () -> Arrays.asList(vmArgs);
            return this;
        }

        public RunConfigBuilder args(Supplier<List<String>> args) {
            this.args = args;
            return this;
        }

        public RunConfigBuilder args(List<String> args) {
            this.args = () -> args;
            return this;
        }

        public RunConfigBuilder args(String... args) {
            this.args = () -> Arrays.asList(args);
            return this;
        }

        public RunConfigBuilder classpath(Supplier<List<Path>> classpath) {
            this.classpath = classpath;
            return this;
        }

        public RunConfigBuilder classpath(List<Path> classpath) {
            this.classpath = () -> classpath;
            return this;
        }

        public RunConfigBuilder classpath(Path... classpath) {
            this.classpath = () -> Arrays.asList(classpath);
            return this;
        }

        public RunConfigBuilder additionalModulesClasspath(List<IdeModule> additionalModulesClasspath) {
            this.additionalModulesClasspath = additionalModulesClasspath;
            return this;
        }
        
        public RunConfigBuilder additionalModulesClasspath(IdeModule... additionalModulesClasspath) {
            this.additionalModulesClasspath = Arrays.asList(additionalModulesClasspath);
            return this;
        }
        
        public RunConfigBuilder resourcePaths(List<Path> resourcePaths) {
            this.resourcePaths = resourcePaths;
            return this;
        }

        public RunConfigBuilder resourcePaths(Path... resourcePaths) {
            this.resourcePaths = Arrays.asList(resourcePaths);
            return this;
        }

        @SuppressWarnings("null")
        Task build() {
            Objects.requireNonNull(name, "Null name");
            Objects.requireNonNull(mainClass, "Null mainClass");
            Objects.requireNonNull(cwd, "Null cwd");
            return new TaskBuilder(name, cwd)
                    .withMainClass(mainClass)
                    .withVMArgs(vmArgs.get())
                    .withArgs(args.get())
                    .withClasspath(classpath.get())
                    .withResourcePath(resourcePaths)
                    .buildUnconditionallyThrowing();
        }
    }

    // Slbrachyura start: Implement #equals and #hashcode for IdeModule to guarantee stability
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IdeModule) {
            return ((IdeModule) obj).name.equals(this.name);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() ^ 0x4839;
    }
    // Slbrachyura end
}
