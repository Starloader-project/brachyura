package io.github.coolcrabs.brachyura.util;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

public class JvmUtil {
    private JvmUtil() { }

    public static final int CURRENT_JAVA_VERSION;

    @NotNull
    public static final String CURRENT_JAVA_EXECUTABLE;

    @NotNull
    private static final String @NotNull[] NO_ARGS = new String[0];

    static {
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
        if (compilerversion >= 9 && targetversion >= 7) return new @NotNull String[] {"--release", String.valueOf(targetversion)}; // Doesn't accept 1.8 etc for some reason
        throw new UnsupportedOperationException("Target Version: " + targetversion + " " + "Compiler Version: " + compilerversion);
    }
}
