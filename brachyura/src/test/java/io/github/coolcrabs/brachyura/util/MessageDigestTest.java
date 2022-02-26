package io.github.coolcrabs.brachyura.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class MessageDigestTest {

    @Test
    public void testHexHash() throws Exception {
        byte[] bytes = new byte[8];
        Arrays.fill(bytes, (byte) -16);
        assertEquals(16, MessageDigestUtil.toHexHash(bytes).length());
        Arrays.fill(bytes, (byte) -2);
        assertEquals(16, MessageDigestUtil.toHexHash(bytes).length());
        Arrays.fill(bytes, (byte) 0);
        assertEquals(16, MessageDigestUtil.toHexHash(bytes).length());
        Arrays.fill(bytes, (byte) 16);
        assertEquals(16, MessageDigestUtil.toHexHash(bytes).length());
        Arrays.fill(bytes, (byte) 127);
        assertEquals(16, MessageDigestUtil.toHexHash(bytes).length());
    }
}
