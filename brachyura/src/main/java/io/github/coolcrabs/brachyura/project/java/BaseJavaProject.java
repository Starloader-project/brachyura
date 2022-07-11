package io.github.coolcrabs.brachyura.project.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.ide.Ide;
import io.github.coolcrabs.brachyura.ide.IdeModule;
import io.github.coolcrabs.brachyura.project.Project;
import io.github.coolcrabs.brachyura.project.Task;
import io.github.coolcrabs.brachyura.project.TaskBuilder;
import io.github.coolcrabs.brachyura.util.ArrayUtil;
import io.github.coolcrabs.brachyura.util.PathUtil;

public abstract class BaseJavaProject extends Project {

    @NotNull
    public abstract IdeModule[] getIdeModules();

    // Slbrachyura: Improved task handling
    public List<@NotNull Task> getIdeTasks() {
        List<@NotNull Task> tasks = new ArrayList<>();
        for (Ide ide : Ide.getIdes()) {
            tasks.add(new TaskBuilder(ide.ideName(), this)
                    .build((args) -> {
                        BaseJavaProject buildscriptProject = getBuildscriptProject();
                        if (buildscriptProject != null) {
                            ide.updateProject(getProjectDir(), ArrayUtil.join(IdeModule.class, getIdeModules(), buildscriptProject.getIdeModules()));
                        } else {
                            ide.updateProject(getProjectDir(), getIdeModules());
                        }
                    }));
        }
        return tasks;
    }

    @Deprecated // Slbrachyura: Deprecate task handling with consumers
    public final void getIdeTasks(@NotNull Consumer<@NotNull Task> p) {
        getIdeTasks().forEach(p);
    }

    @NotNull
    public Path getBuildLibsDir() {
        return PathUtil.resolveAndCreateDir(getBuildDir(), "libs");
    }

    @NotNull
    public Path getBuildDir() {
        return PathUtil.resolveAndCreateDir(getProjectDir(), "build");
    }

    @SuppressWarnings("null")
    @NotNull
    public Path getSrcDir() {
        return getProjectDir().resolve("src").resolve("main").resolve("java");
    }

    // Slbrachyura: Improved task handling
    @Override
    @NotNull
    public List<@NotNull Task> getTasks() {
        List<@NotNull Task> tasks = super.getTasks();
        tasks.addAll(getIdeTasks());
        return tasks;
    }

    @NotNull
    public Path getResourcesDir() {
        Path resourceDir = getProjectDir().resolve("src").resolve("main").resolve("resources");
        if (!Files.exists(resourceDir)) {
            try {
                Files.createDirectories(resourceDir);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return resourceDir;
    }
}
