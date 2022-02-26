package io.github.coolcrabs.brachyura.exception;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("serial")
public class TaskFailedException extends RuntimeException {
    public TaskFailedException(@NotNull String message) {
        super(message);
    }

    public TaskFailedException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }
}
