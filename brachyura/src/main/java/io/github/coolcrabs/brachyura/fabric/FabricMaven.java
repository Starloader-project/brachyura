package io.github.coolcrabs.brachyura.fabric;

import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.maven.MavenId;

public class FabricMaven {
    private FabricMaven() { }

    @NotNull
    public static final String URL = "https://maven.fabricmc.net/";

    @NotNull
    public static final String GROUP_ID = "net.fabricmc";

    public static MavenId intermediary(@NotNull String version) {
        return new MavenId(GROUP_ID, "intermediary", version);
    }

    public static MavenId yarn(@NotNull  String version) {
        return new MavenId(GROUP_ID, "yarn", version);
    }

    public static MavenId loader(@NotNull  String version) {
        return new MavenId(GROUP_ID, "fabric-loader", version);
    }

    public static MavenId devLaunchInjector(@NotNull  String version) {
        return new MavenId(GROUP_ID, "dev-launch-injector", version);
    }

    public static MavenId mixinCompileExtensions(@NotNull String version) {
        return new MavenId(GROUP_ID, "fabric-mixin-compile-extensions", version);
    }
}
