package io.github.coolcrabs.brachyura.maven.publish;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.xml.stream.XMLStreamException;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.dependency.MavenDependency;
import io.github.coolcrabs.brachyura.dependency.MavenDependencyScope;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.util.XmlUtil;

public class MavenPublisher {

    @NotNull
    private final List<PublishRepository> repositories = new ArrayList<>();

    @NotNull
    @Contract(mutates = "this", pure = false, value = "!null -> this; null -> fail")
    public MavenPublisher addRepository(@NotNull PublishRepository repo) {
        this.repositories.add(Objects.requireNonNull(repo, "repo is null"));
        return this;
    }

    private byte @NotNull[] generatePom(MavenId artifact, @NotNull Collection<? extends MavenDependency> dependencies) throws PublicationException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XmlUtil.FormattedXMLStreamWriter w = XmlUtil.newStreamWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            w.writeStartDocument("UTF-8", "1.0");
            w.newline();
            w.writeStartElement("project");
            w.writeAttribute("xsi:schemaLocation", "http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd");
            w.writeAttribute("xmlns", "http://maven.apache.org/POM/4.0.0");
            w.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            w.indent();
            w.newline();
                w.writeStartElement("modelVersion");
                w.writeCharacters("4.0.0");
                w.writeEndElement();
                w.newline();
                w.writeStartElement("groupId");
                w.writeCharacters(artifact.groupId);
                w.writeEndElement();
                w.newline();
                w.writeStartElement("artifactId");
                w.writeCharacters(artifact.artifactId);
                w.writeEndElement();
                w.newline();
                w.writeStartElement("version");
                w.writeCharacters(artifact.version);
                w.writeEndElement();
                writeDependenciesBlock(dependencies, w);
            w.unindent();
            w.newline();
            w.writeEndElement();
            w.newline();
        } catch (XMLStreamException e) {
            throw new PublicationException("Cannot generate maven pom", e);
        }
        return out.toByteArray();
    }

    /**
     * Obtains the list of publication repositories that are currently used by this publisher.
     * This returns a clone or a read-only representation of the internal list.
     * Either way, the returned list should not be mutated.
     *
     * @return A {@link List} of {@link PublishRepository PublishRepositories} that are used by this {@link MavenPublisher} instance.
     */
    @NotNull
    @Contract(pure = true, value = "-> new")
    public List<PublishRepository> getPublicationRepositories() {
        return new ArrayList<>(repositories);
    }

    /**
     * Publishes the jar, the sources (as far as present) and a created pom to all connected repository.
     * This method is not fail-fast, that is it will try to publish to all repositories and will then
     * throw an {@link PublicationException} f at least one publication attempt errored out. The cause
     * of that exception will be the first encountered {@link IOException}, any further {@link IOException}
     * are added as {@link Throwable#addSuppressed(Throwable) suppressed exceptions}. If any non-IOException
     * throwable is thrown, the method does not proceed further as it makes no attempt at catching such exceptions.
     *
     * <p>Should there be an issue writing the maven pom, a {@link PublicationException} will also be thrown.
     * No files will be published in that case as the pom is the first file that is published. After that the
     * jar itself is published and then the source jar - should it be present.
     *
     * @param artifact The artifact to publish
     * @param dependencies The dependencies of the artifact, used to generate the dependency section of the pom
     * @throws PublicationException Exception that is raised should there be an issue actually pushing the resources to the
     * publication repositories.
     */
    public void publishJar(@NotNull JavaJarDependency artifact, @NotNull Collection<? extends MavenDependency> dependencies) throws PublicationException{
        byte[] pomContent = generatePom(artifact.getMavenId(), dependencies);
        PublicationException pubEx = null;
        for (PublishRepository repo : this.repositories) {
            try {
                repo.publish(new PublicationId(artifact.getMavenId(), "pom"), pomContent);
            } catch (IOException e) {
                if (pubEx == null) {
                    pubEx = new PublicationException(repo, e);
                } else {
                    pubEx.addSuppressed(e);
                }
            }
            try {
                repo.publish(new PublicationId(artifact.getMavenId(), "jar"), artifact.jar);
            } catch (IOException e) {
                if (pubEx == null) {
                    pubEx = new PublicationException(repo, e);
                } else {
                    pubEx.addSuppressed(e);
                }
            }
            try {
                Path source = artifact.sourcesJar;
                if (source != null) {
                    repo.publish(new PublicationId(artifact.getMavenId().withClassifier("sources"), "jar"), source);
                }
            } catch (IOException e) {
                if (pubEx == null) {
                    pubEx = new PublicationException(repo, e);
                } else {
                    pubEx.addSuppressed(e);
                }
            }
        }
        if (pubEx != null) {
            throw pubEx;
        }
    }

    private void writeDependenciesBlock(@NotNull Collection<? extends MavenDependency> dependencies, @NotNull XmlUtil.FormattedXMLStreamWriter out) throws XMLStreamException {
        out.newline();
        out.writeStartElement("dependencies");
        for (MavenDependency dependency : dependencies) {
            if (dependency.getScope() == MavenDependencyScope.COMPILE_ONLY) {
                continue;
            }
            out.indent();
            out.newline();
            writeDependency(dependency, out);
            out.unindent();
        }
        out.newline();
        out.writeEndElement();
    }

    private void writeDependency(MavenDependency dependency, @NotNull XmlUtil.FormattedXMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement("dependency");
        out.indent();
        out.newline();
            out.writeStartElement("groupId");
            out.writeCharacters(dependency.getMavenId().groupId);
            out.writeEndElement();
            out.newline();
            out.writeStartElement("artifactId");
            out.writeCharacters(dependency.getMavenId().artifactId);
            out.writeEndElement();
            out.newline();
            out.writeStartElement("version");
            out.writeCharacters(dependency.getMavenId().version);
            out.writeEndElement();
            out.newline();
            out.writeStartElement("scope");
            out.writeCharacters(dependency.getScope().toString().toLowerCase(Locale.ENGLISH));
            out.writeEndElement();
        out.unindent();
        out.newline();
        out.writeEndElement();
    }
}
