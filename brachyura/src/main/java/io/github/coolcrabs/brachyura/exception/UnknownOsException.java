package io.github.coolcrabs.brachyura.exception;

@SuppressWarnings("serial")
public class UnknownOsException extends RuntimeException {

    public UnknownOsException(String osString) {
        super(osString);
    }
}
