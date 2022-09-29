package io.github.coolcrabs.brachyura.fabric;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.EnigmaReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import io.github.coolcrabs.brachyura.dependency.FileDependency;
import io.github.coolcrabs.brachyura.exception.UnreachableException;
import io.github.coolcrabs.brachyura.mappings.Namespaces;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.maven.MavenResolver;
import io.github.coolcrabs.brachyura.util.FileSystemUtil;
import io.github.coolcrabs.brachyura.util.Util;

public class Yarn {
    // Either obf-named or intermediary-named
    public final MappingTree tree;

    private Yarn(MappingTree tree) {
        this.tree = tree;
    }

    @NotNull
    public static Yarn ofV2(Path file) {
        try {
            MemoryMappingTree tree = new MemoryMappingTree(true);
            MappingReader.read(file, MappingFormat.TINY_2, tree);
            return new Yarn(tree);
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    @NotNull
    public static Yarn ofObfEnigma(Path dir) {
        try {
            MemoryMappingTree tree = new MemoryMappingTree(true);
            EnigmaReader.read(dir, Namespaces.OBF, Namespaces.NAMED, tree);
            return new Yarn(tree);
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    @NotNull
    public static Yarn ofV2Jar(@NotNull Path file) {
        try {
            try (FileSystem fileSystem = FileSystemUtil.newJarFileSystem(file)) {
                return ofV2(fileSystem.getPath("mappings/mappings.tiny"));
            }
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    @NotNull
    public static Yarn ofObfEnigmaZip(@NotNull Path file) {
        try {
            try (FileSystem fileSystem = FileSystemUtil.newJarFileSystem(file)) {
                return ofObfEnigma(fileSystem.getPath("/"));
            }
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    @NotNull
    public static Yarn ofMaven(@NotNull MavenResolver resolver, @NotNull MavenId mavenId) {
        Path v2 = resolver.resolveArtifactLocationCached(mavenId, "mergedv2", "jar");
        Path enigma = resolver.resolveArtifactLocationCached(mavenId, "enigma", "zip");
        if (v2 == null && enigma == null) {
            try {
                v2 = resolver.resolveArtifact(mavenId, "mergedv2", "jar").asFileDependency().file;
            } catch (IOException e) {
                try {
                    enigma = resolver.resolveArtifact(mavenId, "enigma", "zip").asFileDependency().file;
                } catch (IOException e1) {
                    IllegalStateException ex = new IllegalStateException("Cannot resolve yarn artifact!");
                    ex.addSuppressed(e1);
                    ex.addSuppressed(e);
                    throw ex;
                }
            }
        }
        if (v2 != null) {
            return ofV2Jar(v2);
        }
        if (enigma != null) {
            return ofObfEnigmaZip(enigma);
        }
        throw new UnreachableException();
    }

    @Deprecated
    @NotNull
    public static Yarn ofMaven(String repo, MavenId id) {
        FileDependency v2 = io.github.coolcrabs.brachyura.maven.Maven.getMavenFileDep(repo, id, "-mergedv2.jar", false);
        FileDependency enigma = io.github.coolcrabs.brachyura.maven.Maven.getMavenFileDep(repo, id, "-enigma.zip", false);
        if (v2 == null && enigma == null) {
            try {
                v2 = io.github.coolcrabs.brachyura.maven.Maven.getMavenFileDep(repo, id, "-mergedv2.jar");
                Util.<FileNotFoundException>unsneak();
            } catch (FileNotFoundException e) {
                enigma = io.github.coolcrabs.brachyura.maven.Maven.getMavenFileDep(repo, id, "-enigma.zip");
            }
        }
        if (v2 != null) {
            return ofV2Jar(v2.file);
        }
        if (enigma != null) {
            return ofObfEnigmaZip(enigma.file);
        }
        throw new UnreachableException();
    }
}
