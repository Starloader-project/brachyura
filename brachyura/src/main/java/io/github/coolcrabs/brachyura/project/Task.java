package io.github.coolcrabs.brachyura.project;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.exception.TaskFailedException;
import io.github.coolcrabs.brachyura.util.ThrowingRunnable;

/**
 * A task is an executor that either performs command-like requests when the brachyura bootstrap is invoked
 * through the CLI or is used to generate run configurations that are used by the IDE.
 *
 * <p>With Slbrachyura, finer control is given with latter functionality while not being very convoluted to use.
 */
public abstract class Task {

    @NotNull
    public final String name;

    private final int ideRunConfigJavaVersion;

    @NotNull
    private final String ideRunConfigMainClass;

    @NotNull
    private final Path ideRunConfigWorkingDir;

    @NotNull
    private final List<String> ideRunConfigVMArgs;

    @NotNull
    private final List<String> ideRunConfigArgs;

    @NotNull
    private final List<Path> ideRunConfigClasspath;

    @NotNull
    private final List<Path> ideRunConfigResourcepath;

    private static final int DEFAULT_JAVA_VERSION = 8;

    @NotNull
    private static final String DEFAULT_MAIN_CLASS = "io.github.coolcrabs.brachyura.project.BuildscriptDevEntry";

    /**
     * Obtains the name of the task. Used in the CLI as the task name and used in IDE run configuration files for
     * the name of the run configuration. The task name should ideally not contain any spaces to simplify the process
     * of executing the task.
     *
     * @return The name of the task
     * @since 0.90.3
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Obtains the java version to use when running this task through the IDE.
     * <b>Does not have an effect on the CLI, the java version used to execute the bootstrap will be used
     * to run the task.</b>
     *
     * @return The java version to use for executing this task
     * @since 0.90.3
     */
    public int getIdeRunConfigJavaVersion() {
        return ideRunConfigJavaVersion;
    }

    /**
     * Returns the class that is the main class of the task within the IDE run. This usually will be the bootstrap class
     * that in turn invokes the task, more specifically {@link BuildscriptDevEntry}. It can also be another class,
     * if other applications should be run through the IdeRunConfig.
     *
     * <p>The specified class needs to be present on the classpath.
     *
     * <p><b>Disregarded if the task is run through the CLI</b>
     *
     * @return The application main class behind the task
     * @since 0.90.3
     */
    @NotNull
    public String getIdeRunConfigMainClass() {
        return ideRunConfigMainClass;
    }

    /**
     * Obtains the working directory used to launch the task within the IDE. Usually the project's project directory.
     *
     * <p><b>Disregarded if the task is run through the CLI</b>
     *
     * @return The working directory to use
     * @since 0.90.3
     */
    @NotNull
    public Path getIdeRunConfigWorkingDir() {
        return ideRunConfigWorkingDir;
    }

    /**
     * Obtains the arguments that are passed to the application's main method.
     *
     * <p><b>Disregarded if the task is run through the CLI</b>
     *
     * @return The arguments passed to the application
     * @since 0.90.3
     */
    @NotNull
    public List<String> getIdeRunConfigArgs() {
        return ideRunConfigArgs;
    }

    /**
     * Obtains the arguments that are passed to the JVM when launching through an IDE run configuration.
     *
     * <p><b>Disregarded if the task is run through the CLI</b>
     *
     * @return The VM arguments to pass
     * @since 0.90.3
     */
    @NotNull
    public List<String> getIdeRunConfigVMArgs() {
        return ideRunConfigVMArgs;
    }

    /**
     * Obtains a list of paths (usually jar files) where classes are located that should be used when launching through an IDE
     * run configuration.
     *
     * <p><b>Disregarded if the task is run through the CLI</b>
     *
     * @return The classpath to use
     * @since 0.90.3
     */
    @NotNull
    public List<Path> getIdeRunConfigClasspath() {
        return ideRunConfigClasspath;
    }

    /**
     * It is a bit unknown what the resource path does.
     * It appears that it is identical to {@link #getIdeRunConfigClasspath()} in terms of behaviour.
     *
     * <p><b>Disregarded if the task is run through the CLI</b>
     *
     * @return The resource path to use
     * @since 0.90.3
     */
    @NotNull
    public List<Path> getIdeRunConfigResourcepath() {
        return ideRunConfigResourcepath;
    }

    @NotNull
    public static TaskBuilder builder(@NotNull String name, @NotNull Project project) {
        return new TaskBuilder(name, project);
    }

    @NotNull
    private static final Path getDefaultWorkingDirectory(Project project) {
        return project.getProjectDir().resolve("buildscript").resolve("run"); // put in a method to prevent accidentally eagerly caching it too early
    }

    @NotNull
    private static final Path inferProjectPath(Path workingDir) {
        ifCond:
        if (workingDir.endsWith("buildscript/run")) {
            Path p = Objects.requireNonNull(workingDir.getParent()).getParent();
            if (p == null) {
                break ifCond;
            }
            return p;
        }
        try {
            return EntryGlobals.getProjectDir();
        } catch (Exception e) {
            // Not set - who would have thought? So we need to use an arbitrary directory
            return workingDir;
        }
    }

    @NotNull
    private static final List<Path> getDefaultClasspath() {
        return EntryGlobals.getCompileDependencies(true);
    }

    @NotNull
    private static final List<String> getDefaultArgs(@NotNull String taskName, @NotNull Path projectPath) {
        List<String> args = new ArrayList<>();
        args.add(projectPath.toString());

        StringBuilder builder = new StringBuilder();
        for (Path path : EntryGlobals.getCompileDependencies(true)) {
            builder.append(path.toString());
            builder.append(File.pathSeparatorChar);
        }
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        args.add(taskName);

        return args;
    }

