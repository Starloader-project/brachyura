package io.github.coolcrabs.brachyura.quilt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.github.coolcrabs.brachyura.decompiler.BrachyuraDecompiler;
import io.github.coolcrabs.brachyura.fabric.FabricContext.ModDependencyCollector;
import io.github.coolcrabs.brachyura.fabric.FabricLoader;
import io.github.coolcrabs.brachyura.mappings.Namespaces;
import io.github.coolcrabs.brachyura.maven.MavenResolver;
import io.github.coolcrabs.brachyura.minecraft.Minecraft;
import io.github.coolcrabs.brachyura.minecraft.VersionMeta;
import io.github.coolcrabs.brachyura.util.JvmUtil;
import io.github.coolcrabs.brachyura.util.PathUtil;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerVisitor;
import net.fabricmc.mappingio.tree.MappingTree;

public class QmQuiltProjectTest {
    SimpleQuiltProject proj = new SimpleQuiltProject() {

        @Override
        public VersionMeta createMcVersion() {
            return Minecraft.getVersion("1.18.2");
        }

        @Override
        public int getJavaVersion() {
            return 17;
        };

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
            resolver.addRepository(QuiltMaven.REPOSITORY);
            return QuiltMappings.ofMaven(resolver, QuiltMaven.quiltMappings("1.18.2+build.22")).toIntermediary(this.context.get().intermediary.get());
        }

        @Override
        public FabricLoader getLoader() {
            return new FabricLoader(QuiltMaven.URL, QuiltMaven.loader("0.16.0-beta.5"));
        }

        @Override
        public void getModDependencies(ModDependencyCollector d) {
            
        }

        @Override
        @NotNull
        public Path getProjectDir() {
            Path result = PathUtil.CWD.resolveSibling("test").resolve("quilt").resolve("testmod_qm");
            assertTrue(Files.isDirectory(result)); 
            return result;
        }

        @Override
        public BrachyuraDecompiler decompiler() {
            return null;
        };
    };

    @Test
    void ide() {
        long a = System.currentTimeMillis();
        proj.getTasks(p -> {
            if (p.name.equals("netbeans")) p.doTask(new String[]{});
            if (p.name.equals("idea")) p.doTask(new String[]{});
            if (p.name.equals("jdt")) p.doTask(new String[]{});
        });
        long b = System.currentTimeMillis();
        System.out.println(b - a);
    }

    @Test
    @Disabled // SlBrachyura: QM is broken - up to upstream to fix
    void compile() {
        if (JvmUtil.CURRENT_JAVA_VERSION >= 17) {
            try {
                proj.build();
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }
}
    
