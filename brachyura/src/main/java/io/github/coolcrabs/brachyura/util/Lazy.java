package io.github.coolcrabs.brachyura.util;

import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

// Based on https://dzone.com/articles/be-lazy-with-java-8
// Modified to take the supplier in the constructor
@SuppressWarnings("all")
public class Lazy<T> implements Supplier<T> {

    private volatile T value;
    private final Supplier<T> supplier;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    @NotNull
    public T get() {
        final T result = value; // Just one volatile read 
        return result == null ? maybeCompute() : result;
    }

    private synchronized T maybeCompute() {
        if (value == null) {
            value = Objects.requireNonNull(supplier.get());
        }
        return value;
    }

}
