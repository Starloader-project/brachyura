package io.github.coolcrabs.brachyura.project;

import java.nio.file.Path;
import java.util.List;

import org.jetbrains.annotations.NotNull;

class EntryGlobals {
    private EntryGlobals() { }

    static Path projectDir;
    static List<Path> buildscriptClasspath;

    @NotNull
    static List<Path> getCompileDependencies() {
        List<Path> buildscriptClasspath = EntryGlobals.buildscriptClasspath;
        if (buildscriptClasspath == null) {
            throw new IllegalStateException("The buildscript classpath was not set. Did you call BuildscriptDevEntry#main or BrachyuraEntry#main?");
        }
        return buildscriptClasspath;
    }
}
