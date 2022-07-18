package io.github.coolcrabs.brachyura.ide.source;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface SourceLookupEntry {

    @NotNull
    @Contract(value = "-> !null", pure = true)
    public String getEclipseJDTType();

    @NotNull
    @Contract(value = "-> !null", pure = true)
    public String getEclipseJDTValue();
}
