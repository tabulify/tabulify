package com.tabulify.tabul;

/**
 * Used in test to report a bad exit status
 */
public class TabulExitStatusException extends RuntimeException {


    private final Integer exitStatus;


  public TabulExitStatusException(int exitStatus, Exception e) {
    super(e);
        this.exitStatus = exitStatus;
    }

    @SuppressWarnings("unused")
    public Integer getExitStatus() {
        return exitStatus;
    }

}
