package net.slipcor.pvparena.exceptions;

import net.slipcor.pvparena.core.Language;

public class GameplayRuntimeException extends RuntimeException {
    public GameplayRuntimeException(String message) {
        super(message);
    }

    public GameplayRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameplayRuntimeException(Language.MSG message) {
        super(Language.parse(message));
    }

    public GameplayRuntimeException(Language.MSG message, Throwable cause) {
        super(Language.parse(message), cause);
    }
}
