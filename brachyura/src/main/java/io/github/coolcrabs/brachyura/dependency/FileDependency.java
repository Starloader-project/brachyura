package io.github.coolcrabs.brachyura.dependency;

import java.nio.file.Path;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public class FileDependency implements Dependency {
    @NotNull
    public final Path file;

    public FileDependency(@NotNull Path file) {
        Objects.requireNonNull(file);
        this.file = file;
    }
}
