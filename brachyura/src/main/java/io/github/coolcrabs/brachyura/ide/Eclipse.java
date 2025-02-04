package io.github.coolcrabs.brachyura.ide;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jetbrains.annotations.NotNull;

import com.google.gson.stream.JsonWriter;

import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.ide.source.SourceLookupEntry;
import io.github.coolcrabs.brachyura.project.Task;
import io.github.coolcrabs.brachyura.util.AtomicFile;
import io.github.coolcrabs.brachyura.util.JvmUtil;
import io.github.coolcrabs.brachyura.util.PathUtil;
import io.github.coolcrabs.brachyura.util.Util;
import io.github.coolcrabs.brachyura.util.XmlUtil;
import io.github.coolcrabs.brachyura.util.XmlUtil.FormattedXMLStreamWriter;

public enum Eclipse implements Ide {
    INSTANCE;

    public static final String JDT_JRE_CONTAINER_KEY = "org.eclipse.jdt.launching.JRE_CONTAINER";
    public static final String JDT_JRE_CONTAINER_JVM = JDT_JRE_CONTAINER_KEY + "/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-";

    @Override
    @NotNull
    public String ideName() {
        return "jdt";
    }

    // Slbrachyura start: Don't completely overwrite old eclipse preferences
    void updateEclipseJDTCorePreferences(IdeModule module) throws IOException {
        Path preferenceFile = PathUtil.resolveAndCreateDir(module.root, ".settings").resolve("org.eclipse.jdt.core.prefs");
        List<String> otherLines = new ArrayList<>();
        if (Files.exists(preferenceFile)) {
            try (BufferedReader reader = Files.newBufferedReader(preferenceFile)) {
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    int keySeperator = line.indexOf('=');
                    if (keySeperator == -1) {
                        otherLines.add(line);
                    }
                    String key = line.substring(0, keySeperator);
                    if (key.equals("eclipse.preferences.version")) {
                        String value = line.substring(keySeperator + 1);
                        if (!value.equals("1")) {
                            throw new IOException("Unrecognised eclipse preferences version: " + value + ". Only \"1\" is supported!");
                        }
                        continue; // Key is overwritten
                    }
                    if (key.equals("org.eclipse.jdt.core.compiler.codegen.targetPlatform")
                            || key.equals("rg.eclipse.jdt.core.compiler.compliance")
                            || key.equals("org.eclipse.jdt.core.compiler.source")) {
                        continue; // We override these keys
                    }
                    otherLines.add(line);
                }
            } catch (Exception e) {
                otherLines.clear();
                e.printStackTrace();
            }
        }
        try (BufferedWriter prefs = Files.newBufferedWriter(preferenceFile)) {
            for (String line : otherLines) {
                prefs.write(line);
                prefs.write('\n');
            }
            prefs.write("eclipse.preferences.version=1\n");
            String j = JvmUtil.javaVersionString(module.javaVersion);
            prefs.write("org.eclipse.jdt.core.compiler.codegen.targetPlatform="); prefs.write(j); prefs.write('\n');
            prefs.write("org.eclipse.jdt.core.compiler.compliance="); prefs.write(j); prefs.write('\n');
            prefs.write("org.eclipse.jdt.core.compiler.source="); prefs.write(j); prefs.write('\n');
        }
    }
    // Slbrachyura end

    @Override
    public void updateProject(Path projectRoot, IdeModule[] ideModules) {
        Ide.validate(ideModules);
        try {
            Files.walkFileTree(projectRoot, Collections.emptySet(), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".launch")) {
                        Files.delete(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            for (IdeModule module : ideModules) {
                if (Files.exists(module.root.resolve(".brachyura").resolve("eclipseout"))) PathUtil.deleteDirectoryChildren(module.root.resolve(".brachyura").resolve("eclipseout"));
                if (Files.exists(module.root.resolve(".brachyura").resolve("eclipsetestout"))) PathUtil.deleteDirectoryChildren(module.root.resolve(".brachyura").resolve("eclipsetestout"));
                writeModule(module);
                writeClasspath(module);
                writeLaunchConfigs(projectRoot, module);
                vscodeLaunchJson(projectRoot, ideModules);
            }
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    void writeModule(IdeModule module) throws IOException, XMLStreamException {
        try (FormattedXMLStreamWriter w = XmlUtil.newStreamWriter(Files.newBufferedWriter(module.root.resolve(".project")))) {
            w.writeStartDocument("UTF-8", "1.0");
            w.newline();
            w.writeStartElement("projectDescription");
            w.indent();
            w.newline();
                w.writeStartElement("name");
                w.writeCharacters(module.name);
                w.writeEndElement();
                w.newline();
                w.writeStartElement("comment");
                w.writeEndElement();
                w.newline();
                w.writeStartElement("projects");
                w.writeEndElement();
                w.newline();
                w.writeStartElement("buildSpec");
                w.indent();
                w.newline();
                    w.writeStartElement("buildCommand");
                    w.indent();
                    w.newline();
                        w.writeStartElement("name");
                        w.writeCharacters("org.eclipse.jdt.core.javabuilder");
                        w.writeEndElement();
                        w.newline();
                        w.writeStartElement("arguments");
                        w.writeEndElement();
                        w.unindent();
                        w.newline();
                    w.writeEndElement();
                    w.unindent();
                    w.newline();
                w.writeEndElement();
                w.newline();
                w.writeStartElement("natures");
                w.indent();
                w.newline();
                    w.writeStartElement("nature");
                    w.writeCharacters("org.eclipse.jdt.core.javanature");
                    w.writeEndElement();
                    w.unindent();
                    w.newline();
                w.writeEndElement();
                w.unindent();
                w.newline();
            w.writeEndElement();
            w.newline();
            w.writeEndDocument();
        }
        // Slbrachyura start: Don't completely overwrite old eclipse preferences
        updateEclipseJDTCorePreferences(module);
        // Slbrachyura end
        // Slbrachyura start: Set resource encoding for eclipse
        try (BufferedWriter prefs = Files.newBufferedWriter(module.root.resolve(".settings").resolve("org.eclipse.core.resources.prefs"))) {
            prefs.write("eclipse.preferences.version=1\n");
            prefs.write("encoding/<project>=UTF-8");
        }
        // Slbrachyura end
    }

    void writeClasspath(IdeModule project) throws IOException, XMLStreamException {
        Path dotClasspath = project.root.resolve(".classpath");
        try (FormattedXMLStreamWriter w = XmlUtil.newStreamWriter(Files.newBufferedWriter(dotClasspath))) {
            w.writeStartDocument("UTF-8", "1.0");
            w.newline();
            w.writeStartElement("classpath");
            w.indent();
            w.newline();
                w.writeEmptyElement("classpathentry");
                w.writeAttribute("kind", "con");
                w.writeAttribute("path", JDT_JRE_CONTAINER_JVM + JvmUtil.javaVersionString(project.javaVersion));
                sourceClasspathEntryAttributes(w, project.root, project.sourcePaths, false);
                sourceClasspathEntryAttributes(w, project.root, project.resourcePaths, false);
                sourceClasspathEntryAttributes(w, project.root, project.testSourcePaths, true);
                sourceClasspathEntryAttributes(w, project.root, project.testResourcePaths, true);
                moduleDepClasspathEntries(w, project);
                for (JavaJarDependency dep : project.dependencies.get()) {
                    w.newline();
                    w.writeStartElement("classpathentry");
                    w.writeAttribute("kind", "lib");
                    w.writeAttribute("path", dep.jar.toString());
                    Path sourcesJar = dep.sourcesJar;
                    if (sourcesJar != null) {
                        w.writeAttribute("sourcepath", sourcesJar.toString());
                    }
                    w.writeStartElement("attributes");
                    Path eclipseAnnotations = dep.eclipseExternalAnnotations;
                    if (eclipseAnnotations != null) {
                        w.writeEmptyElement("attribute");
                        w.writeAttribute("name", "annotationpath");
                        w.writeAttribute("value", eclipseAnnotations.toString());
                    }
                    String javadocURL = dep.getJavadocURL();
                    if (javadocURL != null) {
                        w.writeEmptyElement("attribute");
                        w.writeAttribute("name", "javadoc_location");
                        w.writeAttribute("value", javadocURL);
                    }
                    w.writeEndElement();
                    w.writeEndElement();
                }
                w.newline();
                w.writeEmptyElement("classpathentry");
                w.writeAttribute("kind", "output");
                w.writeAttribute("path", ".brachyura/eclipseout");
            w.unindent();
            w.newline();
            w.writeEndElement();
            w.newline();
            w.writeEndDocument();
        }
    }

    void writeLaunchConfigs(Path projectDir, IdeModule ideProject) throws IOException, XMLStreamException {
        try {
            for (Task task : ideProject.tasks) {

                // TODO use a proper system for that
                if (task.name.equals("idea")
                        || task.name.equals("netbeans")
                        || task.name.equals("publish")
                        || task.name.equals("publishToMavenLocal")) {
                    continue;
                }

                String rcname = ideProject.name + " - " + task.getName();
                try (FormattedXMLStreamWriter w = XmlUtil.newStreamWriter(Files.newBufferedWriter(projectDir.resolve(rcname + ".launch")))) {
                    w.writeStartDocument("UTF-8", "1.0");
                    w.writeStartElement("launchConfiguration");
                    w.writeAttribute("type", "org.eclipse.jdt.launching.localJavaApplication");
                    w.indent();
                    w.newline();
                        w.writeStartElement("listAttribute");
                        w.writeAttribute("key", "org.eclipse.debug.core.MAPPED_RESOURCE_PATHS");
                        w.indent();
                        w.newline();
                            w.writeEmptyElement("listEntry");
                            w.writeAttribute("value", "/" + rcname + "/");
                            w.unindent();
                            w.newline();
                        w.writeEndElement();
                        w.newline();
                        w.writeStartElement("listAttribute");
                        w.writeAttribute("key", "org.eclipse.debug.core.MAPPED_RESOURCE_TYPES");
                        w.indent();
                        w.newline();
                            w.writeEmptyElement("listEntry");
                            w.writeAttribute("value", "4");  // ???
                            w.unindent();
                            w.newline();
                        w.writeEndElement();
                        w.newline();
                        stringAttribute(w, "org.eclipse.debug.core.source_locator_id", "org.eclipse.jdt.launching.sourceLocator.JavaSourceLookupDirector");
                        w.newline();
                        stringAttribute(w, "org.eclipse.debug.core.source_locator_memento", sourceLookupValue(task.getIdeDebugConfigSourceLookupEntries()));
                        w.newline();
                        booleanAttribute(w, "org.eclipse.jdt.launching.ATTR_ATTR_USE_ARGFILE", false);
                        w.newline();
                        booleanAttribute(w, "org.eclipse.jdt.launching.ATTR_SHOW_CODEDETAILS_IN_EXCEPTION_MESSAGES", false);
                        w.newline();
                        booleanAttribute(w, "org.eclipse.jdt.launching.ATTR_USE_CLASSPATH_ONLY_JAR", false);
                        w.newline();
                        booleanAttribute(w, "org.eclipse.jdt.launching.ATTR_USE_START_ON_FIRST_THREAD", true);
                        w.newline();
                        w.writeStartElement("listAttribute");
                        w.writeAttribute("key", "org.eclipse.jdt.launching.CLASSPATH");
                        w.indent();
                            List<Path> cp = new ArrayList<>(task.getIdeRunConfigClasspath());
                            cp.addAll(task.getIdeRunConfigResourcepath());
                            for (Path p : cp) {
                                w.newline();
                                w.writeEmptyElement("listEntry");
                                w.writeAttribute("value", libraryValue(p));
                            }
                            List<IdeModule> modules = new ArrayList<>();
                            modules.add(ideProject);
                            // TODO Replace - if needed
                            // modules.addAll(rc.additionalModulesClasspath);
                            for (IdeModule mod : modules) {
                                w.newline();
                                w.writeEmptyElement("listEntry");
                                w.writeAttribute("value", projectValue(mod.name));
                            }
                            w.unindent();
                            w.newline();
                        w.writeEndElement();
                        w.newline();
                        booleanAttribute(w, "org.eclipse.jdt.launching.DEFAULT_CLASSPATH", false);
                        w.newline();
                        stringAttribute(w, "org.eclipse.jdt.launching.MAIN_TYPE", task.getIdeRunConfigMainClass());
                        w.newline();
                        String javaVersionString = JDT_JRE_CONTAINER_JVM + JvmUtil.javaVersionString(task.getIdeRunConfigJavaVersion()) + "/";
                        stringAttribute(w, JDT_JRE_CONTAINER_KEY, javaVersionString);
                        w.newline();
                        StringBuilder args = new StringBuilder();
                        for (String arg : task.getIdeRunConfigArgs()) {
                            args.append(quote(arg));
                            args.append(' ');
                        }
                        stringAttribute(w, "org.eclipse.jdt.launching.PROGRAM_ARGUMENTS", args.toString());
                        w.newline();
                        stringAttribute(w, "org.eclipse.jdt.launching.PROJECT_ATTR", ideProject.name);
                        w.newline();
                        StringBuilder vmargs = new StringBuilder();
                        for (String vmarg : task.getIdeRunConfigVMArgs()) {
                            vmargs.append(quote(vmarg));
                            vmargs.append(' ');
                        }
                        stringAttribute(w, "org.eclipse.jdt.launching.VM_ARGUMENTS", vmargs.toString());
                        w.newline();
                        stringAttribute(w, "org.eclipse.jdt.launching.WORKING_DIRECTORY", task.getIdeRunConfigWorkingDir().toString());
                        w.unindent();
                        w.newline();
                    w.writeEndElement();
                    w.newline();
                    w.writeEndDocument();
                }
            }
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }

    String libraryValue(Path lib) throws XMLStreamException {
        StringWriter writer = new StringWriter();
        try (FormattedXMLStreamWriter w = XmlUtil.newStreamWriter(writer)) {
            w.writeStartDocument("UTF-8", "1.0");
            w.newline();
            w.writeEmptyElement("runtimeClasspathEntry");
            w.writeAttribute("externalArchive", lib.toString());
            w.writeAttribute("path", "5"); // ???
            w.writeAttribute("type", "2"); // ???
            w.newline();
            w.writeEndDocument();
        }
        return writer.toString();
    }

    String projectValue(String project) throws XMLStreamException {
        StringWriter writer = new StringWriter();
        try (FormattedXMLStreamWriter w = XmlUtil.newStreamWriter(writer)) {
            w.writeStartDocument("UTF-8", "1.0");
            w.newline();
            w.writeEmptyElement("runtimeClasspathEntry");
            w.writeAttribute("projectName", project);
            w.writeAttribute("path", "5"); // ???
            w.writeAttribute("type", "1"); // ???
            w.newline();
            w.writeEndDocument();
        }
        return writer.toString();
    }

    String sourceLookupValue(@NotNull List<SourceLookupEntry> sources) throws XMLStreamException {
        StringWriter writer = new StringWriter();
        try (FormattedXMLStreamWriter w = XmlUtil.newStreamWriter(writer)) {
            w.writeStartDocument("UTF-8", "1.0");
            w.writeStartElement("sourceLookupDirector");
                w.writeStartElement("sourceContainers");
                w.writeAttribute("duplicates", "false");
                    for (SourceLookupEntry sourceEntry : sources) {
                        w.writeEmptyElement("container");
                        w.writeAttribute("memento", sourceEntry.getEclipseJDTValue());
                        w.writeAttribute("typeId", sourceEntry.getEclipseJDTType());
                    }
                    w.writeEmptyElement("container");
                    w.writeAttribute("memento", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><default/>");
                    w.writeAttribute("typeId", "org.eclipse.debug.core.containerType.default");
                    w.unindent();
                w.writeEndElement();
            w.writeEndElement();
            w.writeEndDocument();
        }
        return writer.toString();
    }

    void booleanAttribute(FormattedXMLStreamWriter w, String key, boolean value) throws XMLStreamException {
        w.writeEmptyElement("booleanAttribute");
        w.writeAttribute("key", key);
        w.writeAttribute("value", Boolean.toString(value));
    }

    void stringAttribute(FormattedXMLStreamWriter w, String key, String value) throws XMLStreamException {
        w.writeEmptyElement("stringAttribute");
        w.writeAttribute("key", key);
        w.writeAttribute("value", value);
    }

    void sourceClasspathEntryAttributes(FormattedXMLStreamWriter w, Path projectDir, List<@NotNull Path> paths, boolean isTest) throws XMLStreamException {
        for (Path src : paths) {
            w.newline();
            if (isTest) {
                w.writeStartElement("classpathentry");
            } else {
                w.writeEmptyElement("classpathentry");
            }
            w.writeAttribute("kind", "src");
            w.writeAttribute("path", projectDir.relativize(src).toString());
            if (isTest) {
                w.writeAttribute("output", ".brachyura/eclipsetestout");
                w.indent();
                w.newline();
                w.writeStartElement("attributes");
                w.indent();
                w.newline();
                w.writeEmptyElement("attribute");
                w.writeAttribute("name", "test");
                w.writeAttribute("value", "true");
                w.unindent();
                w.newline();
                w.writeEndElement();
                w.unindent();
                w.newline();
                w.writeEndElement();
            }
        }
    }

    void moduleDepClasspathEntries(FormattedXMLStreamWriter w, IdeModule module) throws XMLStreamException {
        for (IdeModule mod : module.dependencyModules) {
            w.newline();
            w.writeEmptyElement("classpathentry");
            w.writeAttribute("combineaccessrules", "false");
            w.writeAttribute("kind", "src");
            w.writeAttribute("path", "/" + mod.name);
        }
    }

    static String quote(String arg) {
        return '"' + arg.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    void vscodeLaunchJson(Path rootDir, IdeModule... basemodules) throws IOException {
        try (AtomicFile atomicFile = new AtomicFile(rootDir.resolve(".vscode").resolve("launch.json"))) {
            try (JsonWriter jsonWriter = new JsonWriter(PathUtil.newBufferedWriter(atomicFile.tempPath))) {
                jsonWriter.setIndent("  ");
                jsonWriter.beginObject();
                jsonWriter.name("version").value("0.2.0");
                jsonWriter.name("configurations");
                jsonWriter.beginArray();
                for (IdeModule mod : basemodules) {
                    for (Task task : mod.tasks) {
                        jsonWriter.beginObject();
                        jsonWriter.name("type").value("java");
                        jsonWriter.name("name").value(mod.name + " - " + task.getName());
                        jsonWriter.name("request").value("launch");
                        jsonWriter.name("cwd").value(task.getIdeRunConfigWorkingDir().toString());
                        jsonWriter.name("console").value("internalConsole");
                        jsonWriter.name("mainClass").value(task.getIdeRunConfigMainClass());
                        jsonWriter.name("vmArgs");
                        jsonWriter.beginArray();
                        for (String vmArg : task.getIdeRunConfigVMArgs()) {
                            jsonWriter.value(vmArg);
                        }
                        jsonWriter.endArray();
                        jsonWriter.name("args");
                        jsonWriter.beginArray();
                        for (String arg : task.getIdeRunConfigArgs()) {
                            jsonWriter.value(arg);
                        }
                        jsonWriter.endArray();
                        jsonWriter.name("stopOnEntry").value(false);
                        jsonWriter.name("projectName").value(mod.name);
                        jsonWriter.name("classPaths");
                        jsonWriter.beginArray();
                        jsonWriter.value(mod.root.resolve(".brachyura").resolve("eclipseout").toString());
                        for (IdeModule m : mod.dependencyModules) {
                            jsonWriter.value(m.root.resolve(".brachyura").resolve("eclipseout").toString());
                        }
                        for (Path path : task.getIdeRunConfigResourcepath()) {
                            jsonWriter.value(path.toString());
                        }
                        for (Path path : task.getIdeRunConfigClasspath()) {
                            jsonWriter.value(path.toString());
                        }
                        jsonWriter.endArray();
                        jsonWriter.endObject();
                    }
                }
                jsonWriter.endArray();
                jsonWriter.endObject();
                jsonWriter.flush();
            }
            atomicFile.commit();
        }
    }
}
