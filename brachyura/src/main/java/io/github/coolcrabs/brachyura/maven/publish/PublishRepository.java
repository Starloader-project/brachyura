package io.github.coolcrabs.brachyura.maven.publish;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;

public interface PublishRepository {

    /**
     * Publishes a resource to the repository.
     *
     * @param id The id to publish the file under.
     * @param source The contents of the published resource
     * @throws IOException If there was an error during publishing
     */
    public void publish(@NotNull PublicationId id, byte @NotNull[] source) throws IOException;

    /**
     * Publishes a resource to the repository.
     *
     * @param id The id to publish the file under.
     * @param source The stream that will contain the contents of the published resource
     * @throws IOException If there was an error while reading the source stream or if there was an error during publishing
     */
    public void publish(@NotNull PublicationId id, @NotNull InputStream source) throws IOException;

    /**
     * Publishes an already existing resource to the publication repository.
     *
     * @param id The id to publish the file under.
     * @param source The source to copy the file from.
     * @throws IOException If there was an error while reading the source file or if there was an error during publishing
     */
    public void publish(@NotNull PublicationId id, @NotNull Path source) throws IOException;
}
