package io.github.coolcrabs.brachyura.minecraft;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.ConnectException;

import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import io.github.coolcrabs.brachyura.util.Util;

class LauncherMetaDownloaderTest {
    @Test
    void downloadLauncherMeta() {
        assertDoesNotThrow(() -> {
            try {
                LauncherMeta meta = LauncherMetaDownloader.getLauncherMeta();
                assertNotNull(meta.latest.release);
                Util.<ConnectException>unsneak();
            } catch (ConnectException e) {
                Logger.warn("Connection failure!");
                e.printStackTrace();
                return;
            }
        });
    }
}
