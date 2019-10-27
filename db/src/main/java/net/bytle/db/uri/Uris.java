package net.bytle.db.uri;

import java.net.URI;

public class Uris {

    static void print(URI uri){


        System.out.println("Opaque            : "+uri.isOpaque());
        System.out.println("Absolute          : "+uri.isAbsolute());
        System.out.println("Scheme            : "+uri.getScheme());
        System.out.println("SchemeSpecificPart: "+uri.getSchemeSpecificPart());
        System.out.println("Authority         : "+uri.getAuthority());
        System.out.println("Host              : "+uri.getHost());
        System.out.println("UserInfo          : "+uri.getUserInfo());
        System.out.println("Path              : "+uri.getPath());
        System.out.println("Query             : "+uri.getQuery());
        System.out.println("Fragment          : "+uri.getFragment());

    }
}
