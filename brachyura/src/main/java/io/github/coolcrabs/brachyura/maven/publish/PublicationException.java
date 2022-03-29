package io.github.coolcrabs.brachyura.maven.publish;

import org.jetbrains.annotations.NotNull;

public class PublicationException extends Exception {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 1363592146562777104L;

    public PublicationException(PublishRepository failedRepository, @NotNull Exception cause) {
        super("Cannot publish to repository " + failedRepository.toString(), cause);
    }

    public PublicationException(String message, @NotNull Exception cause) {
        super(message, cause);
    }
}
