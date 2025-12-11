package com.tabulify.exception;

/**
 * Used when an exec command returns a bad exit code
 */
public class BadExitStatusException extends Exception {


    private final Integer exitStatus;

    public BadExitStatusException(String s, Integer exitStatus) {

        super(s);
        this.exitStatus = exitStatus;
    }

    public BadExitStatusException(Exception e) {
        super(e);
        this.exitStatus = 1;
    }

    @SuppressWarnings("unused")
    public Integer getExitStatus() {
        return exitStatus;
    }
}
