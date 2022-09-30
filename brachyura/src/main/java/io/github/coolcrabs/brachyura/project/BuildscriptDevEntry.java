package io.github.coolcrabs.brachyura.project;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.github.coolcrabs.brachyura.plugins.Plugin;
import io.github.coolcrabs.brachyura.plugins.Plugins;

class BuildscriptDevEntry {
    public static void main(String[] args) throws Throwable {
        if (args.length < 3) {
            throw new IllegalStateException("Need at least 3 arguments. Arguments: " + Arrays.toString(args));
        }
        EntryGlobals.setProjectDir(Paths.get(args[0]));
        // Slbrachyura: Suppress null warning by explicitly declaring generics
        EntryGlobals.setCompileDependencies(Arrays.stream(args[1].split(File.pathSeparator)).map(Paths::get).collect(Collectors.<Path>toList()));

        List<Plugin> plugins = Plugins.getPlugins();
        for (Plugin plugin : plugins) {
            plugin.onEntry();
        }
        try {
            Project buildscript;
            try {
                buildscript = (Project) Class.forName("Buildscript").getDeclaredConstructor().newInstance();
                if (buildscript == null) {
                    throw new AssertionError();
                }
            } catch (ClassNotFoundException cnfe) {
                buildscript = new BuildscriptProject().createProject().get();
            }
            // "finalBuildscript" is just "buildscript" but local-class friendly
            Project finalBuildscript = buildscript;
            BuildscriptProject buildscriptProject = new BuildscriptProject() {
                @Override
                protected java.util.Optional<Project> createProject() {
                    return Optional.of(finalBuildscript);
                };
            };
            buildscript.setIdeProject(buildscriptProject);
            buildscript.runTask(args[2]); // Slbrachyura: Cleaner task calling
        } finally {
            for (Plugin plugin : plugins) {
                plugin.onExit();
            }
        }
    }
}
