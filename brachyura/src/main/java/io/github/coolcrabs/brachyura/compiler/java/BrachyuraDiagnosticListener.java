package io.github.coolcrabs.brachyura.compiler.java;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;

import org.tinylog.Logger;

enum BrachyuraDiagnosticListener implements DiagnosticListener<Object> {
    INSTANCE;

    @Override
    public void report(Diagnostic<? extends Object> diagnostic) {
        switch (diagnostic.getKind()) {
            case ERROR:
                Logger.error(diagnostic.toString());
                break;
            case WARNING:
            case MANDATORY_WARNING:
                Logger.warn(diagnostic.toString());
                break;
            case NOTE:
            case OTHER:
            default:
                Logger.info(diagnostic.toString());
                break;
        }
    }
}
