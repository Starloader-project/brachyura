package io.github.coolcrabs.brachyura.compiler.java;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;

import org.tinylog.Logger;

enum BrachyuraDiagnosticListener implements DiagnosticListener<Object> {
    INSTANCE;

    @Override
    public void report(Diagnostic<? extends Object> diagnostic) {
        Diagnostic.Kind diagnosticKind = diagnostic.getKind();
        if (diagnosticKind == Diagnostic.Kind.ERROR) {
            Logger.error(diagnostic.toString());
        } else if (diagnosticKind == Diagnostic.Kind.MANDATORY_WARNING || diagnosticKind == Diagnostic.Kind.WARNING) {
            Logger.warn(diagnostic.toString());
        } else {
            Logger.info(diagnostic.toString());
        }
    }
}
