package io.github.coolcrabs.brachyura.project;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.maven.HttpMavenRepository;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.maven.MavenResolver;
import io.github.coolcrabs.brachyura.project.java.SimpleJavaProject;

// Slbrachyura: Interactive project creation
public class BuildscriptCreator {

    private String buildscriptName = "Buildscript";
    private String buildscriptProjectName = "ExampleBuildscript";
    private Type buildscriptSuperclass = SimpleJavaProject.class;
    private List<MavenId> dependencies = new ArrayList<>();
    private List<String> foreignRepositories = new ArrayList<>();
    private int javaVersion = 8;
    private MavenId projectId = new MavenId("com.example", "example", "0.0.1-SNAPSHOT");

    @NotNull
    @Contract(pure = true, value = "-> new")
    public String getBuildscriptSource() {
        final String className;
        final String packageName;
        {
            int lastDot = buildscriptName.lastIndexOf('.');
            if (lastDot == -1) {
                packageName = "";
            } else {
                packageName = buildscriptName.substring(0, lastDot);
            }
            className = buildscriptName.substring(lastDot + 1);
        }

        List<MethodSpec> methods = new ArrayList<>();
        List<FieldSpec> fields = new ArrayList<>();

        methods.add(MethodSpec.methodBuilder("getId")
                .returns(MavenId.class)
                .addAnnotation(Override.class)
                .addAnnotation(NotNull.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addStatement("return new $T($S, $S, $S)", MavenId.class, projectId.groupId, projectId.artifactId, projectId.version)
                .build());

        methods.add(MethodSpec.methodBuilder("getJavaVersion")
                .returns(int.class)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addStatement("return $L", javaVersion)
                .build());

        methods.add(MethodSpec.methodBuilder("getBuildscriptName")
                .returns(String.class)
                .addAnnotation(Override.class)
                .addAnnotation(NotNull.class)
                .addAnnotation(AnnotationSpec.builder(Contract.class).addMember("pure", CodeBlock.of("true")).build())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addStatement(CodeBlock.of("return $S", buildscriptProjectName))
                .build());

        if (!dependencies.isEmpty() || !foreignRepositories.isEmpty()) {
            TypeName jarDepArrayList = ParameterizedTypeName.get(ArrayList.class, JavaJarDependency.class);
            FieldSpec field = FieldSpec.builder(jarDepArrayList, "dependencies", Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("new $T()", jarDepArrayList)
                    .build();
            fields.add(field);
            MethodSpec.Builder builder = MethodSpec.methodBuilder("createDependencies")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addAnnotation(NotNull.class)
                    .returns(jarDepArrayList)
                    .addStatement("$T list = new $T(super.createDependencies())", jarDepArrayList, jarDepArrayList)
                    .beginControlFlow("if ($N.isEmpty())", field)
                    .addStatement("$T resolver = new $T($T.MAVEN_LOCAL)", MavenResolver.class, MavenResolver.class, MavenResolver.class);

            for (String repo : foreignRepositories) {
                builder.addStatement("resolver.addRepository(new $T($S))", HttpMavenRepository.class, repo);
            }

            for (MavenId dep : dependencies) {
                builder.addStatement("$N.add(resolver.getJarDepend(new $T($S, $S, $S)))", field, MavenId.class, dep.groupId, dep.artifactId, dep.version);
            }

            methods.add(builder.endControlFlow()
                    .addStatement("list.addAll($N)", field)
                    .addStatement("return list")
                    .build());
        }

        TypeSpec type = TypeSpec.classBuilder(className)
                .addSuperinterface(DescriptiveBuildscriptName.class)
                .superclass(buildscriptSuperclass)
                .addFields(fields)
                .addMethods(methods)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build();

        JavaFile buildscriptFile = JavaFile.builder(packageName, type)
                .build();

        StringBuilder builder = new StringBuilder();
        try {
            buildscriptFile.writeTo(builder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        builder.append('\n');
        return builder.toString();
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public BuildscriptCreator withClassName(@NotNull String name) {
        this.buildscriptName = name;
        return this;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public BuildscriptCreator withDependency(@NotNull MavenId dep) {
        this.dependencies.add(dep);
        return this;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public BuildscriptCreator withHttpRepo(@NotNull String repoURL) {
        this.foreignRepositories.add(repoURL);
        return this;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public BuildscriptCreator withId(@NotNull MavenId id) {
        this.projectId = id;
        return this;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public BuildscriptCreator withJavaVersion(int version) {
        this.javaVersion = version;
        return this;
    }


    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public BuildscriptCreator withProjectName(@NotNull String name) {
        this.buildscriptProjectName = name;
        return this;
    }

    @NotNull
    @Contract(mutates = "this", pure = false, value = "_ -> this")
    public BuildscriptCreator withSuperclass(@NotNull Type superClass) {
        this.buildscriptSuperclass = superClass;
        return this;
    }
}
