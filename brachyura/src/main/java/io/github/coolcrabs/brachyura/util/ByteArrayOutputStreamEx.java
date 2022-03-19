package io.github.coolcrabs.brachyura.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.jetbrains.annotations.NotNull;

/**
 * Allows to avoid copying
 */
public class ByteArrayOutputStreamEx extends ByteArrayOutputStream {
    public byte @NotNull[] buf() {
        return this.buf;
    }

    public InputStream toIs() {
        return new ByteArrayInputStream(buf, 0, size());
    }
}
