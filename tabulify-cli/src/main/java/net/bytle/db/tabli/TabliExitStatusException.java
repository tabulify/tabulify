package net.bytle.db.tabli;

/**
 * Used in test to report a bad exit status
 */
public class TabliExitStatusException extends RuntimeException {


    private final Integer exitStatus;


    public TabliExitStatusException(int exitStatus) {
        super();
        this.exitStatus = 1;
    }

    @SuppressWarnings("unused")
    public Integer getExitStatus() {
        return exitStatus;
    }

}
