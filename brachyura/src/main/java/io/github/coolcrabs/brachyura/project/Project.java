package io.github.coolcrabs.brachyura.project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.coolcrabs.brachyura.project.java.BaseJavaProject;
import io.github.coolcrabs.brachyura.util.PathUtil;

public abstract class Project { // Slbrachyura: Make the Project class abstract
    BaseJavaProject buildscriptIdeProject;

    @Deprecated // Slbrachyura: Deprecate task handling with consumers
    public final void getTasks(@NotNull Consumer<@NotNull Task> p) {
        getTasks().forEach(p);
    }

    // Slbrachyura start: Improved task system
    /**
     * Obtains a list of all tasks. Must be mutable if the class is meant to be extended or
     * used for libraries.
     *
     * @return A list of all tasks registered in this project
     */
    @NotNull
    public List<@NotNull Task> getTasks() {
        return new ArrayList<>();
    }

    public final void runTask(String name, String... args) {
        boolean foundTask = false;
        for (Task task : getTasks()) {
            if (task.name.equals(name)) {
                task.doTask(args);
                foundTask = true;
            }
        }
        if (!foundTask) {
            throw new NoSuchElementException("Unable to get task with given name: " + name);
        }
        // Slbrachyura end
    }

    @NotNull
    public Path getProjectDir() {
        return EntryGlobals.getProjectDir();
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
