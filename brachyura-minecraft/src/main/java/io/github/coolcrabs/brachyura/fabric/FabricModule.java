package io.github.coolcrabs.brachyura.fabric;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.compiler.java.JavaCompilation;
import io.github.coolcrabs.brachyura.compiler.java.JavaCompilationOptions;
import io.github.coolcrabs.brachyura.compiler.java.JavaCompilationResult;
import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.ide.IdeModule;
import io.github.coolcrabs.brachyura.mappings.MappingHasher;
import io.github.coolcrabs.brachyura.mappings.Namespaces;
import io.github.coolcrabs.brachyura.minecraft.Minecraft;
import io.github.coolcrabs.brachyura.processing.ProcessingEntry;
import io.github.coolcrabs.brachyura.processing.ProcessingSource;
import io.github.coolcrabs.brachyura.processing.sources.ProcessingSponge;
import io.github.coolcrabs.brachyura.project.TaskBuilder;
import io.github.coolcrabs.brachyura.project.java.BuildModule;
import io.github.coolcrabs.brachyura.util.AtomicFile;
import io.github.coolcrabs.brachyura.util.JvmUtil;
import io.github.coolcrabs.brachyura.util.Lazy;
import io.github.coolcrabs.brachyura.util.OsUtil;
import io.github.coolcrabs.brachyura.util.PathUtil;
import io.github.coolcrabs.brachyura.util.Util;
import io.github.coolcrabs.brachyura.util.OsUtil.Os;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.Tiny2Writer;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

//TODO the mixin related stuff should be put in a class shared with other toolchains using mixin
public abstract class FabricModule extends BuildModule {
    public final FabricContext context;

    public abstract Path[] getSrcDirs();
    public abstract Path[] getResourceDirs();

    protected List<BuildModule> getModuleDependencies() {
        return Collections.emptyList();
    }

    protected FabricModule(FabricContext context) {
        this.context = context;
    }

    public static class FabricCompilationResult {
        final ProcessingSource processingSource;
        final JavaCompilationResult javaCompilationResult;
        final MemoryMappingTree mixinMappings;

        public FabricCompilationResult(ProcessingSource processingSource, JavaCompilationResult javaCompilationResult, MemoryMappingTree mixinMappings) {
            this.processingSource = processingSource;
            this.javaCompilationResult = javaCompilationResult;
            this.mixinMappings = mixinMappings;
        }
    }

    @Override
    protected ProcessingSource createCompilationOutput() {
        return fabricCompilationResult.get().processingSource;
    }

    public final Lazy<FabricCompilationResult> fabricCompilationResult = new Lazy<>(this::createFabricCompilationResult);

    @NotNull
    @Contract(pure = true, value = "-> this")
    protected abstract JavaCompilationOptions getExtraCompileOptions();

