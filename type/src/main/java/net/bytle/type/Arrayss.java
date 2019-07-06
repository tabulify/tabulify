package net.bytle.type;

/**
 * Arrays utilities
 * Two ss to not clash with java.utils.Arrays
 */
public class Arrayss {

    /**
     * Does the array contains a character
     * @param haystack
     * @param needle
     * @return
     */
    public static boolean in(char[] haystack, char needle){
        for(char c:haystack){
            if (needle==c){
                return true;
            }
        }
        return false;
    }

}
