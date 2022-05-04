package io.github.coolcrabs.brachyura.build;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTagObject;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

public class Main {

    static final String ACCESSWIDENER_VERSION = "2.1.0";
    static final String ASM_VERSION = "9.3";
    static final String GSON_VERSION = "2.9.0";
    static final String JAVAPOET_VERSION = "1.13.0"; // Slbrachyura: Interactive project creation
    static final String MAPPINGSIO_VERSION = "0.3.0";
    static final String REMAPPER_VERSION = "0.8.2";
    static final String TINYLOG_VERSION = "2.4.1";

    static String[] localLibs = new String[] {"fabricmerge", "cfr", "brachyura", "bootstrap", "brachyura-mixin-compile-extensions", "trieharder", "fernutil"};
    static String[] mavenLibs = new String[] {
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm/" + ASM_VERSION + "/asm-" + ASM_VERSION + ".jar",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm/" + ASM_VERSION + "/asm-" + ASM_VERSION + "-sources.jar",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/" + ASM_VERSION + "/asm-analysis-" + ASM_VERSION + ".jar",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/" + ASM_VERSION + "/asm-analysis-" + ASM_VERSION + "-sources.jar",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-commons/" + ASM_VERSION + "/asm-commons-" + ASM_VERSION + ".jar",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-commons/" + ASM_VERSION + "/asm-commons-" + ASM_VERSION + "-sources.jar",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/" + ASM_VERSION + "/asm-tree-" + ASM_VERSION + ".jar",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/" + ASM_VERSION + "/asm-tree-" + ASM_VERSION + "-sources.jar",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-util/" + ASM_VERSION + "/asm-util-" + ASM_VERSION + ".jar",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-util/" + ASM_VERSION + "/asm-util-" + ASM_VERSION + "-sources.jar",
        "https://repo.maven.apache.org/maven2/com/google/code/gson/gson/" + GSON_VERSION + "/gson-" + GSON_VERSION + ".jar",
        "https://repo.maven.apache.org/maven2/com/google/code/gson/gson/" + GSON_VERSION + "/gson-" + GSON_VERSION + "-sources.jar",
        "https://repo.maven.apache.org/maven2/org/tinylog/tinylog-api/" + TINYLOG_VERSION + "/tinylog-api-" + TINYLOG_VERSION + ".jar",
        "https://repo.maven.apache.org/maven2/org/tinylog/tinylog-api/" + TINYLOG_VERSION + "/tinylog-api-" + TINYLOG_VERSION + "-sources.jar",
        "https://repo.maven.apache.org/maven2/org/tinylog/tinylog-impl/" + TINYLOG_VERSION + "/tinylog-impl-" + TINYLOG_VERSION + ".jar",
        "https://repo.maven.apache.org/maven2/org/tinylog/tinylog-impl/" + TINYLOG_VERSION + "/tinylog-impl-" + TINYLOG_VERSION + "-sources.jar",
        "https://maven.fabricmc.net/net/fabricmc/mapping-io/" + MAPPINGSIO_VERSION + "/mapping-io-" + MAPPINGSIO_VERSION + ".jar",
        "https://maven.fabricmc.net/net/fabricmc/mapping-io/" + MAPPINGSIO_VERSION + "/mapping-io-" + MAPPINGSIO_VERSION + "-sources.jar",
        "https://maven.fabricmc.net/net/fabricmc/tiny-remapper/" + REMAPPER_VERSION + "/tiny-remapper-" + REMAPPER_VERSION + ".jar",
        "https://maven.fabricmc.net/net/fabricmc/tiny-remapper/" + REMAPPER_VERSION + "/tiny-remapper-" + REMAPPER_VERSION + "-sources.jar",
        "https://maven.fabricmc.net/net/fabricmc/access-widener/" + ACCESSWIDENER_VERSION + "/access-widener-" + ACCESSWIDENER_VERSION + ".jar",
        "https://maven.fabricmc.net/net/fabricmc/access-widener/" + ACCESSWIDENER_VERSION + "/access-widener-" + ACCESSWIDENER_VERSION + "-sources.jar",
        "https://repo.maven.apache.org/maven2/org/jetbrains/annotations/23.0.0/annotations-23.0.0.jar",
        "https://repo.maven.apache.org/maven2/org/jetbrains/annotations/23.0.0/annotations-23.0.0-sources.jar",
        "https://repo1.maven.org/maven2/org/apache/httpcomponents/httpclient/4.5.13/httpclient-4.5.13.jar",
        "https://repo1.maven.org/maven2/org/apache/httpcomponents/httpcore/4.4.15/httpcore-4.4.15.jar",
        "https://repo1.maven.org/maven2/commons-codec/commons-codec/1.15/commons-codec-1.15.jar",
        "https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar",
        "https://repo1.maven.org/maven2/com/squareup/javapoet/" + JAVAPOET_VERSION + "/javapoet-" + JAVAPOET_VERSION + ".jar" // Slbrachyura: Interactive project creation
    };

    static boolean github = Boolean.parseBoolean(System.getenv("CI"));
    static String commit = github ? getCommitHash() : null;
    static final String GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");
    static final String GITHUB_REPOSITORY =
            System.getenv("GITHUB_REPOSITORY") != null ? System.getenv("GITHUB_REPOSITORY") : "CoolCrabs/brachyura";
    static GitHub gitHub2;