    protected FabricCompilationResult createFabricCompilationResult() {
        try {
            String mixinOut = "mixinmapout.tiny";
            JavaCompilation compilation0 = new JavaCompilation()
                .addOption(JvmUtil.compileArgs(JvmUtil.CURRENT_JAVA_VERSION, getJavaVersion()))
                .addOption(
                    "-AbrachyuraInMap=" + writeMappings4FabricStuff().toAbsolutePath().toString(),
                    "-AbrachyuraOutMap=" + mixinOut, // Remaps shadows etc
                    "-AbrachyuraInNamespace=" + Namespaces.NAMED,
                    "-AbrachyuraOutNamespace=" + Namespaces.INTERMEDIARY,
                    "-AoutRefMapFile=" + getModuleName() + "-refmap.json", // Remaps annotations
                    "-AdefaultObfuscationEnv=brachyura"
                )
                .addClasspath(context.getCompileDependencies())
                .addSourceDir(getSrcDirs())
                .addOptions(getExtraCompileOptions());
            for (BuildModule m : getModuleDependencies()) {
                compilation0.addClasspath(m.compilationOutput.get());
            }
            JavaCompilationResult compilation = compilation0.compile();
            ProcessingSponge compilationOutput = new ProcessingSponge();
            compilation.getInputs(compilationOutput);
            ProcessingEntry mixinMappings = compilationOutput.popEntry(mixinOut);
            MemoryMappingTree mixinMappingsTree = null;
            if (mixinMappings != null) {
                mixinMappingsTree = new MemoryMappingTree();
                try (Reader reader = new InputStreamReader(mixinMappings.in.get())) {
                    MappingReader.read(reader, MappingFormat.TINY_2, mixinMappingsTree);
                }
            }
            return new FabricCompilationResult(compilationOutput, compilation, mixinMappingsTree);
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    @SuppressWarnings("null")
    @Override
    @NotNull
    public IdeModule ideModule() {
        Path cwd = PathUtil.resolveAndCreateDir(getModuleRoot(), "run");
        Lazy<List<Path>> classpath = new Lazy<>(() -> {
            Path mappingsClasspathChild = writeMappings4FabricStuff().getParent();
            if (mappingsClasspathChild == null) {
                throw new NullPointerException("Ex0003");
            }
            Path mappingsClasspath = mappingsClasspathChild.getParent();
            ArrayList<Path> r = new ArrayList<>(context.runtimeDependencies.get().size() + 1);
            for (JavaJarDependency dependency : context.runtimeDependencies.get()) {
                r.add(dependency.jar);
            }
            r.add(mappingsClasspath);
            return r;
        });
        return new IdeModule.IdeModuleBuilder()
            .name(getModuleName())
            .root(getModuleRoot())
            .javaVersion(getJavaVersion())
            .dependencies(context.ideDependencies)
            .sourcePaths(getSrcDirs())
            .resourcePaths(getResourceDirs())
            .dependencyModules(getModuleDependencies().stream().map(BuildModule::ideModule).collect(Collectors.toList()))
            .addTask(new TaskBuilder("Minecraft Client", cwd)
                    .withMainClass("net.fabricmc.loader.launch.knot.KnotClient")
                    .withClasspath(classpath.get())
                    .withResourcePath(Arrays.asList(getResourceDirs()))
                    .withVMArgs(this.ideVmArgs(true))
                    .withArgs(this.ideArgs(true))
                    .withJavaVersion(getJavaVersion())
                    .buildUnconditionallyThrowing())
            .addTask(new TaskBuilder("Minecraft Server", cwd)
                    .withMainClass("net.fabricmc.loader.launch.knot.KnotServer")
                    .withClasspath(classpath.get())
                    .withResourcePath(Arrays.asList(getResourceDirs()))
                    .withVMArgs(this.ideVmArgs(false))
                    .withArgs(this.ideArgs(false))
                    .withJavaVersion(getJavaVersion())
                    .buildUnconditionallyThrowing())
            .build();
    }

    public Path writeMappings4FabricStuff() {
        try {
            MappingTree mappingTree = context.mappings.get();
            String hash = MappingHasher.hashSha256(mappingTree);
            Path result = getLocalBrachyuraPath().resolve("fabric-mappings-cache").resolve(hash).resolve("mappings").resolve("mappings.tiny"); // floader hardcoded path as it asumes you are using a yarn jar as mapping root of truth
            if (!Files.isRegularFile(result)) {
                try (AtomicFile atomicFile = new AtomicFile(result)) {
                    try (Tiny2Writer tiny2Writer = new Tiny2Writer(Files.newBufferedWriter(atomicFile.tempPath), false)) {
                        mappingTree.accept(tiny2Writer);
                    }
                    atomicFile.commit();
                }
            }
            return result;
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    public List<String> ideVmArgs(boolean client) {
        try {
            ArrayList<String> r = new ArrayList<>();
            r.add("-Dfabric.development=true");
            r.add("-Dfabric.remapClasspathFile=" + context.runtimeRemapClasspath.get());
            r.add("-Dlog4j.configurationFile=" + writeLog4jXml());
            r.add("-Dlog4j2.formatMsgNoLookups=true");
            r.add("-Dfabric.log.disableAnsi=false");
            if (client) {
                String natives = context.extractedNatives.get().stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
                r.add("-Djava.library.path=" + natives);
                r.add("-Dtorg.lwjgl.librarypath=" + natives);
                if (OsUtil.OS == Os.OSX) {
                    r.add("-XstartOnFirstThread");
                }
            }
            return r;
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    public List<String> ideArgs(boolean client) {
        ArrayList<String> r = new ArrayList<>();
        if (client) {
            r.add("--assetIndex");
            r.add(context.downloadedAssets.get());
            r.add("--assetsDir");
            r.add(Minecraft.assets().toAbsolutePath().toString());
        }
        return r;
    }

    public Path writeLog4jXml() throws IOException {
        Path result = getLocalBrachyuraPath().resolve("1_log4j.xml");
        if (!Files.exists(result)) Files.copy(this.getClass().getResourceAsStream("/log4j2.fabric.xml"), result);
        return result;
    }

    public Path getLocalBrachyuraPath() {
        return PathUtil.resolveAndCreateDir(getModuleRoot(), ".brachyura");
    }
}
