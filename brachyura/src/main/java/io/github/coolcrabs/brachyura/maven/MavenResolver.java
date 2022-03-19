package io.github.coolcrabs.brachyura.maven;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.util.PathUtil;

/**
 * A small primitive resolver for maven artifacts.
 * All artifacts are cached to a local folder. Artifacts that do not exist
 * are not cached however.
 *
 * @author Geolykt
 */
public class MavenResolver {

    // FIXME while this usually works, this is not exactly right. See https://stackoverflow.com/a/47833316
    @SuppressWarnings("null")
    @NotNull
    public static final Path MAVEN_LOCAL = PathUtil.HOME.resolve(".m2").resolve("repository");

    @NotNull
    public static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/";
    @NotNull
    public static final MavenRepository MAVEN_CENTRAL_REPO = new HttpMavenRepository(MAVEN_CENTRAL);

    /**
     * Set of all artifacts that may not be resolved via {@link #getTransitiveDependencies(MavenId)}.
     * This means that all transitive child dependencies of the listed artifacts are also not resolved,
     * unless they are included via other means (such as being declared by another artifact or by
     * being directly resolved via the {@link #getTransitiveDependencies(MavenId)} method).
     *
     * <p>Any artifacts in this set may still get obtained directly via {@link #getJarDepend(MavenId)}.
     */
    @NotNull
    private final Set<VersionlessMavenId> blacklistedArtifacts = new HashSet<>();

    @NotNull
    private final List<MavenRepository> repositories = new ArrayList<>();
    @NotNull
    private final Path cacheFolder;
    private boolean resolveTestDependencies = false;

