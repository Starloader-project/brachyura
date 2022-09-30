package io.github.coolcrabs.brachyura;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.util.MessageDigestUtil;
import io.github.coolcrabs.brachyura.util.PathUtil;

public class TestUtil {
    public static final Path ROOT;

    static {
        Path p = PathUtil.CWD;
        // Slbrachyura start: More descriptive error messages if something goes wrong
        if (p.getNameCount() == 0) {
            throw new IllegalStateException("Slbrachyura: Running from root! (WHAT?)");
        }
        while (!p.getFileName().toString().equals("brachyura") && !Files.exists(p.resolve(".brachyuradirmarker"))) {
            p = p.getParent();
            if (p == null || p.getNameCount() == 0) {
                throw new IllegalStateException("Slbrachyura: Hit root! (Consider adding a .brachyuradirmarker file); CWD is " + p);
            }
        }
        // Slbrachyura end
        ROOT = p;
    }

    public static void assertSha256(@NotNull Path file, String expected) {
        assertDoesNotThrow(() -> {
            MessageDigest md = MessageDigestUtil.messageDigest(MessageDigestUtil.SHA256);
            try (DigestInputStream i = new DigestInputStream(PathUtil.inputStream(file), md)) {
                byte[] tmp = new byte[1024];
                while (i.read(tmp) != -1);
            }
            assertEquals(expected.toLowerCase(Locale.ENGLISH), MessageDigestUtil.toHexHash(md.digest()));
        });
    }
}
