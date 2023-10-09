package net.bytle.doctest;

/**
 * A representation of a file block in
 * a {@link DocUnit}
 */
public class DocFileBlock {

    /**
     * The parent unit node
     */
    private final DocUnit docUnit;

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

    public DocFileBlock(DocUnit docUnit) {
        this.docUnit = docUnit;
    }

    static DocFileBlock get(DocUnit docUnit) {
        return new DocFileBlock(docUnit);
    }

    public DocFileBlock setContent(String content) {
        String contentTrim = content.trim();
        if (!contentTrim.equals("")) {
            this.content = contentTrim;
        }
        return this;
    }

    public String getPath() {
        return this.path;
    }

    public DocFileBlock setPath(String path) {
        this.path = path;
        return this;
    }

    public String getLanguage() {
        return this.language;
    }

    public DocFileBlock setLanguage(String language) {
        this.language = language;
        return this;
    }

    public Integer getLocationEnd() {
        return this.locationEnd;
    }

    public DocFileBlock setLocationEnd(int locationEnd) {
        this.locationEnd = locationEnd;
        return this;
    }

    public int getLocationStart() {
        return this.locationStart;
    }

    public DocFileBlock setLocationStart(int locationStart) {
        this.locationStart = locationStart;
        return this;
    }

    public String getContent() {
        return this.content;
    }
}
