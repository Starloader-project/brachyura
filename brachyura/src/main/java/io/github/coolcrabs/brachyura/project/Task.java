package io.github.coolcrabs.brachyura.project;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import io.github.coolcrabs.brachyura.exception.TaskFailedException;
import io.github.coolcrabs.brachyura.util.ThrowingRunnable;

public abstract class Task {
    public final String name;

    Task(String name) {
        this.name = name;
    }

    public static Task of(String name, BooleanSupplier run) {
        return new FailableNoArgTask(name, run);
    }

    public static Task of(String name, Runnable run) {
        return new NoArgTask(name, run);
    }

    public static Task of(String name, ThrowingRunnable run) {
        return new NoArgTask(name, run);
    }

    public static Task of(String name, Consumer<String[]> run) {
        return new TaskWithArgs(name, run);
    }

    public abstract void doTask(String[] args);

    static class FailableNoArgTask extends Task {
        final BooleanSupplier runnable;

        FailableNoArgTask(String name, BooleanSupplier runnable) {
            super(name);
            this.runnable = runnable;
        }

        @Override
        public void doTask(String[] args) {
            if (!runnable.getAsBoolean()) throw new TaskFailedException("Task returned false");
        }
    }

    static class NoArgTask extends Task {
        final ThrowingRunnable runnable;

        NoArgTask(String name, Runnable runnable) {
            super(name);
            this.runnable = () -> runnable.run();
        }

        NoArgTask(String name, ThrowingRunnable runnable) {
            super(name);
            this.runnable = runnable;
        }

        @Override
        public void doTask(String[] args) {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new TaskFailedException("Task failed to execute!", e);
            }
        }
    }

    static class TaskWithArgs extends Task {
        final Consumer<String[]> task;

        TaskWithArgs(String name, Consumer<String[]> task) {
            super(name);
            this.task = task;
        }

        @Override
        public void doTask(String[] args) {
            task.accept(args);
        }
    }
}
