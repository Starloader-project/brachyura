package io.github.coolcrabs.brachyura.exception;

@SuppressWarnings("serial")
public class UnknownJsonException extends RuntimeException {
    public UnknownJsonException(String string) {
        super(string);
    }
}
