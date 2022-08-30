package io.github.coolcrabs.brachyura.plugins.service.builtin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.tinylog.Logger;

import io.github.coolcrabs.brachyura.processing.ProcessingEntry;
import io.github.coolcrabs.brachyura.processing.ProcessingId;
import io.github.coolcrabs.brachyura.processing.ProcessingSink;
import io.github.coolcrabs.brachyura.processing.ProcessingSource;
import io.github.coolcrabs.brachyura.processing.Processor;

public class Shader implements Processor {

    private final Path[] shadePaths;

    public Shader(Path... shadePaths) {
        this.shadePaths = shadePaths;
    }

    @Override
    public void process(Collection<ProcessingEntry> inputs, ProcessingSink sink) throws IOException {
        // TODO Processesors are confusing and need a rewrite. So do that!
        inputs.forEach(pe -> {
            sink.sink(pe.in, pe.id);
        });
        Set<String> paths = new HashSet<>();
        for (Path shadePath : shadePaths) {
            if (shadePath == null) {
                continue;
            }
            try (JarInputStream jarIn = new JarInputStream(Files.newInputStream(shadePath))) {
                for (JarEntry entry = jarIn.getNextJarEntry(); entry != null; entry = jarIn.getNextJarEntry()) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    if (entry.getName().toLowerCase(Locale.ROOT).endsWith(".mf") || !paths.add(entry.getName())) {
                        continue;
                    }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = jarIn.read(buffer)) != -1) {
                        baos.write(buffer, 0, read);
                    }
                    sink.sink(() -> new ByteArrayInputStream(baos.toByteArray()), new ProcessingId(entry.getName(), new ProcessingSource() {
                        @Override
                        public void getInputs(ProcessingSink sink) {
                            Logger.error(sink.getClass().toGenericString());
                            // ???
                        }
                    }));
                }
            }
        }
    }
}
