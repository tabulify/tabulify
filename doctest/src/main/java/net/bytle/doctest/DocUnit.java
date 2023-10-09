package net.bytle.doctest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link DocUnit} contains the data of a doc unit test. ie:
 *
 *   * one or more file blocks
 *   * the code - one code block
 *   * the output expectation - one console
 *   * the code language
 */
public class DocUnit {

    private String code;
    private String console;
    private String language;
    private Integer[] consoleLocation;
    private Map<String, String> env = new HashMap<>();
    private List<DocFileBlock> docFileBlocks = new ArrayList<DocFileBlock>();
    private Integer[] codeLocation;
    private Path path; // Path of the docFile


    public static DocUnit get() {
        return new DocUnit();
    }


    public String getCode() {

        return this.code;

    }

    public DocUnit setCode(String code) {

        this.code = code;
        return this;

    }

    /**
     *
     * @return the console content
     */
    public String getConsole() {

        return this.console;

    }

    public DocUnit setConsoleContent(String expectation) {
        this.console = expectation;
        return this;
    }

    public String getLanguage() {
        return this.language;
    }

    public DocUnit setLanguage(String language) {
        this.language = language;
        return this;
    }

    /**
     * Location of the console in the file
     * @return
     */
    public Integer[] getConsoleLocation() {
        return consoleLocation;
    }

    public DocUnit setConsoleLocation(Integer[] consoleLocation) {
        this.consoleLocation = consoleLocation;
        return this;
    }

    public DocUnit setProperty(String properties) {

        properties = properties.trim();
        if (properties.equals("")) {
            return this;
        }

        String[] props = properties.split(" ");
        for (int i = 0; i < props.length; i++) {

            String[] prop = props[i].split("=");
            String key = prop[0];
            String value = prop[1];

            if (key.startsWith("env")) {
                this.env.put(key.substring(3), value);
            }

        }
        return this;
    }

    public Map<String, String> getEnv() {
        return this.env;
    }

    @Override
    public String toString() {
        return code;
    }

    public List<DocFileBlock> getFileBlocks() {

        return docFileBlocks;
    }

    public void addFileBlock(DocFileBlock docFileBlock) {
        this.docFileBlocks.add(docFileBlock);
    }

    public Integer[] getCodeLocation() {
        return this.codeLocation;
    }

    public void setCodeLocation(Integer[] locations) {
        this.codeLocation = locations;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
