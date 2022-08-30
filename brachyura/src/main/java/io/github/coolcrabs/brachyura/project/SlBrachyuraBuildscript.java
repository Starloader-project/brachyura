package io.github.coolcrabs.brachyura.project;

import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * This class is basically a replacement to the traditional requirement of having to (indirectly) extend
 * {@link Project} in your buildscript file.
 *
 * <p>Unlike the old system that is still employed upstream, this new system has the goal of being far more flexible
 * with defining projects and even supporting multi-project/module builds.
 *
 * @since 0.92.1
 */
public interface SlBrachyuraBuildscript extends DescriptiveBuildscriptName {

    public static interface TaskExecutor {
        public void runTasks(@NotNull String taskName, String... args);
    }

    @NotNull
    public List<@NotNull Project> getProjects();

    @NotNull
    public default TaskExecutor getTaskExecutor() {
        return new TaskExecutor() {
            @Override
            public void runTasks(@NotNull String taskName, String... args) {
                for (Project p : SlBrachyuraBuildscript.this.getProjects()) {
                    p.runTask(taskName, args);
                }
            }
        };
    }
}
