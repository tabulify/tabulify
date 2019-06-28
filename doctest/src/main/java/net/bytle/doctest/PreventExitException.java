package net.bytle.doctest;

public class PreventExitException extends RuntimeException {

    public PreventExitException(String message) {
        super(message);
    }

}
