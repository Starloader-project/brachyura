package io.github.coolcrabs.brachyura.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.plugins.Plugin;
import io.github.coolcrabs.brachyura.plugins.Plugins;

public class BrachyuraEntry {
    private BrachyuraEntry() { }

    private static final boolean isBlank(@NotNull String string) {
        int length = string.length();
        for (int i = 0; i < length; i++) {
            if (!Character.isWhitespace(string.codePointAt(i))) {
                return false;
            }
        }
        return true;
    }

    // Slbrachyura: Interactive project creation
    private static void interactiveSetup(String[] args, Path projectDir, List<Path> classpath) {
        Path relJavaSourceFolder = Paths.get("src", "main", "java");
        Path javaSourceFolder = projectDir.resolve(relJavaSourceFolder);
        if (Files.exists(javaSourceFolder)) {
            System.err.println("Cannot create template: The source folder (" + javaSourceFolder.toAbsolutePath().toString() + ") already exists. Consider deleting it if you are sure of your actions.");
            return;
        }
        Path buildscriptSourceFolder = projectDir.resolve("buildscript").resolve(relJavaSourceFolder);
        if (Files.exists(buildscriptSourceFolder)) {
            System.err.println("Cannot create template: Buildscript folder already exists. Consider deleting it if you are sure of your actions.");
            return;
        }
        Path mainResourceFolder = projectDir.resolve("src").resolve("main").resolve("resources");
        if (Files.exists(mainResourceFolder)) {
            System.err.println("Cannot create template: The resources folder (" + mainResourceFolder.toAbsolutePath().toString() + ") already exists. Consider deleting it if you are sure of your actions.");
            return;
        }

        try {
            Files.createDirectories(javaSourceFolder);
            Files.createDirectories(buildscriptSourceFolder);
            Files.createDirectories(mainResourceFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Path buildscriptFile = buildscriptSourceFolder.resolve("Buildscript.java");
        Path exampleApplicationFile = javaSourceFolder.resolve("ExampleApplication.java");

        BuildscriptCreator bsCreator = new BuildscriptCreator();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.println("Choose java version [8]: ");
                String javaVer = br.readLine();
                if (javaVer == null || isBlank(javaVer)) {
                    javaVer = "8";
                }
                try {
                    bsCreator.withJavaVersion(Integer.parseInt(javaVer.trim()));
                    break;
                } catch (NumberFormatException nfe) {
                    System.err.println("Unparseable java version: \"" + javaVer + "\". Note: it must be an integer");
                }
            }
            System.out.println("Choose project name [ExampleBuildscript]: ");
            String buildscriptProjectName = br.readLine();
            if (buildscriptProjectName == null || isBlank(buildscriptProjectName)) {
                buildscriptProjectName = "ExampleBuildscript";
            }
            bsCreator.withProjectName(buildscriptProjectName);

            System.out.println("Choose maven group id [org.example]: ");
            String groupId = br.readLine();
            if (groupId == null || isBlank(groupId)) {
                groupId = "org.example";
            }
            System.out.println("Choose maven artifact id [example]: ");
            String artifactId = br.readLine();
            if (artifactId == null || isBlank(artifactId)) {
                artifactId = "example";
            }
            System.out.println("Choose maven artifact version [0.0.1-SNAPSHOT]: ");
            String version = br.readLine();
            if (version == null || isBlank(version)) {
                version = "0.0.1-SNAPSHOT";
            }
            bsCreator.withId(new MavenId(groupId, artifactId, version));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            Files.write(buildscriptFile, bsCreator.getBuildscriptSource().getBytes(StandardCharsets.UTF_8));
            Files.write(exampleApplicationFile,
                     ("public class ExampleApplication {\n"
                    + "\n"
                    + "    public static void main(String[] args) {\n"
                    + "        System.out.println(\"Hello, World!\");\n"
                    + "    }\n"
                    + "}\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Called via reflection by bootstrap
    public static void main(String[] args, Path projectDir, List<Path> classpath) {
        if (args.length != 0 && args[0].equalsIgnoreCase("createTemplate")) {
            interactiveSetup(args, projectDir, classpath);
            return;
        }
        int exitcode = 0;
        List<Plugin> plugins = Plugins.getPlugins();
        for (Plugin plugin : plugins) {
            plugin.onEntry();
        }
        try {
            EntryGlobals.projectDir = projectDir;
            EntryGlobals.buildscriptClasspath = classpath;
            BuildscriptProject buildscriptProject = new BuildscriptProject();
            // Slbrachyura start: Improved task system
            if (args.length >= 1 && "buildscript".equalsIgnoreCase(args[0])) {
                boolean searchingTasks = true;
                if (args.length >= 2) {
                    for (Task task : buildscriptProject.getTasks()) {
                        if (task.name.equals(args[1])) {
                            if (!searchingTasks) {
                                throw new IllegalStateException("There are multiple tasks with the name \"" + task.name + "\".");
                            }
                            task.doTask(Arrays.copyOfRange(args, 2, args.length));
                            searchingTasks = false;
                        }
                    }
                    if (searchingTasks) {
                        Logger.error("Unable to find task with name: " + args[1]);
                    }
                }
                if (searchingTasks) {
                    StringBuilder availableTasks = new StringBuilder();
                    for (Task task : buildscriptProject.getTasks()) {
                        availableTasks.append(' ');
                        availableTasks.append(task.name);
                    }
                    Logger.info("Available buildscript tasks: " + availableTasks.toString());
                }
            } else {
                Optional<Project> o = buildscriptProject.project.get();
                if (o.isPresent()) {
                    Project project = o.get();
                    project.setIdeProject(buildscriptProject);
                    boolean searchingTasks = true;
                    if (args.length >= 1) {
                        for (Task task : project.getTasks()) {
                            if (task.name.equals(args[0])) {
                                if (!searchingTasks) {
                                    throw new IllegalStateException("There are multiple tasks with the name \"" + task.name + "\".");
                                }
                                task.doTask(Arrays.copyOfRange(args, 1, args.length));
                                searchingTasks = false;
                            }
                        }
                        if (searchingTasks) {
                            Logger.error("Unable to find task with name: " + args[0]);
                        }
                    }
                    if (searchingTasks) {
                        StringBuilder availableTasks = new StringBuilder();
                        for (Task task : project.getTasks()) {
                            availableTasks.append(' ');
                            availableTasks.append(task.name);
                        }
                        Logger.info("Available buildscript tasks: " + availableTasks.toString());
                    }
                    // Slbrachyura end
                } else {
                    Logger.warn("Invalid build script :(");
                    Logger.info("Tip: If you invoke the bootstrap with \"createTemplate\" a template project will be created.");
                }
            }
        } catch (Exception e) {
            Logger.error("Task Failed");
            Logger.error(e);
            exitcode = 1;
        }
        for (Plugin plugin : plugins) {
            plugin.onExit();
        }
        System.exit(exitcode);
    }
}