    private static void copyBootstrapConfig(Path boostrapJar, Path bootstrapConfig) throws Exception {
        Map<JarEntry, byte[]> data = new HashMap<>();
        try (JarFile jarFile = new JarFile(boostrapJar.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                try (InputStream input = jarFile.getInputStream(entry)) {
                    data.put(entry, readFully(input).toByteArray());
                }
            }
        }
        try (JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(boostrapJar))) {
            for (Map.Entry<JarEntry, byte[]> entry : data.entrySet()) {
                jarOut.putNextEntry(entry.getKey());
                jarOut.write(entry.getValue());
            }
            jarOut.putNextEntry(new JarEntry("brachyurabootstrapconf.txt"));
            jarOut.write(Files.readAllBytes(bootstrapConfig));
        }
    }

    public static void main(String[] args) throws Exception {
        if (github) {
            gitHub2 = new GitHubBuilder().withOAuthToken(GITHUB_TOKEN).build();
        }
        Path workDirectory = Paths.get("").toAbsolutePath();
        Path outDir = workDirectory.resolve("out");
        if (Files.isDirectory(outDir)) {
            deleteDirectory(outDir);
        }
        Files.createDirectories(outDir);

        Path bootstrapJar = null;
        Path boostrapConfig = outDir.resolve("brachyurabootstrapconf.txt");

        try (BufferedWriter bootstrapConfigWriter = Files.newBufferedWriter(boostrapConfig)) {
            bootstrapConfigWriter.write(String.valueOf(0) + "\n");
            for (String lib : localLibs) {
                Path a = workDirectory.resolveSibling(lib).resolve("target");
                Stream<Path> b = Files.walk(a, 1);
                Path jar = 
                    b.filter(p -> p.toString().endsWith(".jar") && !p.toString().endsWith("-sources.jar") && !p.toString().endsWith("-javadoc.jar"))
                    .sorted(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })).findFirst()
                    .orElseThrow(() -> new RuntimeException(lib));
                b.close();
                Path sources = jar.resolveSibling(jar.getFileName().toString().replace(".jar", "-sources.jar"));
                Path targetjar = outDir.resolve(jar.getFileName());
                Path targetSources = outDir.resolve(sources.getFileName());
                if ("bootstrap".equals(lib)) {
                    doLocalDep(jar, targetjar, true);
                    doLocalDep(sources, targetSources, false);
                    bootstrapJar = targetjar;
                } else {
                    if (github) {
                        bootstrapConfigWriter.write(doGithubDep(jar, targetjar, true));
                        bootstrapConfigWriter.write(doGithubDep(sources, targetSources, false));
                    } else {
                        bootstrapConfigWriter.write(doLocalDep(jar, targetjar, true));
                        bootstrapConfigWriter.write(doLocalDep(sources, targetSources, false));
                    }
                }
            }
            for (String lib : mavenLibs) {
                boolean isJar = !lib.endsWith("-sources.jar");
                String filename = lib.substring(lib.lastIndexOf('/') + 1);
                String hash;
                if (!Boolean.getBoolean("de.geolykt.starloader.brachyura.build.offline")) {
                    HttpURLConnection connection = (HttpURLConnection) new URL(lib + ".sha1").openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2"); // hydos moment
                    try (InputStream is = connection.getInputStream()) {
                        hash = readFullyAsString(is);
                    }
                } else {
                    hash = "offline-build";
                }
                bootstrapConfigWriter.write(lib + "\t" + hash + "\t" + filename + "\t" + isJar + "\n");
            }
        }

        copyBootstrapConfig(bootstrapJar, boostrapConfig);

        if (github) {
            uploadGithub(outDir);
        }
    }

    static void uploadGithub(Path outDir) throws Exception {
        GHRepository repo = gitHub2.getRepository(GITHUB_REPOSITORY);
        System.out.println("Creating tag " + commit);
        GHTagObject tag = repo.createTag("v_" + commit, commit, commit, "commit");
        GHRelease release = repo.createRelease(tag.getTag()).commitish(commit).create();
        Files.walkFileTree(outDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                release.uploadAsset(file.toFile(), file.toString().endsWith(".jar") ? "application/zip" : "text/strings");
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static String doLocalDep(Path source, Path target, boolean isJar) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream is = new DigestInputStream(new BufferedInputStream(Files.newInputStream(source)), md)) {
            Files.copy(is, target);
        }
        String hash = toHexHash(md.digest());
        return target.toUri().toString() + "\t" + hash + "\t" + target.getFileName().toString() + "\t" + isJar + "\n";
    }

    static String doGithubDep(Path source, Path target, boolean isJar) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream is = new DigestInputStream(new BufferedInputStream(Files.newInputStream(source)), md)) {
            Files.copy(is, target);
        }
        String hash = toHexHash(md.digest());
        return "https://github.com/" + GITHUB_REPOSITORY + "/releases/download/" + "v_" + commit + "/" + target.getFileName().toString() + "\t" + hash + "\t" + target.getFileName().toString() + "\t" + isJar + "\n";
    }

    static void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static String getCommitHash() {
        try {
            Process process = new ProcessBuilder("git", "rev-parse", "--verify", "HEAD").start();
            String result = readFullyAsString(process.getInputStream()).replaceAll("[^a-zA-Z0-9]","");
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("Git returned " + exit);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String readFullyAsString(InputStream inputStream) throws IOException {
        return readFully(inputStream).toString(StandardCharsets.UTF_8.name());
    }

    // https://stackoverflow.com/a/10505933
    private static ByteArrayOutputStream readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos;
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
