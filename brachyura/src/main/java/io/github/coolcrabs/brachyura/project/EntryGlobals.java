package io.github.coolcrabs.brachyura.project;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

class EntryGlobals {
    private EntryGlobals() { }

    private static Path projectDir;
    private static List<Path> buildscriptClasspath;

    @NotNull
    static List<Path> getCompileDependencies(boolean performFallback) {
        List<Path> buildscriptClasspath = EntryGlobals.buildscriptClasspath;
        if (buildscriptClasspath == null) {
            if (performFallback) {
                List<Path> cp = new ArrayList<>();
                String classpathStr = System.getProperty("java.class.path");
                String[] cpEntries = classpathStr.split(File.pathSeparator);
                for (String cpEntry : cpEntries) {
                    cp.add(Paths.get(cpEntry));
                }
                return cp;
            }
            throw new IllegalStateException("The buildscript classpath was not set. Did you call BuildscriptDevEntry#main or BrachyuraEntry#main?");
        }
        return buildscriptClasspath;
    }

    static void setCompileDependencies(List<Path> deps) {
        EntryGlobals.buildscriptClasspath = deps;
    }

    static void setProjectDir(Path dir) {
        EntryGlobals.projectDir = dir;
    }

    @NotNull
    static Path getProjectDir() {
        Path projectDir = EntryGlobals.projectDir;
        if (projectDir == null) {
            throw new IllegalStateException("The project directory was not set. Did you call BuildscriptDevEntry#main or BrachyuraEntry#main?");
        }
        return projectDir;
    }
}
