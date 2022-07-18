package io.github.coolcrabs.brachyura.project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.coolcrabs.brachyura.exception.TaskFailedException;
import io.github.coolcrabs.brachyura.ide.source.JavaJRESourceLookupEntry;
import io.github.coolcrabs.brachyura.ide.source.SourceLookupEntry;
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

    @NotNull
    private List<SourceLookupEntry> debugSources = new ArrayList<>();

    @Nullable
    private Integer javaVersion = null;

    @Nullable
    private String mainClass = null;

    @NotNull
    private final String name;

    @Nullable
    private List<Path> resourcePath = null;

    @Nullable
    private List<String> vmArgs = null;

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

    /**
     * Adds a {@link SourceLookupEntry} that should be added to the list of entries used in eclipse's lookup feature.
     * The same system may be used to aid the debugging in other IDEs, if contributors are willing to maintain such a
     * feature in the future.
     *
     * <p>Additionally, if no {@link JavaJRESourceLookupEntry} is added before {@link TaskBuilder#build(Consumer)} is run,
     * an instance of that class is added with the java version specified by {@link #withJavaVersion(int)}
     * (or it's default) is added to the list of sources.
     *
     * @param entry The entry to add
     * @return The current builder instance
     * @since 0.90.5
     */
    @NotNull
    @Contract(mutates = "this", pure = false, value = "!null -> this; null -> fail")
    public TaskBuilder addDebugSource(@NotNull SourceLookupEntry entry) {
        Objects.requireNonNull(entry, "entry may not be null");
        this.debugSources.add(entry);
        return this;
    }

    /**
     * Adds multiple {@link SourceLookupEntry} that should be added to the list of entries used in eclipse's lookup feature.
     * The same system may be used to aid the debugging in other IDEs, if contributors are willing to maintain such a
     * feature in the future.
     *
     * <p>The "default" source lookup entry, which - by default - represents the buildscript project and it's dependencies,
     * will be included either way and cannot be overridden, but will have a lower priority than any entries
     * added by this method.
     *
     * <p>Additionally, if no {@link JavaJRESourceLookupEntry} is added before {@link TaskBuilder#build(Consumer)} is run,
     * an instance of that class is added with the java version specified by {@link #withJavaVersion(int)}
     * (or it's default) is added to the list of sources.
     *
     * @param entries The entries to add
     * @return The current builder instance
     * @since 0.90.5
     */
    @NotNull
    @Contract(mutates = "this", pure = false, value = "!null -> this; null -> fail")
    public TaskBuilder addDebugSources(@NotNull Collection<SourceLookupEntry> entries) {
        Objects.requireNonNull(entries, "entries may not be null");
        this.debugSources.addAll(entries);
        return this;
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

        List<SourceLookupEntry> debugSources = this.debugSources;
        if (debugSources.isEmpty()) {
            debugSources = defaults.getIdeDebugConfigSourceLookupEntries();
        }

        boolean containsJRE = false;
        for (SourceLookupEntry e : debugSources) {
            if (e instanceof JavaJRESourceLookupEntry) {
                containsJRE = true;
                break; // Short-circuit
            }
        }

        if (!containsJRE) {
            // Add the JRE
            if (debugSources.getClass() != ArrayList.class) { // Ensure that debugSources can be mutated
                debugSources = new ArrayList<>(debugSources);
            }
            debugSources.add(new JavaJRESourceLookupEntry(javaVersion));
        }

        return new Task(name, javaVersion, mainClass, workingDir, vmArgs, args, resourcePath, classpath, debugSources) {
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
    public TaskBuilder withJavaVersion(int version) {
        this.javaVersion = version;
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
}
