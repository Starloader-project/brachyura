package io.github.coolcrabs.brachyura.plugins.service.builtin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.plugins.service.ProjectConfigurator;
import io.github.coolcrabs.brachyura.plugins.service.ProjectProviderService;
import io.github.coolcrabs.brachyura.processing.ProcessorChain;
import io.github.coolcrabs.brachyura.project.java.SimpleJavaProject;
import io.github.coolcrabs.brachyura.util.JvmUtil;

public class SlSimpleJavaProjectProviderService implements ProjectProviderService<SimpleJavaProject> {

    private static class SimpleJavaProjectConfigurator implements ProjectConfigurator<SimpleJavaProject> {

        @NotNull
        private final MavenId mavenId;
        private int version = JvmUtil.CURRENT_JAVA_VERSION;
        private final Set<JavaJarDependency> compileDeps = new HashSet<>();
        private final Set<JavaJarDependency> shadeDeps = new HashSet<>();

        public SimpleJavaProjectConfigurator(@NotNull MavenId mavenId) {
            this.mavenId = mavenId;
        }

        @Override
        @NotNull
        public SimpleJavaProject build() {
            return new SimpleJavaProject() {
                @Override
                @NotNull
                public MavenId getId() {
                    return mavenId;
                }

                @Override
                public int getJavaVersion() {
                    return version;
                }

                @Override
                @NotNull
                public List<JavaJarDependency> createDependencies() {
                    return new ArrayList<>(shadeDeps);
                }

                @Override
                public ProcessorChain getResourceProcessorChain() {
                    List<Path> extraShadeDeps = new ArrayList<>();
                    shadeDeps.forEach(dep -> {
                        extraShadeDeps.add(dep.jar);
                    });
                    return new ProcessorChain(super.getResourceProcessorChain(), new Shader(extraShadeDeps.toArray(new Path[0])));
                }
            };
        }

        @Override
        @NotNull
        public ProjectConfigurator<SimpleJavaProject> javaVersion(int version) {
            this.version = version;
            return this;
        }

        @Override
        @NotNull
        public ProjectConfigurator<SimpleJavaProject> withDependencies(DependencyActionType action,
                Collection<JavaJarDependency> deps) {
            if (action == DependencyActionType.JAR_IN_JAR) {
                throw new UnsupportedOperationException("SimpleJavaProject does not support Jar-In-Jars (JIJ)!");
            }
            if (action == DependencyActionType.SHADE) {
                shadeDeps.addAll(deps);
            }
            compileDeps.addAll(deps);
            return this;
        }
    }

    @Override
    @NotNull
    public ProjectConfigurator<SimpleJavaProject> createConfigurator(@NotNull MavenId mavenId) {
        return new SimpleJavaProjectConfigurator(mavenId);
    }
}
