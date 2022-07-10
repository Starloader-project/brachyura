package io.github.coolcrabs.brachyura.project;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private Path workingDir = null;

    public TaskBuilder(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    @Contract(value = "!null -> fail; null -> new", pure = true)
    public Task build(@NotNull Consumer<String[]> action) {
        Objects.requireNonNull(action, "action cannot be null");
        Task defaults = new Task(name) {
            @Override
            public void doTask(String[] args) {
            }
        };

        Path workingDir = this.workingDir;
        if (workingDir == null) {
            workingDir = defaults.getIdeRunConfigWorkingDir();
        }

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

        return new Task(name, defaults.getIdeRunConfigJavaVersion(), mainClass, workingDir, vmArgs, args, resourcePath, classpath) {
            @Override
            public void doTask(String[] args) {
                action.accept(args);
            }
        };
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

    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public TaskBuilder withArgs(@NotNull List<String> args) {
        this.args = args;
        return this;
    }

    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public TaskBuilder withClasspath(@NotNull List<Path> classpath) {
        this.classpath = classpath;
        return this;
    }

    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public TaskBuilder withMainClass(@NotNull String main) {
        this.mainClass = main;
        return this;
    }

    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public TaskBuilder withResourcePath(@NotNull List<Path> resourcePath) {
        this.resourcePath = resourcePath;
        return this;
    }

    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public TaskBuilder withVMArgs(@NotNull List<String> vmArgs) {
        this.vmArgs = vmArgs;
        return this;
    }

    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public TaskBuilder withWorkingDirectory(@NotNull Path workingDir) {
        this.workingDir = workingDir;
        return this;
    }
}
