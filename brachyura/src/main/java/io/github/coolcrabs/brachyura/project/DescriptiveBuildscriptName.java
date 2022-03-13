package io.github.coolcrabs.brachyura.project;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Interface that can be applied on any class that extends {@link Project}. This is used
 * to dynamically determine the name of the buildscript project as used in IDEs. Therefore
 * any subclasses of {@link BuildscriptProject} should not implement this method.
 *
 * <p>The main intention behind this interface is to prevent collisions of the name of two
 * projects that are technically unique but for the IDE they share the same name, which
 * can result in issues such as the IDE only accepting one buildscript project.
 */
public interface DescriptiveBuildscriptName {

    /**
     * Obtains the name of the buildscript project as shown in IDEs. The default is "Buildscript".
     * While technically ANY string can be returned, it is highly recommended to return a non-changing
     * string and for this method to be functionally pure.
     * As such multiple invocations must return the same string.
     *
     * <p>Failure to do so may result in buggy behaviour in the IDE or future brachyura code as well as any other code
     * that may depend on this method. It probably will not catastrophically fail.
     *
     * @return The name of the buildscript project as used by IDEs.
     */
    @NotNull
    @Contract(pure = true)
    public String getBuildscriptName();
}
