package io.github.coolcrabs.brachyura.fabric;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.tree.MappingTree;

import io.github.coolcrabs.brachyura.decompiler.BrachyuraDecompiler;
import io.github.coolcrabs.brachyura.fabric.FabricContext.ModDependencyCollector;
import io.github.coolcrabs.brachyura.fabric.FabricContext.ModDependencyFlag;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.maven.MavenResolver;
import io.github.coolcrabs.brachyura.minecraft.Minecraft;
import io.github.coolcrabs.brachyura.minecraft.VersionMeta;
import io.github.coolcrabs.brachyura.util.JvmUtil;
import io.github.coolcrabs.brachyura.util.PathUtil;

class MojmapProjectTest {
    SimpleFabricProject fabricProject = new SimpleFabricProject() {
        @Override
        public VersionMeta createMcVersion() {
            return Minecraft.getVersion("1.19-pre1");
        }

        @Override
        public int getJavaVersion() {
            return 17;
        };

        @Override
        public MappingTree createMappings() {
            return createMojmap();
        }

        @Override
        public FabricLoader getLoader() {
            return new FabricLoader(FabricMaven.URL, FabricMaven.loader("0.14.6"));
        }

        @Override
        @NotNull
        public Path getProjectDir() {
            Path result = PathUtil.CWD.resolveSibling("test").resolve("fabric").resolve("mojmap");
            assertTrue(Files.isDirectory(result)); 
            return result;
        }

        @Override
        public void getModDependencies(ModDependencyCollector d) {
            MavenResolver resolver = new MavenResolver(MavenResolver.MAVEN_LOCAL);
            resolver.addRepository(FabricMaven.REPOSITORY);
            resolver.addRepository(MavenResolver.MAVEN_CENTRAL_REPO);
            jij(d.add(resolver.getJarDepend(new MavenId(FabricMaven.GROUP_ID + ".fabric-api", "fabric-resource-loader-v0", "0.5.0+4a05de7f73")), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE));
            jij(d.add(resolver.getJarDepend(new MavenId(FabricMaven.GROUP_ID + ".fabric-api", "fabric-game-rule-api-v1", "1.0.16+ec94c6f673")), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE));
            jij(d.add(resolver.getJarDepend(new MavenId(FabricMaven.GROUP_ID + ".fabric-api", "fabric-registry-sync-v0", "0.9.12+56447d9b73")), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE));
            jij(d.add(resolver.getJarDepend(new MavenId(FabricMaven.GROUP_ID + ".fabric-api", "fabric-api-base", "0.4.7+f71b366f73")), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE));
            jij(d.add(resolver.getJarDepend(new MavenId(FabricMaven.GROUP_ID + ".fabric-api", "fabric-networking-api-v1", "1.0.24+f71b366f73")), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE));
            jij(d.add(resolver.getJarDepend(new MavenId("org.ini4j:ini4j:0.5.4")), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE));
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
        long a = System.currentTimeMillis();
        try {
            fabricProject.runTask("netbeans");
            fabricProject.runTask("idea");
            fabricProject.runTask("jdt");
        } catch (Exception e) {
            e.printStackTrace(); // Slbrachyura: print stacktraces, properly
            throw e;
        }
        long b = System.currentTimeMillis();
        System.out.println(b - a);
    }

    @Test
    void compile() {
        if (JvmUtil.CURRENT_JAVA_VERSION >= 16) {
            try {
                fabricProject.build();
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }
}
