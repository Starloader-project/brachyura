package io.github.coolcrabs.brachyura.plugins.service;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.project.Project;

public interface ProjectProviderService<T extends Project> {

    @NotNull
    @Contract(pure = true, value = "-> new")
    public ProjectConfigurator<T> createConfigurator(@NotNull MavenId id);
}