    public MavenResolver(@NotNull Path cacheFolder) {
        this.cacheFolder = cacheFolder;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public MavenResolver addRepository(@NotNull MavenRepository repository) {
        this.repositories.add(repository);
        return this;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "!null -> this; null -> fail")
    public MavenResolver addRepositories(@NotNull Collection<MavenRepository> repositories) {
        this.repositories.addAll(repositories);
        return this;
    }

    @Nullable
    public Path resolveArtifactLocationCached(@NotNull MavenId artifact, @NotNull String classifier, @NotNull String extension) {
        String folder = artifact.groupId.replace('.', '/') + '/' + artifact.artifactId + '/' + artifact.version + '/';
        String nameString;
        if (classifier.isEmpty()) {
            nameString = artifact.artifactId + '-' + artifact.version + '.' + extension;
        } else {
            nameString = artifact.artifactId + '-' + artifact.version + '-' + classifier + '.' + extension;
        }
        Path cacheFile = cacheFolder.resolve(folder).resolve(nameString);
        if (Files.exists(cacheFile)) {
            return cacheFile;
        }
        return null;
    }

    @NotNull
    public ResolvedFile resolveArtifact(@NotNull MavenId artifact, @NotNull String classifier, @NotNull String extension) throws IOException {
        String folder = artifact.groupId.replace('.', '/') + '/' + artifact.artifactId + '/' + artifact.version + '/';
        String nameString;
        if (classifier.isEmpty()) {
            nameString = artifact.artifactId + '-' + artifact.version + '.' + extension;
        } else {
            nameString = artifact.artifactId + '-' + artifact.version + '-' + classifier + '.' + extension;
        }
        ResolvedFile directFile = resolveFileContents(folder, nameString);
        if (directFile != null) {
            return directFile;
        }

        ResolvedFile mavenMeta = resolveFileContents(folder, "maven-metadata.xml");
        if (mavenMeta != null) {
            // TODO evaluate using the maven meta
            throw new UnsupportedOperationException("Not yet supported!");
        } else {
            throw new IOException("Unable to resolve artifact: maven-metadata.xml is missing and the file"
                    + " was not able to be fetched directly.");
        }
    }

    @Nullable
    private ResolvedFile resolveFileContents(@NotNull String folder, @NotNull String file) {
        Path cacheFile = cacheFolder.resolve(folder).resolve(file);
        if (Files.exists(cacheFile)) {
            try {
                ResolvedFile f = new ResolvedFile(null, Files.readAllBytes(cacheFile));
                f.setCachePath(cacheFile);
                return f;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (MavenRepository repo : repositories) {
            ResolvedFile resolved = repo.resolve(folder, file);
            if (resolved != null) {
                IOException symlinkEx = null;
                if (resolved.getCachePath() != null) {
                    // Create symbol link to the resolved file
                    try {
                        Files.createDirectories(cacheFile.getParent());
                        Files.createSymbolicLink(cacheFile, resolved.getCachePath());
                        return resolved;
                    } catch (IOException e) {
                        // Cannot create symbolic link
                        symlinkEx = e;
                    }
                }
                try {
                    Files.createDirectories(cacheFile.getParent());
                    Files.write(cacheFile, resolved.data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                    resolved.setCachePath(cacheFile);
                } catch (IOException e) {
                    IllegalStateException toThrow = new IllegalStateException("Unable to write to cache", e);
                    toThrow.addSuppressed(symlinkEx);
                    throw toThrow;
                }
                return resolved;
            }
        }
        return null;
    }

    @Contract(mutates = "this", pure = false, value = "_ -> this")
    @NotNull
    public MavenResolver setResolveTestDependencies(boolean resolveTestDependencies) {
        this.resolveTestDependencies = resolveTestDependencies;
        return this;
    }

    private void getTransitiveDependencyVersions(@NotNull MavenId artifact, @NotNull Map<VersionlessMavenId, MavenId> versions) {
        VersionlessMavenId verlessMavenId = new VersionlessMavenId(artifact.groupId, artifact.artifactId);
        if (versions.containsKey(verlessMavenId)) {
            String currentver = versions.get(verlessMavenId).version;
            String[] versionOld = currentver.split("\\.");
            String[] versionNew = artifact.version.split("\\.");
            int minLen = Math.min(versionOld.length, versionNew.length);
            for (int i = 0; i < minLen; i++) {
                String versionPartOld = versionOld[i];
                String versionPartNew = versionNew[i];
                if (versionPartOld.length() > versionPartNew.length()) {
                    return;
                } else if (versionPartNew.length() == versionPartOld.length()) {
                    int cmp = versionPartOld.compareTo(versionPartNew);
                    if (cmp == 0) {
                        continue;
                    } else if (cmp < 0) {
                        // Currently queried one is newer
                        break;
                    } else {
                        // Currently queried one is older
                        return;
                    }
                } else {
                    break;
                }
            }
            if (versionOld.length > versionNew.length) {
                return;
            } else if (versionOld.length == versionNew.length) {
                return;
            }
        }
        versions.put(verlessMavenId, artifact);
        try {
            Document xmlDoc;
            {
                ResolvedFile file = resolveArtifact(artifact, "", "pom");
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                xmlDoc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(file.data));
            }
            Element project = xmlDoc.getDocumentElement();
            project.normalize();
            NodeList dependencies;
            Map<String, String> placeholders = new HashMap<>();
            {
                NodeList children = project.getChildNodes();
                Element dependenciesBlock = null;
                for (int i = 0; i < children.getLength(); i++) {
                    Node block = children.item(i);
                    if (!block.hasChildNodes() || !(block instanceof Element)) {
                        continue;
                    }
                    Element blockElem = (Element) block;
                    if (blockElem.getTagName().equals("dependencies")) {
                        if (dependenciesBlock != null) {
                            throw new IllegalStateException("Pom for artifact " + artifact.toString() + " contains multiple "
                                    + "dependencies blocks.");
                        }
                        dependenciesBlock = blockElem;
                    } else if (blockElem.getTagName().equals("properties")) {
                        NodeList properties = blockElem.getChildNodes();
                        for (int j = 0; j < properties.getLength(); j++) {
                            Node property = properties.item(j);
                            if (property instanceof Element) {
                                Element propertyElement = (Element) property;
                                placeholders.put("${" + propertyElement.getTagName() + "}", propertyElement.getTextContent());
                            }
                        }
                    }
                }
                if (dependenciesBlock == null) {
                    return;
                }
                dependencies = dependenciesBlock.getChildNodes();
            }

            for (int i = 0; i < dependencies.getLength(); i++) {
                Node depend = dependencies.item(i);
                if (!depend.hasChildNodes()) {
                    continue;
                }
                Element elem = (Element) depend;
                if (!elem.getTagName().equals("dependency")) {
                    continue;
                }
                String groupId = elem.getElementsByTagName("groupId").item(0).getTextContent();
                String artifactId = elem.getElementsByTagName("artifactId").item(0).getTextContent();

                if (groupId.equals("${project.groupId}")) {
                    // Okay, placeholders are going to be an issue ... Let's just implement the most common ones
                    groupId = artifact.groupId;
                }

                Node scopeElement = elem.getElementsByTagName("scope").item(0);
                if (scopeElement instanceof Element && !resolveTestDependencies) {
                    Element scope = (Element) scopeElement;
                    if (scope.getTextContent().equals("test")) {
                        continue;
                    }
                }

                if (artifactId == null) {
                    throw new NullPointerException();
                }

                if (blacklistedArtifacts.contains(new VersionlessMavenId(groupId, artifactId))) {
                    continue;
                }

                Node versionElement = elem.getElementsByTagName("version").item(0);
                if (versionElement == null) {
                    // TODO implicitly, the release tag should be used - let's do that eventually
                    Logger.warn("Pom for Artifact " + artifact.toString() + " does not supply an "
                            + "explicit version for artifact " + groupId + ":" + artifactId + ". It was not"
                            + " added to the dependency tree. Consider adding it manually.");
                    continue;
                }
                String version = versionElement.getTextContent();
                if (version.equals("${project.version}")) {
                    version = artifact.version;
                } else {
                    // We might need to apply placeholders recursively, but for the meantime this ought to do
                    version = placeholders.getOrDefault(version, version);
                }
                artifactId = placeholders.getOrDefault(artifactId, artifactId);
                groupId = placeholders.getOrDefault(groupId, groupId);

                if (artifactId == null || groupId == null || version == null) {
                    throw new IllegalStateException("Logical error");
                }

                getTransitiveDependencyVersions(new MavenId(groupId, artifactId, version), versions);
            }
        } catch (SAXException | ParserConfigurationException e1) {
            e1.printStackTrace();
        } catch (IOException ignored) {}
    }

    /**
     * Prevent the resolution of multiple artifacts (regardless of version) through the
     * {@link #getTransitiveDependencies(MavenId)} method. This means that all transitive child dependencies of
     * these artifacts are not resolved, unless they are included via other means (such as being declared by
     * another artifact or by being directly resolved via the {@link #getTransitiveDependencies(MavenId)} method).
     *
     * <p>Any artifacts in the set of blacklisted artifacts may still get obtained directly via {@link #getJarDepend(MavenId)}.
     *
     * @param artifacts The artifacts to blacklist
     * @return The current {@link MavenResolver} instance.
     */
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    @NotNull
    public MavenResolver preventTransitiveResolution(@NotNull Iterable<MavenId> artifacts) {
        for (MavenId artifact : artifacts) {
            this.blacklistedArtifacts.add(new VersionlessMavenId(artifact.groupId, artifact.artifactId));
        }
        return this;
    }

    /**
     * Prevent the resolution of an artifact (regardless of version) through the {@link #getTransitiveDependencies(MavenId)} method.
     * This means that all transitive child dependencies of this artifact are not resolved, unless they are included via
     * other means (such as being declared by another artifact or by being directly resolved via the
     * {@link #getTransitiveDependencies(MavenId)} method).
     *
     * <p>Any artifacts in the set of blacklisted artifacts may still get obtained directly via {@link #getJarDepend(MavenId)}.
     *
     * @param artifact The artifact to blacklist
     * @return The current {@link MavenResolver} instance.
     */
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    @NotNull
    public MavenResolver preventTransitiveResolution(@NotNull MavenId artifact) {
        this.blacklistedArtifacts.add(new VersionlessMavenId(artifact.groupId, artifact.artifactId));
        return this;
    }

    /**
     * Obtains the jar dependency that corresponds to a maven artifact and if applicable obtains
     * all transitive dependencies of that dependency in a cyclic manner. The highest version is used for
     * each artifact, provided the artifact could be resolved.
     *
     * @param artifact The root artifact to resolve
     * @return A collection of resolved artifacts
     */
    @SuppressWarnings("null")
    @NotNull
    public Collection<JavaJarDependency> getTransitiveDependencies(@NotNull MavenId artifact) {
        Map<VersionlessMavenId, MavenId> versions = new HashMap<>();
        Map<MavenId, JavaJarDependency> dependencies = new HashMap<>();
        getTransitiveDependencyVersions(artifact, versions);
        versions.values().forEach(shouldBeDependency -> {
            JavaJarDependency resolvedDependency = getJarDepend(shouldBeDependency);
            if (resolvedDependency == null) {
                return;
            }
            dependencies.put(shouldBeDependency, resolvedDependency);
        });
        return dependencies.values();
    }

    @Nullable
    public JavaJarDependency getJarDepend(@NotNull MavenId artifact) {
        // TODO some way of forcing refresh, even if jars are in the nolookup file
        Set<String> nolookup = new HashSet<>();
        Path nolookupCacheFile;
        {
            String folder = artifact.groupId.replace('.', '/') + '/' + artifact.artifactId + '/' + artifact.version + '/';
            String file = artifact.artifactId + '-' + artifact.version + ".nolookup";
            nolookupCacheFile = cacheFolder.resolve(folder).resolve(file);
            if (Files.exists(nolookupCacheFile)) {
                try {
                    nolookup.addAll(Files.readAllLines(nolookupCacheFile, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        ResolvedFile sources = null;
        ResolvedFile ijAnnotations = null;
        if (!nolookup.contains("sources")) {
            try {
                sources = resolveArtifact(artifact, "sources", "jar");
            } catch (Exception exception1) {
                nolookup.add("sources");
            }
        }
        if (!nolookup.contains("intelliJ-annotations")) {
            try {
                ijAnnotations = resolveArtifact(artifact, "annotations", "zip");
            } catch (Exception exception1) {
                // We aren't fully done here, but close enough. See:
                // https://youtrack.jetbrains.com/issue/IDEA-132487#focus=streamItem-27-3082925.0-0
                nolookup.add("intelliJ-annotations");
            }
        }
        if (!nolookup.isEmpty()) {
            try {
                Files.createDirectories(nolookupCacheFile.getParent());
                Files.write(nolookupCacheFile, nolookup);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to cache failed lookups", e);
            }
        }
        try {
            Path sourcesPath = null;
            if (sources != null) {
                sourcesPath = sources.getCachePath();
            }
            ResolvedFile resolvedJar = resolveArtifact(artifact, "", "jar");
            Path jarPath = resolvedJar.getCachePath();
            if (jarPath == null) {
                return null;
            }
            Path ijAnnotPath = null;
            if (ijAnnotations != null) {
                ijAnnotPath = ijAnnotations.getCachePath();
            }
            return new JavaJarDependency(jarPath, sourcesPath, artifact, null, ijAnnotPath);
        } catch (Exception e) {
            return null;
        }
    }
} class VersionlessMavenId {

    @NotNull
    private String groupId;
    @NotNull
    private String artifactId;

    VersionlessMavenId(@NotNull String group, @NotNull String artifact) {
        this.groupId = group;
        this.artifactId = artifact;
    }

    @Override
    public int hashCode() {
        return this.groupId.hashCode() ^ this.artifactId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VersionlessMavenId) {
            VersionlessMavenId other = (VersionlessMavenId) obj;
            return other.groupId.equals(this.groupId) && other.artifactId.equals(this.artifactId);
        }
        return false;
    }
}
