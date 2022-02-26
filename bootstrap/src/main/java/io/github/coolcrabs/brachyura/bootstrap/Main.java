package io.github.coolcrabs.brachyura.bootstrap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Main {
    public static final int VERSION = 0;
    static final Path BOOTSTRAP_DIR = Paths.get(System.getProperty("user.home")).resolve(".brachyura").resolve("bootstrap");

    public static void main(String[] args) throws Throwable {
        System.out.println("Using brachyura bootstrap " + VERSION);
        // https://stackoverflow.com/a/2837287
        Path projectPath = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        Files.createDirectories(BOOTSTRAP_DIR);
        Path conf = projectPath.resolve("brachyurabootstrapconf.txt");
        List<Path> classpath = new ArrayList<>();
        BufferedReader confReader = null;
        try {
            if (Files.isRegularFile(conf)) {
                confReader = Files.newBufferedReader(conf);
            } else {
                InputStream confis = Main.class.getResourceAsStream("/brachyurabootstrapconf.txt");
                if (confis == null) {
                    throw new RuntimeException("Unable to find brachyurabootstrapconf.txt");
                }
                confReader = new BufferedReader(new InputStreamReader(confis));
            }
            classpath.addAll(getDependencies(confReader, "brachyurabootstrapconf"));
        } finally {
            if (confReader != null) {
                confReader.close();
            }
        }
        classpath.addAll(getBuildscriptDependencies(projectPath));
        URL[] urls = new URL[classpath.size()];
        for (int i = 0; i < classpath.size(); i++) {
            urls[i] = classpath.get(i).toUri().toURL();
        }
        // https://kostenko.org/blog/2019/06/runtime-class-loading.html
        URLClassLoader classLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        Class<?> entry = Class.forName("io.github.coolcrabs.brachyura.project.BrachyuraEntry", true, classLoader);
        MethodHandles.publicLookup().findStatic(
            entry,
            "main",
            MethodType.methodType(void.class, String[].class, Path.class, List.class)
        )
        .invokeExact(args, projectPath, classpath);
    }

    private static Collection<? extends Path> getBuildscriptDependencies(Path projectPath) throws Exception {
        Path buildscriptDir = projectPath.resolve("buildscript");
        if (!Files.isDirectory(buildscriptDir)) {
            return Collections.emptyList();
        }
        Path buildscriptDependsFile = buildscriptDir.resolve("build-dependencies.txt");
        if (!Files.isRegularFile(buildscriptDependsFile)) {
            return Collections.emptyList();
        }
        try (BufferedReader reader = Files.newBufferedReader(buildscriptDependsFile, StandardCharsets.UTF_8)) {
            return getDependencies(reader, "build-dependencies.txt");
        }
    }

    private static Collection<? extends Path> getDependencies(BufferedReader confReader, String name) throws Exception {
        ArrayList<Path> dependencies = new ArrayList<>();
        int confVersion = Integer.parseInt(confReader.readLine());
        if (confVersion != VERSION) {
            throw new RuntimeException("Unsupported " + name + " config version " + confVersion + ". Supported version is " + VERSION + " you need to update or downgrade bootstrap jar to use this brachyura version.");
        }
        String line = null;
        while ((line = confReader.readLine()) != null) {
            if (line.isEmpty()) {
                continue;
            }
            String[] a = line.split("\\s+");
            URL url = new URL(a[0].trim());
            String hash = a[1].trim();
            String fileName = a[2].trim();
            boolean isjar = Boolean.parseBoolean(a[3].trim());
            Path download = getDownload(url, hash, fileName);
            if (isjar) dependencies.add(download);
        }
        return dependencies;
    }

    static Path getDownload(URL url, String hash, String fileName) throws Exception {
        if ("file".equals(url.getProtocol())) return Paths.get(url.toURI()); // For debug usage
        Path target = BOOTSTRAP_DIR.resolve(fileName);
        if (!Files.isRegularFile(target)) {
            Path tempFile = Files.createTempFile(BOOTSTRAP_DIR, hash, ".tmp");
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                try (InputStream is = new DigestInputStream(url.openStream(), md)) {
                    Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }
                String actualHash = toHexHash(md.digest());
                if (hash.equalsIgnoreCase(actualHash)) {
                    Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE);
                } else {
                    throw new RuntimeException("Incorrect hash expected " + hash + " got " + actualHash);
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        return target;
    }

    public static String toHexHash(byte[] hash) {
        final StringBuilder hex = new StringBuilder(2 * hash.length);
        for (final byte b : hash) {
            int x = ((int) b) & 0x00FF;
            if (x < 16) {
                hex.append('0');
            }
            hex.append(Integer.toHexString(x));
        }
        return hex.toString();
    }
}
