package net.bytle.doctest;

/**
 * A representation of a file block in
 * a {@link DocTestUnit}
 */
public class DocTestFileBlock {

    /**
     * The parent unit node
     */
    private final DocTestUnit docTestUnit;

    /**
     * The file properties
     * * language
     * * path location
     */
    private String language;
    /**
     * The path to the file
     */
    private String path;
    /**
     * The content if any
     */
    private String content = null;
    /**
     * The start location in the file by characters
     */
    private Integer locationStart = null;
    private Integer locationEnd = null;

    public DocTestFileBlock(DocTestUnit docTestUnit) {
        this.docTestUnit = docTestUnit;
    }

    static DocTestFileBlock get(DocTestUnit docTestUnit) {
        return new DocTestFileBlock(docTestUnit);
    }

    public DocTestFileBlock setContent(String content) {
        String contentTrim = content.trim();
        if (!contentTrim.equals("")) {
            this.content = contentTrim;
        }
        return this;
    }

    public String getPath() {
        return this.path;
    }

    public DocTestFileBlock setPath(String path) {
        this.path = path;
        return this;
    }

    public String getLanguage() {
        return this.language;
    }

    public DocTestFileBlock setLanguage(String language) {
        this.language = language;
        return this;
    }

    public Integer getLocationEnd() {
        return this.locationEnd;
    }

    public DocTestFileBlock setLocationEnd(int locationEnd) {
        this.locationEnd = locationEnd;
        return this;
    }

    public int getLocationStart() {
        return this.locationStart;
    }

    public DocTestFileBlock setLocationStart(int locationStart) {
        this.locationStart = locationStart;
        return this;
    }

    public String getContent() {
        return this.content;
    }
}
