package io.github.coolcrabs.brachyura.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.GZIPOutputStream;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class PathUtil {
    private PathUtil() { }

    public static final Path HOME = Paths.get(System.getProperty("user.home"));
    public static final Path CWD = Paths.get("").toAbsolutePath();
    private static Path brachyuraPath;

    @NotNull
    @Contract(value = "-> !null", pure = true)
    public static Path brachyuraPath() {
        Path brachyuraPath = PathUtil.brachyuraPath;
        if (brachyuraPath != null) {
            return brachyuraPath;
        }
        // Follow https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html
        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        Path home;
        boolean hide = false;
        if (xdgDataHome == null || xdgDataHome.isEmpty()) {
            // "If $XDG_DATA_HOME is either not set or empty, a default equal to $HOME/.local/share should be used."
            Path userhome = Paths.get(System.getProperty("user.home"));
            Path share = userhome.resolve(".local").resolve("share");
            if (Files.exists(share)) {
                home = share;
            } else {
                // Probably on windows or another OS that does not work with the XDG spec
                String appdataLocationString = System.getenv("appdata");
                if (appdataLocationString == null) {
                    // Does not work with the XDG Spec, but also not on windows
                    home = userhome;
                    hide = true;
                } else {
                    Path windowsAppdataFolder = Paths.get(appdataLocationString);
                    if (Files.exists(windowsAppdataFolder)) {
                        // Make it in the appdata folder
                        home = windowsAppdataFolder;
                    } else {
                        // Just make it relative to the user home
                        home = userhome;
                        hide = true;
                    }
                }
            }
        } else {
            home = Paths.get(xdgDataHome);
        }
        if (hide) {
            brachyuraPath = home.resolve(".brachyura");
        } else {
            brachyuraPath = home.resolve("brachyura");
        }
        brachyuraPath = brachyuraPath.resolve("starloader");
        PathUtil.brachyuraPath = brachyuraPath;
        return brachyuraPath;
    }

    @NotNull
    public static Path cachePath() {
        return brachyuraPath().resolve("cache");
    }

    @NotNull
    public static Path resolveAndCreateDir(Path parent, @NotNull String child) {
        try {
            Path result = parent.resolve(child);
            Files.createDirectories(result);
            return result;
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    public static void deleteDirectoryChildren(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    public static void copyDir(Path source, Path target) {
        Path a = pathTransform(target.getFileSystem(), source);
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path targetFile = target.resolve(a.relativize(pathTransform(target.getFileSystem(), file)));
                    Files.createDirectories(targetFile.getParent());
                    Files.copy(file, targetFile);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    // https://stackoverflow.com/a/22611925
    @NotNull
    public static Path pathTransform(FileSystem fs, final Path path) {
        Path ret = fs.getPath(path.isAbsolute() ? fs.getSeparator() : "");
        for (Path component : path) {
            ret = ret.resolve(component.getFileName().toString());
        }
        return ret;
    }

    @NotNull
    public static InputStream inputStream(@NotNull Path path) {
        try {
            return new BufferedInputStream(Files.newInputStream(path));
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    public static OutputStream outputStream(Path path) {
        try {
            Files.createDirectories(path.getParent());
            return new BufferedOutputStream(Files.newOutputStream(path));
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    /**
     * Returns a temp file in the same directory as the target file
     */
    @NotNull
    public static Path tempFile(Path target) {
        try {
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
            if (temp == null) {
                throw new NullPointerException("Nullabillity violation"); // Slbrachyura: Geolykt is too dumb to figure out EEA
            }
            return temp;
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    public static Path tempDir(Path target) {
        try {
            Files.createDirectories(target.getParent());
            return Files.createTempDirectory(target.getParent(), target.getFileName().toString());
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    public static void moveAtoB(Path a, Path b) {
        try {
            Files.move(a, b, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.delete(b);
            } catch (Exception e2) {
                // File prob wasn't created
            }
            throw Util.sneak(e);
        }
    }

    public static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    public static void deleteDirectory(Path dir) {
        try {
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
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    public static BufferedWriter newBufferedWriter(Path path) {
        try {
            Files.createDirectories(path.getParent());
            return Files.newBufferedWriter(path);
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    public static BufferedWriter newBufferedWriter(Path path, OpenOption... options) {
        try {
            Files.createDirectories(path.getParent());
            return Files.newBufferedWriter(path, options);
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    public static BufferedReader newBufferedReader(Path path) {
        try {
            return Files.newBufferedReader(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    public static BufferedWriter newGzipBufferedWriter(Path path) {
        try {
            Files.createDirectories(path.getParent());
            return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path)), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }

    public static void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw Util.sneak(e);
        }
    }
}
