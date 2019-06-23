package net.bytle.doctest;

import java.nio.file.Path;

/**
 * The result of a run executed
 * via {@link DocTest#run(Path)}
 */
public class DocTestRunResult {


    private final Path path;
    private String doc;
    private int error = 0;

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
}
