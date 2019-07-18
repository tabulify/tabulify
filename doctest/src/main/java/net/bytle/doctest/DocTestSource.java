package net.bytle.doctest;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;


/**
 * A file object used to represent a java source coming from a string.
 * This class is used by the {@link DocTestExecutorUnit#eval(DocTestUnit)} function
 * when compiling to represent the Java source code in a string in place of in a file
 * Therefore no file is created.
 */
public class DocTestSource extends SimpleJavaFileObject {


    /**
     * The source code of this "file".
     */
    final String code;
    private final String className;


    /**
     * Constructs a new JavaSourceFromString.
     *
     * @param name the name of the compilation unit represented by this file object
     * @param code the source code for the compilation unit represented by this file object
     */
    DocTestSource(String name, String code) {
        super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension),
                Kind.SOURCE);
        this.className = name;
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }

}

