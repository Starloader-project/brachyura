package io.github.coolcrabs.brachyura.fabric;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.github.coolcrabs.brachyura.TestUtil;
import io.github.coolcrabs.brachyura.decompiler.BrachyuraDecompiler;
import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.mappings.Namespaces;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.maven.MavenResolver;
import io.github.coolcrabs.brachyura.minecraft.Minecraft;
import io.github.coolcrabs.brachyura.minecraft.VersionMeta;
import io.github.coolcrabs.brachyura.util.PathUtil;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerVisitor;
import net.fabricmc.mappingio.tree.MappingTree;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;

class J8FabricProjectTest {
    FabricProject fabricProject = new FabricProject() {
        @Override
        public VersionMeta createMcVersion() {
            return Minecraft.getVersion("1.16.5");
        }

        @Override
        public Consumer<AccessWidenerVisitor> getAw() {
            return (v) -> {
                try {
                    new AccessWidenerReader(v).read(Files.newBufferedReader(getResourcesDir().resolve("testaw.accesswidener")), Namespaces.NAMED);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
        };

        @Override
        public MappingTree createMappings() {
            MavenResolver resolver = new MavenResolver(MavenResolver.MAVEN_LOCAL);
            resolver.addRepository(FabricMaven.REPOSITORY);
            MappingTree tree = Yarn.ofMaven(resolver, FabricMaven.yarn("1.16.5+build.10")).tree;
            return tree;
        }

        @Override
        public FabricLoader getLoader() {
            return new FabricLoader(FabricMaven.URL, FabricMaven.loader("0.12.5"));
        }

        @Override
        @NotNull
        public Path getProjectDir() {
            Path result = PathUtil.CWD.resolveSibling("testmod");
            assertTrue(Files.isDirectory(result)); 
            return result;
        }

        @Override
        public void getModDependencies(ModDependencyCollector d) {
            MavenResolver resolver = new MavenResolver(MavenResolver.MAVEN_LOCAL);
            resolver.addRepository(FabricMaven.REPOSITORY);
            resolver.addRepository(MavenResolver.MAVEN_CENTRAL_REPO);
            d.add(resolver.getJarDepend(new MavenId("org.ini4j", "ini4j", "0.5.4")), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE, ModDependencyFlag.JIJ);
            d.add(resolver.getJarDepend(new MavenId(FabricMaven.GROUP_ID + ".fabric-api", "fabric-resource-loader-v0", "0.4.8+3cc0f0907d")), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE, ModDependencyFlag.JIJ);
            d.add(resolver.getJarDepend(new MavenId(FabricMaven.GROUP_ID + ".fabric-api", "fabric-game-rule-api-v1", "1.0.7+3cc0f0907d")), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE, ModDependencyFlag.JIJ);
        };

       @Override
       public BrachyuraDecompiler decompiler() {
           return null;
       };
    };

    @Test
    @Disabled(value = "Known to fail - Will probably be fixed upstream")
    void compile() {
        try {
            JavaJarDependency b = fabricProject.build();
            TestUtil.assertSha256(b.jar, "c7ee5d98a960e6d49d6fa55bbd3eab2b7de301bc5b0c8be8ad60a8b5de8f86b9");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void ide() {
        //Todo better api for this?
        fabricProject.getTasks(p -> {
            try {
                if (p.name.equals("netbeans")) p.doTask(new String[]{});
                if (p.name.equals("idea")) p.doTask(new String[]{});
                if (p.name.equals("jdt")) p.doTask(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        });
    }

    @Disabled
    @Test
    void bruh() {
        fabricProject.getTasks(p -> {
            if (p.name.equals("runMinecraftClient"))
                try {
                    p.doTask(new String[]{}); 
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }

        });
    }
}
