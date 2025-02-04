package io.github.coolcrabs.brachyura.processing;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public final class ProcessingId {
    @NotNull
    public final String path;
    public final ProcessingSource source;

    public ProcessingId(@NotNull String path, ProcessingSource source) {
        this.path = path;
        this.source = source;
    }

    @Override
    public String toString() {
        return "ProcessingId [path=" + path + ", source=" + source + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, source);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProcessingId) {
            ProcessingId o = (ProcessingId) obj;
            return path.equals(o.path) && source.equals(o.source);
        }
        return false;
    }
}
