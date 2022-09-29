package io.github.coolcrabs.brachyura.quilt;

import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.maven.HttpMavenRepository;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.maven.MavenRepository;

public class QuiltMaven {
    private QuiltMaven() { }

    public static final String URL = "https://maven.quiltmc.org/repository/release/";

    @NotNull
    public static final MavenRepository REPOSITORY = new HttpMavenRepository(URL);

    public static final String GROUP_ID = "org.quiltmc";

    @NotNull
    public static MavenId loader(@NotNull String version) {
        return new MavenId(GROUP_ID, "quilt-loader", version);
    }

    @NotNull
    public static MavenId quiltMappings(@NotNull String version) {
        return new MavenId(GROUP_ID, "quilt-mappings", version);
    }
}
