package io.github.coolcrabs.brachyura.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChecksumViolationException extends IllegalStateException {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = -1553231956218321447L;

    @NotNull
    private final String file;

    public ChecksumViolationException(@NotNull String dataChecksum, @Nullable String recievedChecksum, @NotNull String file) {
        super("Checksum violation: Got " + dataChecksum + " but the maven repository stored " + recievedChecksum + " as the checksum for the file \"" + file + "\"");
        this.file = file;
    }

    @NotNull
    public String getFile() {
        return file;
    }
}