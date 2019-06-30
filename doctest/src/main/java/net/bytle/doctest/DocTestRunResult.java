package net.bytle.doctest;

import java.nio.file.Path;
import java.util.Map;

/**
 * The result of a run executed
 * via {@link DocTestRunner#run(Path, Map)}
 */
public class DocTestRunResult {


    private final Path path;
    private String doc;
    private int error = 0;
    // Indicate if the doc has been executed
    private boolean hasBeenExecuted = false;

    public DocTestRunResult(Path path) {
        this.path = path;
    }

    public static DocTestRunResult get(Path path) {
        return new DocTestRunResult(path);
    }

    public String getNewDoc() {
        return this.doc;
    }

    public void setNewDoc(String doc) {
        this.doc = doc;
    }

    public void addError() {
        this.error++;
    }

    public int getErrors() {
        return this.error;
    }

    public boolean hasRun() {
        return this.hasBeenExecuted;
    }

    public DocTestRunResult setHasBeenExecuted(boolean b) {
        this.hasBeenExecuted = true;
        return this;
    }
}
