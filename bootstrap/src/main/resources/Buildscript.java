import org.jetbrains.annotations.NotNull;

import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.project.DescriptiveBuildscriptName;
import io.github.coolcrabs.brachyura.project.java.SimpleJavaProject;

public class Buildscript extends SimpleJavaProject implements DescriptiveBuildscriptName {

    @Override
    @NotNull
    public String getBuildscriptName() {
        return "ExampleBuildscript";
    }

    @Override
    public MavenId getId() {
        return new MavenId("org.example", "example", "0.0.1-SNAPSHOT");
    }
}
