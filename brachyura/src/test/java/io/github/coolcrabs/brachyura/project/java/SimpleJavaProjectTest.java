package io.github.coolcrabs.brachyura.project.java;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.ide.IdeModule;
import io.github.coolcrabs.brachyura.ide.IdeModule.RunConfigBuilder;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.maven.MavenResolver;
import io.github.coolcrabs.brachyura.util.PathUtil;

class SimpleJavaProjectTest {
    @Test
    void compile() {
        SimpleJavaProject project = new SimpleJavaProject() {
            @Override
            @NotNull
            public MavenId getId() {
                return new MavenId("io.github.coolcrabs", "testprogram", "0.0");
            }

            @Override
            public int getJavaVersion() {
                return 8;
            }

            @Override
            @NotNull
            public Path getProjectDir() {
                return PathUtil.CWD.resolveSibling("testprogram");
            }

            @Override
            @NotNull
            public List<JavaJarDependency> createDependencies() {
                // Slbrachyura start: Migrate legacy Maven code to MavenResolver
                List<JavaJarDependency> jarDeps = new ArrayList<>();
                MavenResolver resolver = new MavenResolver(getLocalBrachyuraPath().resolve("cache"));
                resolver.addRepository(MavenResolver.MAVEN_CENTRAL_REPO);
                jarDeps.add(resolver.getJarDepend(new MavenId("org.junit.platform:junit-platform-console-standalone:1.8.2")));
                return jarDeps;
                // Slbrachyura end
            }

            @Override
            public SimpleJavaModule createProjectModule() {
                return new SimpleJavaProjectModule() {
                    @Override
                    @NotNull
                    public IdeModule ideModule() {
                        return new IdeModule.IdeModuleBuilder()
                            .name(getModuleName())
                            .root(getModuleRoot())
                            .javaVersion(getJavaVersion())
                            .sourcePaths(getSrcDirs())
                            .resourcePaths(getResourceDirs())
                            .testSourcePath(getModuleRoot().resolve("src").resolve("test").resolve("java"))
                            .testResourcePath(getModuleRoot().resolve("src").resolve("test").resolve("resources"))
                            .dependencies(dependencies.get())
                            .dependencyModules(getModuleDependencies().stream().map(BuildModule::ideModule).collect(Collectors.toList()))
                            .runConfigs(
                                new RunConfigBuilder()
                                    .name("bruh")
                                    .cwd(getModuleRoot())
                                    .mainClass("io.github.coolcrabs.testprogram.TestProgram")
                            )
                            .build();
                    }
                };
            }
        };
        //Todo better api for this?
        project.getTasks(p -> {
            if (p.name.equals("jdt")) p.doTask(new String[]{});
            if (p.name.equals("netbeans")) p.doTask(new String[]{});
            if (p.name.equals("idea")) p.doTask(new String[]{});
        });
        assertDoesNotThrow(() -> {
            Assertions.assertNotNull(project.build());
        });
        project.getTasks(p -> {
            if (p.name.equals("publishToMavenLocal")) p.doTask(new String[]{});
        });
    }
}
