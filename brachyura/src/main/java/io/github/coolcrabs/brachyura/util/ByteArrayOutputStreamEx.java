package io.github.coolcrabs.brachyura.util;

import java.io.ByteArrayOutputStream;

import org.jetbrains.annotations.NotNull;

/**
 * Allows to avoid copying
 */
public class ByteArrayOutputStreamEx extends ByteArrayOutputStream {
    public byte @NotNull[] buf() {
        return this.buf;
    }
}
