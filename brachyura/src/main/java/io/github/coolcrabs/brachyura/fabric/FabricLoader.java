package io.github.coolcrabs.brachyura.fabric;

import java.io.Reader;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;

import io.github.coolcrabs.brachyura.dependency.FileDependency;
import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.maven.HttpMavenRepository;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.maven.MavenResolver;
import io.github.coolcrabs.brachyura.util.Util;

public class FabricLoader {
    @NotNull
    public final JavaJarDependency jar;
    public final JavaJarDependency[] clientDeps;
    public final JavaJarDependency[] commonDeps;
    public final JavaJarDependency[] serverDeps;

    public FabricLoader(@NotNull String mavenRepo, @NotNull MavenId id) {
        MavenResolver mavenResolver = new MavenResolver(MavenResolver.MAVEN_LOCAL);
        if (mavenRepo.startsWith("http")) {
            mavenResolver.addRepository(new HttpMavenRepository(mavenRepo));
        } else {
            throw new IllegalArgumentException("Unsupported protocol: " + mavenRepo);
        }

        try {
            JavaJarDependency jarDep = mavenResolver.getJarDepend(id);
            if (jarDep == null) {
                throw new IllegalStateException("Unable to resolve maven jar artifact: " + id.toString());
            }
            this.jar = jarDep;
            FileDependency jsonFile = mavenResolver.resolveArtifact(id, "", "json").asFileDependency();
            FloaderMeta floaderMeta;
            try (Reader jsonReader = Files.newBufferedReader(jsonFile.file)) {
                floaderMeta = new Gson().fromJson(jsonReader, FloaderMeta.class);
            }
            Set<String> addedRepos = new HashSet<>();
            clientDeps = new JavaJarDependency[floaderMeta.libraries.client.length];
            for (int i = 0; i < floaderMeta.libraries.client.length; i++) {
                FloaderMeta.Dep dep = floaderMeta.libraries.client[i];
                if (addedRepos.add(dep.url)) {
                    // This is a sub-optimal approach
                    mavenResolver.addRepository(new HttpMavenRepository(dep.url));
                }
                clientDeps[i] = mavenResolver.getJarDepend(new MavenId(dep.name));
            }
            commonDeps = new JavaJarDependency[floaderMeta.libraries.common.length];
            for (int i = 0; i < floaderMeta.libraries.common.length; i++) {
                FloaderMeta.Dep dep = floaderMeta.libraries.common[i];
                if (addedRepos.add(dep.url)) {
                    // This is a sub-optimal approach
                    mavenResolver.addRepository(new HttpMavenRepository(dep.url));
                }
                commonDeps[i] = mavenResolver.getJarDepend(new MavenId(dep.name));
            }
            serverDeps = new JavaJarDependency[floaderMeta.libraries.server.length];
            for (int i = 0; i < floaderMeta.libraries.server.length; i++) {
                FloaderMeta.Dep dep = floaderMeta.libraries.server[i];
                if (addedRepos.add(dep.url)) {
                    // This is a sub-optimal approach
                    mavenResolver.addRepository(new HttpMavenRepository(dep.url));
                }
                serverDeps[i] = mavenResolver.getJarDepend(new MavenId(dep.name));
            }
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    class FloaderMeta {
        Libraries libraries;

        class Libraries {
            Dep[] client;
            Dep[] common;
            Dep[] server;
        }

        class Dep {
            @NotNull
            String name;
            @NotNull
            String url;

            public Dep() {
                this.name = "";
                this.url = "";
            }
        }
    }
}
