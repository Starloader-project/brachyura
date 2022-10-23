package io.github.coolcrabs.fernutil;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import io.github.coolcrabs.fernutil.FernUtil.JavadocProvider;
import io.github.coolcrabs.fernutil.FernUtil.LineNumbers;
import net.fabricmc.fernflower.api.IFabricJavadocProvider;

class TJump {
    TJump() { }

    public static class PackageHack {
        PackageHack() { }

        public static void decompile(Path inJar, Path outSources, List<Path> cp, Consumer<LineNumbers> lines, JavadocProvider provider) throws IOException {
            boolean fabric;
            try {
                Class.forName("net.fabricmc.fernflower.api.IFabricResultSaver", false, TJump.class.getClassLoader());
                fabric = true;    
            } catch (Exception e) {
                fabric = false;
            }
            ArrayList<Path> cp0 = new ArrayList<>(cp.size() + 1);
            cp0.add(inJar);
            cp0.addAll(cp);
            try (
                TBytecodeProvider bytecodeProvider = new TBytecodeProvider(cp0);
                TFFResultSaver resultSaver = fabric ? new TFFResultSaverFabric(outSources, lines) : new TFFResultSaver(outSources, lines);
            ) {
                HashMap<String, Object> options = new HashMap<>();
                options.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1");
                options.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1");
                options.put(IFernflowerPreferences.REMOVE_SYNTHETIC, "1");
                options.put(IFernflowerPreferences.INDENT_STRING, "    ");
                options.put(IFernflowerPreferences.NEW_LINE_SEPARATOR, "\n");
                // Threads configured by default in all ff forks
                if (fabric) options.put(IFabricJavadocProvider.PROPERTY_NAME, new TJavadocProviderFabric(provider));
                Class<?> ffClass = Class.forName("org.jetbrains.java.decompiler.main.Fernflower", true, PackageHack.class.getClassLoader());
                MethodType ffCtorType = MethodType.fromMethodDescriptorString("(Lorg/jetbrains/java/decompiler/main/extern/IBytecodeProvider;Lorg/jetbrains/java/decompiler/main/extern/IResultSaver;Ljava/util/Map;Lorg/jetbrains/java/decompiler/main/extern/IFernflowerLogger;)V", ffClass.getClassLoader());
                MethodHandle ffCtorHandle = MethodHandles.publicLookup().findConstructor(ffClass, ffCtorType);
                Object ff = ffCtorHandle.invoke(bytecodeProvider, resultSaver, options, new TFFLogger());
                MethodHandles.publicLookup().bind(ff, "addSource", MethodType.methodType(Void.class, File.class)).invoke(inJar.toFile());
                MethodHandle addLibHandle = MethodHandles.publicLookup().bind(ff, "addLibrary", MethodType.methodType(Void.class, File.class));
                for (Path p : cp) {
                    addLibHandle.invoke(p.toFile());
                }
                MethodHandles.publicLookup().bind(ff, "decompileContext", MethodType.methodType(Void.class)).invoke();
            } catch (Throwable t) {
                if (t instanceof Exception) {
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    throw new IllegalStateException(t);
                } else {
                    sneak(t);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private static <T extends Throwable> void sneak(Throwable t) throws T {
            throw (T) t;
        }
    }
}
