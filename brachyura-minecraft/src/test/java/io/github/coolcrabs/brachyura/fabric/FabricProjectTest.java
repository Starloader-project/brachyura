package io.github.coolcrabs.brachyura.fabric;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.tree.MappingTree;

import io.github.coolcrabs.brachyura.TestUtil;
import io.github.coolcrabs.brachyura.decompiler.BrachyuraDecompiler;
import io.github.coolcrabs.brachyura.fabric.FabricContext.ModDependencyCollector;
import io.github.coolcrabs.brachyura.fabric.FabricContext.ModDependencyFlag;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.maven.MavenResolver;
import io.github.coolcrabs.brachyura.minecraft.Minecraft;
import io.github.coolcrabs.brachyura.minecraft.VersionMeta;
import io.github.coolcrabs.brachyura.util.JvmUtil;

class FabricProjectTest {
    SimpleFabricProject fabricProject = new SimpleFabricProject() {
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
            resolver.addRepository(FabricMaven.REPOSITORY);
            MappingTree tree = Yarn.ofMaven(resolver, FabricMaven.yarn("1.18.2+build.2")).tree;
            return tree;
        }

        @Override
        public FabricLoader getLoader() {
            return new FabricLoader(FabricMaven.URL, FabricMaven.loader("0.13.3"));
        }

        @Override
        @NotNull
        public Path getProjectDir() {
            Path result = TestUtil.ROOT.resolve("testmod");
            assertTrue(Files.isDirectory(result)); 
            return result;
        }

        @Override
        public void getModDependencies(ModDependencyCollector d) {
            MavenResolver resolver = new MavenResolver(MavenResolver.MAVEN_LOCAL);
            resolver.addRepository(FabricMaven.REPOSITORY);
            resolver.addRepository(MavenResolver.MAVEN_CENTRAL_REPO);
            d.add(resolver.getJarDepend(new MavenId(FabricMaven.GROUP_ID + ".fabric-api", "fabric-resource-loader-v0", "0.4.18+2de5574560")), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE);
            d.add(resolver.getJarDepend(new MavenId(FabricMaven.GROUP_ID + ".fabric-api", "fabric-game-rule-api-v1", "1.0.13+d7c144a860")), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE);
            d.add(resolver.getJarDepend(new MavenId(FabricMaven.GROUP_ID + ".fabric-api", "fabric-registry-sync-v0", "0.9.8+0d9ab37260")), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE);
            d.add(resolver.getJarDepend(new MavenId("org.ini4j", "ini4j", "0.5.4")), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE);
        };

        @Override
        public BrachyuraDecompiler decompiler() {
            return null;
            // return new CfrDecompiler();
            // return new FernflowerDecompiler(Maven.getMavenJarDep("https://maven.quiltmc.org/repository/release", new MavenId("org.quiltmc:quiltflower:1.7.0"))); 
        };
    };

    @Test
    void testProject() {
        assertTrue(Files.isRegularFile(fabricProject.context.get().intermediaryjar.get().jar));
        assertTrue(Files.isRegularFile(fabricProject.context.get().namedJar.get().jar));
        // assertTrue(Files.isRegularFile(fabricProject.getDecompiledJar().jar));
    }
    
    @Test
    void ide() {
        try {
            fabricProject.runTask("netbeans");
            fabricProject.runTask("idea");
            fabricProject.runTask("jdt");
        } catch (Exception e) {
            e.printStackTrace(); // Slbrachyura: print stacktraces, properly
            throw e;
        }
    }

    @Test
    void compile() {
        if (JvmUtil.CURRENT_JAVA_VERSION >= 17) {
            try {
                fabricProject.build();
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }
}
