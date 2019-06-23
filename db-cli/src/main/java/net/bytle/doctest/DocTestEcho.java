package net.bytle.doctest;

/**
 * An example of a {@link DocTestCodeRunner#addMainClass(String, String) MainClass}
 * implementing a basic echo cli
 * <p>
 * This class is used for testing purpose
 * <p>
 * In the documentation, you would see something like that
 * <p>
 * echo Hello Nico
 */
public class DocTestEcho {

    public static void main(String[] args) {
        System.out.println(String.join("", args));
    }

}
