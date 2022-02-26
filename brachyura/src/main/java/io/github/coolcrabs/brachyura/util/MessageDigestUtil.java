package io.github.coolcrabs.brachyura.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.jetbrains.annotations.NotNull;

public class MessageDigestUtil {
    private MessageDigestUtil() { }

    public static final String SHA1 = "SHA-1";
    public static final String SHA256 = "SHA-256";

    public static MessageDigest messageDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    @SuppressWarnings("null")
    @NotNull
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

    public static void update(MessageDigest md, String string) {
        if (string != null) {
            md.update(string.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void update(MessageDigest md, int i) {
        md.update(
            new byte[] {
                (byte)(i >>> 24),
                (byte)(i >>> 16),
                (byte)(i >>> 8),
                (byte)i
            }
        );
    }

    public static void update(MessageDigest md, long i) {
        md.update(
            new byte[] {
                (byte)(i >>> 56),
                (byte)(i >>> 48),
                (byte)(i >>> 40),
                (byte)(i >>> 32),
                (byte)(i >>> 24),
                (byte)(i >>> 16),
                (byte)(i >>> 8),
                (byte)i
            }
        );
    }
}
