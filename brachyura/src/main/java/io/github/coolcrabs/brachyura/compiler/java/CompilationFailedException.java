package io.github.coolcrabs.brachyura.compiler.java;

import org.jetbrains.annotations.NotNull;

public class CompilationFailedException extends RuntimeException {

    private static final long serialVersionUID = 7555474203258502895L; // Slbrachyura: Add serialVersionUID

    public CompilationFailedException() {
        // Default-constructor
    }

    public CompilationFailedException(@NotNull Throwable t) {
        super(t);
    }
}
