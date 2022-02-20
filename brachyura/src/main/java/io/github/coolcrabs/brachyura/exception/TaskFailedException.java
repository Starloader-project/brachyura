package io.github.coolcrabs.brachyura.exception;

@SuppressWarnings("serial")
public class TaskFailedException extends RuntimeException {
    public TaskFailedException(String message) {
        super(message);
    }
}
