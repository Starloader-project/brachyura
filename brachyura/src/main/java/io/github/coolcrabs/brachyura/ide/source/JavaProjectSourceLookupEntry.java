package io.github.coolcrabs.brachyura.ide.source;

import java.io.StringWriter;

import javax.xml.stream.XMLStreamException;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.project.java.BuildModule;
import io.github.coolcrabs.brachyura.project.java.SimpleJavaProject;
import io.github.coolcrabs.brachyura.util.XmlUtil.FormattedXMLStreamWriter;

/**
 * Creates a debug source lookup entry that provides the source of another java project.
 * Usually it will be another brachyura build module, but really can be any java project under a given name
 * however doing that increases the risk of the project not running on other computers.
 *
 * <p>Dependencies of that project are excluded and may need to be added manually using
 * {@link JarDepSourceLookupEntry}.
 *
 * <p>It is generally preferable to use this class over {@link JarDepSourceLookupEntry} if possible
 * as those two projects will more or less be linked, making hotswapping easier.
 *
 * @author Geolykt
 * @since 0.90.5
 */
public class JavaProjectSourceLookupEntry implements SourceLookupEntry {

    @NotNull
    private final String projectName;

    public JavaProjectSourceLookupEntry(@NotNull BuildModule module) {
        this.projectName = module.getModuleName();
    }

    public JavaProjectSourceLookupEntry(@NotNull SimpleJavaProject project) {
        this(project.projectModule.get());
    }

    public JavaProjectSourceLookupEntry(@NotNull String projectName) {
        this.projectName = projectName;
    }

    @Override
    @NotNull
    @Contract(value = "-> !null", pure = true)
    public String getEclipseJDTType() {
        return "org.eclipse.jdt.launching.sourceContainer.javaProject";
    }

    @Override
    @NotNull
    @Contract(value = "-> !null", pure = true)
    public String getEclipseJDTValue() {
        StringWriter writer = new StringWriter();
        try (FormattedXMLStreamWriter xmlWriter = new FormattedXMLStreamWriter(writer)) {
            xmlWriter.writeStartDocument("UTF-8", "1.0");
            xmlWriter.writeEmptyElement("javaProject");
            xmlWriter.writeAttribute("name", getProjectName());
            xmlWriter.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
        }
        return writer.toString();
    }

    @NotNull
    @Contract(pure = true)
    public String getProjectName() {
        return projectName;
    }
}
