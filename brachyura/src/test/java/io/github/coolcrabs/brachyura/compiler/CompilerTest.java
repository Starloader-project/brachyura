package io.github.coolcrabs.brachyura.compiler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.github.coolcrabs.brachyura.compiler.java.JavaCompilation;
import io.github.coolcrabs.brachyura.compiler.java.JavaCompilationResult;
import io.github.coolcrabs.brachyura.exception.CompilationFailure;
import io.github.coolcrabs.brachyura.processing.sources.ProcessingSponge;
import io.github.coolcrabs.brachyura.util.PathUtil;

class CompilerTest {
    @Test
    void e() {
        Path src = PathUtil.CWD.getParent().resolve("testprogram").resolve("src").resolve("main").resolve("java");
        JavaCompilationResult compilation;
        try {
            compilation = new JavaCompilation()
                .addSourceDir(src)
                .compile();
        } catch (CompilationFailure e1) {
            assertDoesNotThrow(() -> {
                throw e1;
            });
            throw new InternalError("This line of code should not execute. If it did, you should be running.");
        }
        ProcessingSponge a = new ProcessingSponge();
        compilation.getInputs(a);
        int[] count = new int[1];
        a.getInputs((in, id) -> {
            count[0]++;
            Path sourceFile = compilation.getSourceFile(id);
            System.out.println(sourceFile);
            assertTrue(sourceFile.startsWith(src));
        });
    }

    @Test
    void mem() {
        Path dir = PathUtil.CWD.getParent().resolve("test").resolve("compiler").resolve("java").resolve("memclass");
        JavaCompilationResult compilationA;
        try {
            compilationA = new JavaCompilation()
                .addSourceFile(dir.resolve("ClassA.java"))
                .addSourceFile(dir.resolve("coolpackage").resolve("CoolPackageClassA.java"))
                .compile();
        } catch (CompilationFailure e1) {
            assertDoesNotThrow(() -> {
                throw e1;
            });
            throw new InternalError("This line of code should not execute. If it did, you should be running.");
        }
        ProcessingSponge a = new ProcessingSponge();
        compilationA.getInputs(a);
        JavaCompilationResult compilationB;
        try {
            compilationB = new JavaCompilation()
                .addSourceFile(dir.resolve("ClassB.java"))
                .addClasspath(a)
                .compile();
        } catch (CompilationFailure e2) {
            assertDoesNotThrow(() -> {
                throw e2;
            });
            throw new InternalError("This line of code should not execute. If it did, you should be running.");
        }
        ProcessingSponge b = new ProcessingSponge();
        compilationB.getInputs(b);
        int[] count = new int[1];
        b.getInputs((in, id) -> {
            count[0]++;
            Path sourceFile = compilationB.getSourceFile(id);
            System.out.println(sourceFile);
        });
    }
}
