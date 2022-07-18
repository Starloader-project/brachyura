package io.github.coolcrabs.brachyura.ide.source;

import java.io.StringWriter;
import java.nio.file.Path;

import javax.xml.stream.XMLStreamException;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.util.XmlUtil.FormattedXMLStreamWriter;

public class JarDepSourceLookupEntry implements SourceLookupEntry {

    @NotNull
    private final Path sourcesJar;

    public JarDepSourceLookupEntry(@NotNull JavaJarDependency dep) {
        Path sourcesJar = dep.sourcesJar;
        if (sourcesJar == null) {
            throw new IllegalArgumentException("JavaJarDependency has no sources jar attached.");
        }
        this.sourcesJar = sourcesJar;
    }

    @Override
    @NotNull
    @Contract(value = "-> !null", pure = true)
    public String getEclipseJDTType() {
        return "org.eclipse.debug.core.containerType.externalArchive";
    }

    @Override
    @NotNull
    @Contract(value = "-> !null", pure = true)
    public String getEclipseJDTValue() {
        StringWriter writer = new StringWriter();
        try (FormattedXMLStreamWriter xmlWriter = new FormattedXMLStreamWriter(writer)) {
            xmlWriter.writeStartDocument("UTF-8", "1.0");
            xmlWriter.writeEmptyElement("archive");
            xmlWriter.writeAttribute("detectRoot", "true"); // I have no idea what this does
            xmlWriter.writeAttribute("path", sourcesJar.toAbsolutePath().toString());
            xmlWriter.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
        }
        return writer.toString();
    }
}
