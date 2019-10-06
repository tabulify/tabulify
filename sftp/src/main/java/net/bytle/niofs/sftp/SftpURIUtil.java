package net.bytle.niofs.sftp;

import java.net.URI;


/**
 * Created by gerard on 17-06-2016.
 * Class from static util function over URI
 */
public class SftpURIUtil {


    /**
     * For printing and test purpose
     * print the URI without the pwd
     * @param uri
     * @return a string without the pwd
     */
    public static String toStringWithoutPassword(URI uri)  {
        return uri.getScheme()+"://"+
                (getUserFromUserInfo(uri) != null ? getUserFromUserInfo(uri) : "null" )
                +"@"+
                (uri.getHost() != null ? uri.getHost() : "null")
                +":"+
                (uri.getPort() !=-1 ? uri.getPort() : String.valueOf(SftpFileSystem.DEFAULT_PORT))
                +uri.getPath();
    }


    // Extract the user and the password
    public static String getUserFromUserInfo(URI uri) {
        String userInfo = uri.getUserInfo();

        if (userInfo != null) {
            return userInfo.substring(0, userInfo.indexOf(":"));
        } else {
            return null;
        }
    }

    // Extract the user and the password
    public static String getPasswordFromUserInfo(URI uri) {
        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            return userInfo.substring(userInfo.indexOf(":") + 1, userInfo.length());
        } else {
            return null;
        }
    }



}
