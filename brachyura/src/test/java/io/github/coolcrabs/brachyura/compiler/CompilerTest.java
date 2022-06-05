package io.github.coolcrabs.brachyura.compiler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.github.coolcrabs.brachyura.compiler.java.JavaCompilation;
import io.github.coolcrabs.brachyura.compiler.java.JavaCompilationResult;
import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.maven.HttpMavenRepository;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.maven.MavenResolver;
import io.github.coolcrabs.brachyura.processing.sources.ProcessingSponge;
import io.github.coolcrabs.brachyura.util.PathUtil;

class CompilerTest {
    @Test
    void e() {
        Path src = PathUtil.CWD.resolveSibling("testprogram").resolve("src").resolve("main").resolve("java");
        JavaCompilationResult compilation = new JavaCompilation()
                .addSourceDir(src)
                .compile();
        ProcessingSponge a = new ProcessingSponge();
        compilation.getInputs(a);
        int[] count = new int[1];
        a.getInputs((in, id) -> {
            count[0]++;
            Path sourceFile = compilation.getSourceFile(id);
            assertTrue(sourceFile.startsWith(src));
        });
    }

    @Test
    void mem() {
        Path dir = PathUtil.CWD.resolveSibling("test").resolve("compiler").resolve("java").resolve("memclass");
        JavaCompilationResult compilationA = new JavaCompilation()
                .addSourceFile(dir.resolve("ClassA.java"))
                .addSourceFile(dir.resolve("coolpackage").resolve("CoolPackageClassA.java"))
                .compile();
        ProcessingSponge a = new ProcessingSponge();
        compilationA.getInputs(a);
        JavaCompilationResult compilationB = new JavaCompilation()
                .addSourceFile(dir.resolve("ClassB.java"))
                .addClasspath(a)
                .compile();
        ProcessingSponge b = new ProcessingSponge();
        compilationB.getInputs(b);
        int[] count = new int[1];
        b.getInputs((in, id) -> {
            count[0]++;
            compilationB.getSourceFile(id);
        });
    }

    @Test
    void immutables() {
        Path dir = PathUtil.CWD.resolveSibling("test").resolve("compiler").resolve("java").resolve("immutables");
        JavaCompilationResult compilationA;
        try {
            MavenResolver resolver = new MavenResolver(MavenResolver.MAVEN_LOCAL);
            resolver.addRepository(new HttpMavenRepository(MavenResolver.MAVEN_CENTRAL));
            JavaJarDependency jarDep = resolver.getJarDepend(new MavenId("org.immutables:value:2.8.2"));
            if (jarDep == null) {
                throw new IOException("Unable to locate artifact: org.immutables:values:2.8.2");
            }
            compilationA = new JavaCompilation()
                .addSourceDir(dir)
                .addClasspath(jarDep.jar)
                .compile();
            ProcessingSponge a = new ProcessingSponge();
            compilationA.getInputs(a);
            int[] count = new int[1];
            a.getInputs((in, id) -> {
                count[0]++;
                compilationA.getSourceFile(id);
            });
        } catch (Exception e) {
            assertDoesNotThrow(() -> {
                throw e;
            });
        }
    }
}
