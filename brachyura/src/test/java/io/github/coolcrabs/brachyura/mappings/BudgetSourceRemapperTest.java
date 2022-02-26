package io.github.coolcrabs.brachyura.mappings;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.github.coolcrabs.brachyura.fabric.FabricMaven;
import io.github.coolcrabs.brachyura.fabric.Yarn;
import io.github.coolcrabs.brachyura.maven.MavenResolver;
import io.github.coolcrabs.brachyura.util.StreamUtil;
import io.github.coolmineman.trieharder.FindReplaceSourceRemapper;
import net.fabricmc.mappingio.tree.MappingTree;

class BudgetSourceRemapperTest {
    @Test
    void smallYarn() throws Exception {
        MappingTree tree = Yarn.ofMaven(new MavenResolver(MavenResolver.MAVEN_LOCAL).addRepository(FabricMaven.REPOSITORY), FabricMaven.yarn("1.16.5+build.10")).tree;
        FindReplaceSourceRemapper remapper = new FindReplaceSourceRemapper(tree, tree.getNamespaceId(Namespaces.INTERMEDIARY), tree.getNamespaceId(Namespaces.NAMED));
        try (InputStream is = getClass().getResourceAsStream("/PlantInAJar1_16_Intermediary.java")) {
            String remapped = remapper.remapString(StreamUtil.readFullyAsString(is));
            assertFalse(remapped.contains("class_"));
            assertFalse(remapped.contains("method_"));
            assertFalse(remapped.contains("field_"));
        }
    }

    @Test
    void smallYarn2() throws Exception {
        MappingTree tree = Yarn.ofMaven(new MavenResolver(MavenResolver.MAVEN_LOCAL).addRepository(FabricMaven.REPOSITORY), FabricMaven.yarn("1.16.5+build.10")).tree;
        FindReplaceSourceRemapper remapper = new FindReplaceSourceRemapper(tree, tree.getNamespaceId(Namespaces.INTERMEDIARY), tree.getNamespaceId(Namespaces.NAMED));
        try (InputStream is = getClass().getResourceAsStream("/PlantInAJar1_16_Intermediary.java")) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String remapped = remapper.remapString(line);
                    System.out.println(remapped);
                }
            }
        }
    }

    @Test
    void microYarn() throws Exception {
        assertDoesNotThrow(() -> {
            MappingTree tree = Yarn.ofMaven(new MavenResolver(MavenResolver.MAVEN_LOCAL).addRepository(FabricMaven.REPOSITORY), FabricMaven.yarn("1.16.5+build.10")).tree;
            new FindReplaceSourceRemapper(tree, tree.getNamespaceId(Namespaces.INTERMEDIARY), tree.getNamespaceId(Namespaces.NAMED));
        });
    }
}
