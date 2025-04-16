package com.tabulify.tabli;

/**
 * Used in test to report a bad exit status
 */
public class TabliExitStatusException extends RuntimeException {


    private final Integer exitStatus;


    public TabliExitStatusException(int exitStatus) {
        super();
        this.exitStatus = exitStatus;
    }

    @SuppressWarnings("unused")
    public Integer getExitStatus() {
        return exitStatus;
    }

}
