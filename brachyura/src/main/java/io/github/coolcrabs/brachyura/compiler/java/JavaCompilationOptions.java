package io.github.coolcrabs.brachyura.compiler.java;

import java.util.ArrayList;
import java.util.Collection;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.util.JvmUtil;

public class JavaCompilationOptions {

    @NotNull
    private Collection<String> rawOptions = new ArrayList<>();

    private int targetVersion = Integer.MIN_VALUE;

    /**
     * Adds an option that should be used for the compilation.
     *
     * @param option The option to add.
     * @return The current instance of {@link JavaCompilation}, for chaining
     */
    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public JavaCompilationOptions addRawOption(@NotNull String option) {
        this.rawOptions.add(option);
        return this;
    }

    /**
     * Adds multiple options that should be used for the compilation.
     *
     * @param options The options to add.
     * @return The current instance of {@link JavaCompilation}, for chaining
     */
    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public JavaCompilationOptions addRawOptions(@NotNull Collection<String> options) {
        this.rawOptions.addAll(options);
        return this;
    }

    /**
     * Commits all compilation options defined in this {@link JavaCompilationOptions} instance
     * to a {@link JavaCompilation}.
     *
     * @param compilation
     * @return The modified parameter, for chaining
     */
    @NotNull
    @Contract(mutates = "param", pure = false, value = "param1 -> param1")
    public JavaCompilation commit(@NotNull JavaCompilation compilation) {
        if (targetVersion != Integer.MIN_VALUE) {
            compilation.addOption(JvmUtil.compileArgs(JvmUtil.CURRENT_JAVA_VERSION, targetVersion));
        }
        return compilation.addOptions(rawOptions);
    }

    /**
     * Creates a clone of this {@link JavaCompilationOptions} instance.
     * If the returned instance is modified the current instance will not be modified and vice versa:
     * they will operate independently.
     *
     * @return The resulting clone
     */
    @NotNull
    @Contract(pure = true, value = "-> new")
    public JavaCompilationOptions copy() {
        return new JavaCompilationOptions().setTargetVersion(targetVersion).addRawOptions(rawOptions);
    }

    /**
     * Sets the target version used in the build. While this does not prevent the usage of newer java features,
     * it does at least allow older java versions to run the resulting class files.
     *
     * <p>Note: current implementation just sets the release version, not the target version
     *
     * @param targetVersion The used target version.
     * @return The current instance of {@link JavaCompilation}, for chaining
     */
    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public JavaCompilationOptions setTargetVersion(int targetVersion) {
        this.targetVersion = targetVersion;
        return this;
    }

    /**
     * Sets the target version used in the build, provided it was not already set.
     * While this does not prevent the usage of newer java features,
     * it does at least allow older java versions to run the resulting class files.
     *
     * <p>Note: current implementation just sets the release version, not the target version
     *
     * @param targetVersion The used target version.
     * @return The current instance of {@link JavaCompilation}, for chaining
     */
    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public JavaCompilationOptions setTargetVersionIfAbsent(int targetVersion) {
        if (this.targetVersion == Integer.MIN_VALUE) {
            this.targetVersion = targetVersion;
        }
        return this;
    }
}
