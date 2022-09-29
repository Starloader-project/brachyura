package io.github.coolcrabs.brachyura.util;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

/**
 * Just a lazy with acute dementia.
 * Purely used to preserve ABI with upstream when changes that were deemed to be backwards were
 * neutered so they wouldn't have a crippling effect.
 *
 * @author Geolykt
 * @deprecated Anything relying on this class should be deprecated too as they have confusing behaviour.
 */
@Deprecated
public final class DementiaLazy<T> extends Lazy<T> {

    private final Supplier<T> supplier;

    public DementiaLazy(Supplier<T> supplier) {
        super(supplier);
        this.supplier = supplier;
    }

    @SuppressWarnings("null")
    @Override
    @NotNull
    public T get() {
        return supplier.get();
    }
}
