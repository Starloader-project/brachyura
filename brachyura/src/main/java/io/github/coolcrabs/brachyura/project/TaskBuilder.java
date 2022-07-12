package io.github.coolcrabs.brachyura.project;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.coolcrabs.brachyura.exception.TaskFailedException;
import io.github.coolcrabs.brachyura.util.ThrowingRunnable;

/**
 * A builder for creating instances of the {@link Task} class easily while maintaining absolute control.
 *
 * @author Geolykt
 * @since 0.90.3
 */
public class TaskBuilder {

    @Nullable
    private List<String> args = null;

    @Nullable
    private List<Path> classpath = null;

    @Nullable
    private String mainClass = null;

    @NotNull
    private final String name;

    @Nullable
    private List<Path> resourcePath = null;

    @Nullable
    private List<String> vmArgs = null;

    @Nullable
    private Integer javaVersion = null;

    @NotNull
    private Path workingDir;

    public TaskBuilder(@NotNull String name, @NotNull Path workingDir) {
        this.name = name;
        this.workingDir = workingDir;
    }

    public TaskBuilder(@NotNull String name, @NotNull Project project) {
        this.name = name;
        this.workingDir = project.getProjectDir().resolve("buildscript").resolve("run");
    }

    @NotNull
    @Contract(value = "!null -> fail; null -> new", pure = true)
    public Task build(@NotNull Consumer<String[]> action) {
        Objects.requireNonNull(action, "action cannot be null");

        final Task defaults = new Task(name, workingDir) {
            @Override
            public void doTask(String[] args) {
            }
        };

        String mainClass = this.mainClass;
        if (mainClass == null) {
            mainClass = defaults.getIdeRunConfigMainClass();
        }

        List<Path> classpath = this.classpath;
        if (classpath == null) {
            classpath = defaults.getIdeRunConfigClasspath();
        }

        List<Path> resourcePath = this.resourcePath;
        if (resourcePath == null) {
            resourcePath = defaults.getIdeRunConfigResourcepath();
        }

        List<String> args = this.args;
        if (args == null) {
            args = defaults.getIdeRunConfigArgs();
        }

        List<String> vmArgs = this.vmArgs;
        if (vmArgs == null) {
            vmArgs = defaults.getIdeRunConfigVMArgs();
        }

        Integer javaVersion = this.javaVersion;
        if (javaVersion == null) {
            javaVersion = defaults.getIdeRunConfigJavaVersion();
        }

        return new Task(name, javaVersion, mainClass, workingDir, vmArgs, args, resourcePath, classpath) {
            @Override
            public void doTask(String[] args) {
                action.accept(args);
            }
        };
    }

    @NotNull
    @Contract(value = "!null -> fail; null -> new", pure = true)
    public Task build(@NotNull ThrowingRunnable action) {
        return this.build((args) -> {
            try {
                action.run();
            } catch (Exception e) {
                throw new TaskFailedException("Task failed to execute!", e);
            }
        });
    }

    /**
     * Builds a task that unconditionally throws an exception if {@link Task#doTask(String[])} is called.
     * Such tasks are useful if they intend to create launch configuration in order for IDEs to launch
     * non-brachyura-related applications.
     *
     * @return The built {@link Task} instance
     * @author Geolykt
     * @since 0.90.3
     */
    @NotNull
    @Contract(value = "-> new", pure = true)
    public Task buildUnconditionallyThrowing() {
        return build((args) -> {
            throw new UnsupportedOperationException("This instance of \"Task\" is not meant to be executed!");
        });
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public TaskBuilder withArgs(@NotNull List<String> args) {
        this.args = args;
        return this;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public TaskBuilder withClasspath(@NotNull List<Path> classpath) {
        this.classpath = classpath;
        return this;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public TaskBuilder withMainClass(@NotNull String main) {
        this.mainClass = main;
        return this;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public TaskBuilder withResourcePath(@NotNull List<Path> resourcePath) {
        this.resourcePath = resourcePath;
        return this;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public TaskBuilder withVMArgs(@NotNull List<String> vmArgs) {
        this.vmArgs = vmArgs;
        return this;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public TaskBuilder withWorkingDirectory(@NotNull Path workingDir) {
        this.workingDir = workingDir;
        return this;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public TaskBuilder withJavaVersion(int version) {
        this.javaVersion = version;
        return this;
    }
}
