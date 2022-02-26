package io.github.coolcrabs.brachyura.project.java;

import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.exception.CompilationFailure;

@FunctionalInterface
public interface BuildSupplier {

    @NotNull
    JavaJarDependency get() throws CompilationFailure;
}
