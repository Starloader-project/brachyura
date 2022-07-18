package io.github.coolcrabs.brachyura.ide.source;

import java.io.StringWriter;

import javax.xml.stream.XMLStreamException;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.ide.Eclipse;
import io.github.coolcrabs.brachyura.util.XmlUtil.FormattedXMLStreamWriter;

public class JavaJRESourceLookupEntry implements SourceLookupEntry {

    private int javaVersion;

    public JavaJRESourceLookupEntry(int javaVersion) {
        this.javaVersion = javaVersion;
    }

    @Override
    @NotNull
    @Contract(value = "-> !null", pure = true)
    public String getEclipseJDTType() {
        return "org.eclipse.jdt.launching.sourceContainer.classpathContainer";
    }

    @Override
    @NotNull
    @Contract(value = "-> !null", pure = true)
    public String getEclipseJDTValue() {
        StringWriter writer = new StringWriter();
        try (FormattedXMLStreamWriter xmlWriter = new FormattedXMLStreamWriter(writer)) {
            xmlWriter.writeStartDocument("UTF-8", "1.0");
            xmlWriter.writeEmptyElement("classpathContainer");
            xmlWriter.writeAttribute("path", Eclipse.JDT_JRE_CONTAINER_JVM + getJavaVersion());
            xmlWriter.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
        }
        return writer.toString();
    }

    @Contract(pure = true)
    public int getJavaVersion() {
        return javaVersion;
    }
}
