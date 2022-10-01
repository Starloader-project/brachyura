package io.github.coolcrabs.brachyura.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

public class JvmUtil {
    private JvmUtil() { }

    public static final int CURRENT_JAVA_VERSION;

    @NotNull
    public static final String CURRENT_JAVA_EXECUTABLE;

    @NotNull
    private static final String @NotNull[] NO_ARGS = new String[0];

    private static final Set<Long> SUPPORTED_RELEASE_VERSIONS = new HashSet<>();

    static {

        // Based on com.sun.tools.javac.platform.JDKPlatformProvider
        Path ctSymFile = Paths.get(System.getProperty("java.home")).resolve("lib/ct.sym");

        if (Files.exists(ctSymFile)) {
            try (FileSystem fs = FileSystems.newFileSystem(ctSymFile, (ClassLoader)null);
                 DirectoryStream<Path> dir =
                         Files.newDirectoryStream(fs.getRootDirectories().iterator().next())) {
                for (Path section : dir) {
                    if (section.getFileName().toString().contains("-"))
                        continue;
                    System.out.println("Supported JDK: " + Long.parseUnsignedLong(section.getFileName().toString(), Character.MAX_RADIX));
                    SUPPORTED_RELEASE_VERSIONS.add(Long.parseUnsignedLong(section.getFileName().toString(), Character.MAX_RADIX));
                }
            } catch (IOException | ProviderNotFoundException ex) {
                ex.printStackTrace();
            }
        }

        // https://stackoverflow.com/a/2591122
        // Changed to java.specification.version to avoid -ea and other various odditites
        // See https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/System.html#getProperties()
        // and https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runtime.Version.html
        String version = System.getProperty("java.specification.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        CURRENT_JAVA_VERSION = Integer.parseInt(version);
        // https://stackoverflow.com/a/46852384
        String javaHome = System.getProperty("java.home");
        File bin = new File(javaHome, "bin");
        File exe = new File(bin, "java");
        if (exe.exists()) {
            CURRENT_JAVA_EXECUTABLE = exe.getAbsolutePath();
        } else {
            exe = new File(bin, "java.exe");
            if (exe.exists()) {
                CURRENT_JAVA_EXECUTABLE = exe.getAbsolutePath();
            } else {
                // Give Up
                CURRENT_JAVA_EXECUTABLE = "java";
                Logger.error("Unable to find java executable in java.home");
            }
        }
    }

    public static boolean canCompile(int compilerversion, int targetversion) {
        return compilerversion == targetversion || (compilerversion >= 9 && targetversion >= 7);
    }

    @NotNull
    public static String javaVersionString(int javaversion) {
        return javaversion < 9 ? "1." + javaversion : Integer.toString(javaversion);
    }

    @NotNull
    public static String @NotNull[] compileArgs(int compilerversion, int targetversion) {
        if (compilerversion == targetversion) return NO_ARGS;
        if (compilerversion >= 9 && targetversion >= 7) {
            if (SUPPORTED_RELEASE_VERSIONS.contains(Long.valueOf(compilerversion))) {
                return new @NotNull String[] {"--release", String.valueOf(targetversion)};
            } else {
                return new @NotNull String[] {"--target", String.valueOf(targetversion), "--source", String.valueOf(targetversion)};
            }
        }
        throw new UnsupportedOperationException("Target Version: " + targetversion + " " + "Compiler Version: " + compilerversion);
    }
}
