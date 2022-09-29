package io.github.coolcrabs.brachyura.quilt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import io.github.coolcrabs.brachyura.TestUtil;
import io.github.coolcrabs.brachyura.decompiler.BrachyuraDecompiler;
import io.github.coolcrabs.brachyura.fabric.FabricContext.ModDependencyCollector;
import io.github.coolcrabs.brachyura.fabric.FabricLoader;
import io.github.coolcrabs.brachyura.maven.MavenResolver;
import io.github.coolcrabs.brachyura.minecraft.Minecraft;
import io.github.coolcrabs.brachyura.minecraft.VersionMeta;
import io.github.coolcrabs.brachyura.util.JvmUtil;
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
        public MappingTree createMappings() {
            MavenResolver resolver = new MavenResolver(MavenResolver.MAVEN_LOCAL);
            resolver.addRepository(QuiltMaven.REPOSITORY);
            return QuiltMappings.ofMaven(resolver, QuiltMaven.quiltMappings("1.18.2+build.24")).toIntermediary(this.context.get().intermediary.get());
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
            Path result = TestUtil.ROOT.resolve("test").resolve("quilt").resolve("testmod_qm");
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
        try {
            proj.runTask("netbeans");
            proj.runTask("idea");
            proj.runTask("jdt");
        } catch (Exception e) {
            e.printStackTrace(); // Slbrachyura: print stacktraces, properly
            throw e;
        }
        long b = System.currentTimeMillis();
        System.out.println(b - a);
    }

    @Test
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
