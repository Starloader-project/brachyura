package io.github.coolcrabs.brachyura.project;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.exception.TaskFailedException;

class Tasks implements Consumer<Task> {

    @NotNull
    private final Map<String, Task> tasks = new HashMap<>();

    @Override
    public void accept(Task task) {
        if (tasks.putIfAbsent(task.name, task) != null) {
            throw new TaskFailedException("Duplicate task for " + task.name);
        }
    }

    Task get(String name) {
        return Objects.requireNonNull(tasks.get(name), "Unknown task " + name);
    }

    @NotNull
    public Map<String, Task> getAllTasks() {
        return tasks;
    }

    @Override
    @NotNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Task> a : tasks.entrySet()) {
            sb.append(a.getKey());
            sb.append(", ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
            return sb.toString();
        } else {
            return "[None]";
        }
    }
}
