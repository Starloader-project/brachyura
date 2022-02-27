package io.github.coolcrabs.brachyura.dependency;

import java.nio.file.Path;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public class NativesJarDependency implements Dependency {
    @NotNull
    public final Path jar;

    public NativesJarDependency(@NotNull Path jar) {
        Objects.requireNonNull(jar);
        this.jar = jar;
    }
}
