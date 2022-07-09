package io.github.coolcrabs.brachyura.project;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.coolcrabs.brachyura.project.java.BaseJavaProject;
import io.github.coolcrabs.brachyura.util.PathUtil;

public class Project {
    BaseJavaProject buildscriptIdeProject;

    public void getTasks(@NotNull Consumer<@NotNull Task> p) {
        // no default tasks
    }

    public final void runTask(String name, String... args) {
        // Slbrachyura start: Improved task system
        AtomicBoolean foundTask = new AtomicBoolean();
        getTasks(task -> {
            if (task.name.equals(args[2])) {
                task.doTask(new String[]{});
                foundTask.set(true);
            }
        });
        if (!foundTask.get()) {
            throw new NoSuchElementException("Unable to get task with given name: " + name);
        }
        // Slbrachyura end
    }

    @SuppressWarnings("null") // There are circumstances that this is null, but we are going to ignore these
    @NotNull
    public Path getProjectDir() {
        return EntryGlobals.projectDir;
    }

    @NotNull
    public Path getLocalBrachyuraPath() {
        return PathUtil.resolveAndCreateDir(getProjectDir(), ".brachyura");
    }

    /**
     * Note: usually, this method returns null.
     *
     * <p>The only place where it does not return null is when the buildscript is invoked or something like that.
     *
     * @return Unknown
     */
    @Nullable
    public BaseJavaProject getBuildscriptProject() {
        return buildscriptIdeProject;
    }

    void setIdeProject(BaseJavaProject ideProject) {
        this.buildscriptIdeProject = ideProject;
    }
}
