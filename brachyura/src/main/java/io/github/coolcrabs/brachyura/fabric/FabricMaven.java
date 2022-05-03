package io.github.coolcrabs.brachyura.fabric;

import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.maven.HttpMavenRepository;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.maven.MavenRepository;

public class FabricMaven {
    private FabricMaven() { }

    @NotNull
    public static final String URL = "https://maven.fabricmc.net/";

    @NotNull
    public static final MavenRepository REPOSITORY = new HttpMavenRepository(URL);

    @NotNull
    public static final String GROUP_ID = "net.fabricmc";

    @NotNull
    public static MavenId intermediary(@NotNull String version) {
        return new MavenId(GROUP_ID, "intermediary", version);
    }

    @NotNull
    public static MavenId yarn(@NotNull String version) {
        return new MavenId(GROUP_ID, "yarn", version);
    }

    @NotNull
    public static MavenId loader(@NotNull String version) {
        return new MavenId(GROUP_ID, "fabric-loader", version);
    }
}
