package net.bytle.db.move;

public class MoveProperties {

    public static final Integer COMMIT_FREQUENCY = 99999;

    // Fetch Size from the database
    public static final Integer FETCH_SIZE = 10000;

    // Batch Size (Insert batch)
    public static final Integer BATCH_SIZE = 10000;


    public static MoveProperties of() {
        return new MoveProperties();
    }

}
