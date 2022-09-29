package io.github.coolcrabs.brachyura.project.java;

import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.ide.IdeModule;
import io.github.coolcrabs.brachyura.processing.ProcessingSource;
import io.github.coolcrabs.brachyura.util.Lazy;

/**
 * Represents sources that are compiled together
 */
public abstract class BuildModule {
    public int getJavaVersion() {
        return 8;
    }

    @NotNull
    public abstract String getModuleName();

    @NotNull
    public abstract Path getModuleRoot();

    public final Lazy<ProcessingSource> compilationOutput = new Lazy<>(this::createCompilationOutput);
    protected abstract ProcessingSource createCompilationOutput();

    @Deprecated // use #ideModule instead
    public final Lazy<IdeModule> ideModule = new Lazy<>(this::createIdeModule);

    @Deprecated // use #ideModule instead
    protected final IdeModule createIdeModule() {
        return this.ideModule();
    }

    @NotNull
    public abstract IdeModule ideModule();
}
