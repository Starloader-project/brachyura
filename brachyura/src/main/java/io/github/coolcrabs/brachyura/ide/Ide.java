package io.github.coolcrabs.brachyura.ide;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;

public interface Ide {
    public static Ide[] getIdes() {
        return new Ide[] {
            Netbeans.INSTANCE,
            Intellijank.INSTANCE,
            Eclipse.INSTANCE
        };
    }

    static void validate(IdeModule... ideModules) {
        HashSet<IdeModule> modules = new HashSet<>(Arrays.asList(ideModules));
        HashSet<String> names = new HashSet<>();
        for (IdeModule m0 : modules) {
            if (!names.add(m0.name)) throw new IllegalArgumentException("Duplicate modules for name " + m0.name);
            for (IdeModule m1 : m0.dependencyModules) {
                if (!modules.contains(m1)) throw new IllegalArgumentException("Module " + m0.name + " references module " + m1.name + ", which is not listed as a dependency module");
            }
            /* TODO Slbrachyura: Replace if needed
            for (RunConfig rc : m0.runConfigs) {
                for (IdeModule m1 : rc.additionalModulesClasspath) {
                    if (!modules.contains(m1)) throw new IllegalArgumentException("Module " + m0.name + " references module " + m1.name + " not in ide project in a run config");
                }
            }
            */
            // Fail early for lazy
            m0.dependencies.get();
        }
    } 

    @NotNull
    String ideName();
    void updateProject(Path projectRoot, IdeModule[] ideModules);
}