    /**
     * Constructor. Uses default values for stuff needed for IDE run configuration generation.
     * Java 8 compliance is used, but by overriding methods such as {@link #getIdeRunConfigJavaVersion()} this can be changed.
     *
     * @param name The name of the task
     * @param project The project, which is used to infer the default working directory (i.e. {@link #getIdeRunConfigWorkingDir()}) from
     */
    protected Task(@NotNull String name, @NotNull Project project) {
        this(name, getDefaultWorkingDirectory(project), project.getProjectDir());
    }

    /**
     * Constructor. Uses default values for stuff needed for IDE run configuration generation.
     * Java 8 compliance is used, but by overriding methods such as {@link #getIdeRunConfigJavaVersion()} this can be changed.
     *
     * @param name The name of the task
     * @param workingDirectory The default working directory (i.e. {@link #getIdeRunConfigWorkingDir()}), also used to infer the project directory if possible (otherwise falls back to {@link EntryGlobals#getProjectDir()})
     */
    protected Task(@NotNull String name, @NotNull Path workingDirectory) {
        this(name, workingDirectory, inferProjectPath(workingDirectory));
    }

    /**
     * Constructor. Uses default values for stuff needed for IDE run configuration generation.
     * Java 8 compliance is used, but by overriding methods such as {@link #getIdeRunConfigJavaVersion()} this can be changed.
     *
     * @param name The name of the task
     * @param workingDirectory The default working directory (i.e. {@link #getIdeRunConfigWorkingDir()})
     * @param projectPath The path to the project directory
     */
    Task(@NotNull String name, @NotNull Path workingDirectory, @NotNull Path projectPath) {
        this(name, DEFAULT_JAVA_VERSION, DEFAULT_MAIN_CLASS, workingDirectory, new ArrayList<>(), getDefaultArgs(name, projectPath), new ArrayList<>(), getDefaultClasspath());
    }

    Task(@NotNull String name, int javaVer, @NotNull String mainClass, @NotNull Path workingDir, @NotNull List<String> vmArgs, @NotNull List<String> args, @NotNull List<Path> resourcePath, @NotNull List<Path> classPath) {
        this.name = name;
        this.ideRunConfigJavaVersion = javaVer;
        this.ideRunConfigMainClass = mainClass;
        this.ideRunConfigVMArgs = vmArgs;
        this.ideRunConfigArgs = args;
        this.ideRunConfigClasspath = classPath;
        this.ideRunConfigResourcepath = resourcePath;
        this.ideRunConfigWorkingDir = workingDir;
    }

    @NotNull
    @Deprecated // Slbrachyura: Improved task system
    public static Task of(@NotNull String name, BooleanSupplier run) {
        return new FailableNoArgTask(name, EntryGlobals.getProjectDir().resolve("buildscript").resolve("run"), run);
    }

    @NotNull
    @Deprecated // Slbrachyura: Improved task system
    public static Task of(@NotNull String name, Runnable run) {
        return new NoArgTask(name, EntryGlobals.getProjectDir().resolve("buildscript").resolve("run"), run);
    }

    @NotNull
    @Deprecated // Slbrachyura: Improved task system
    public static Task of(@NotNull String name, ThrowingRunnable run) {
        return new NoArgTask(name, EntryGlobals.getProjectDir().resolve("buildscript").resolve("run"), run);
    }

    @NotNull
    @Deprecated // Slbrachyura: Improved task system
    public static Task of(@NotNull String name, Consumer<String[]> run) {
        return new TaskWithArgs(name, EntryGlobals.getProjectDir().resolve("buildscript").resolve("run"), run);
    }

    /**
     * Performs/executes the task's contents. Beware that some tasks only exist to generate run configurations,
     * in which case this method is not applicable and can throw an exception unconditionally for these tasks.
     *
     * @param args The arguments to pass to the task
     */
    public abstract void doTask(String[] args);

    @Deprecated // Slbrachyura: Replaced with TaskBuilder
    static class FailableNoArgTask extends Task {
        final BooleanSupplier runnable;

        FailableNoArgTask(@NotNull String name, @NotNull Path workingDir, BooleanSupplier runnable) {
            super(name, workingDir);
            this.runnable = runnable;
        }

        @Override
        public void doTask(String[] args) {
            if (!runnable.getAsBoolean()) throw new TaskFailedException("Task returned false");
        }
    }

    @Deprecated // Slbrachyura: Replaced with TaskBuilder
    static class NoArgTask extends Task {
        final ThrowingRunnable runnable;

        NoArgTask(@NotNull String name, @NotNull Path workingDir, Runnable runnable) {
            super(name, workingDir);
            this.runnable = () -> runnable.run();
        }

        NoArgTask(@NotNull String name, @NotNull Path workingDir, ThrowingRunnable runnable) {
            super(name, workingDir);
            this.runnable = runnable;
        }

        @Override
        public void doTask(String[] args) {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new TaskFailedException("Task failed to execute!", e);
            }
        }
    }

    @Deprecated // Slbrachyura: Replaced with TaskBuilder
    static class TaskWithArgs extends Task {
        final Consumer<String[]> task;

        TaskWithArgs(@NotNull String name, @NotNull Path workingDir, Consumer<String[]> task) {
            super(name, workingDir);
            this.task = task;
        }

        @Override
        public void doTask(String[] args) {
            task.accept(args);
        }
    }
}
