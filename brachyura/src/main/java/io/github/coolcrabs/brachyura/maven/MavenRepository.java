package io.github.coolcrabs.brachyura.maven;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MavenRepository {

    @Nullable
    public abstract ResolvedFile resolve(@NotNull String folder, @NotNull String file);
}
