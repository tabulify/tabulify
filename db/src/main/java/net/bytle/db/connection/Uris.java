package net.bytle.db.connection;

import java.net.URI;

public class Uris {

    static void print(URI uri){
        String schemeSpecificPart = uri.getSchemeSpecificPart();
        String server = schemeSpecificPart.substring(0, schemeSpecificPart.indexOf(":"));
        System.out.println("SchemeSpecificPart: "+schemeSpecificPart);
        System.out.println("Server            : "+server);
        System.out.println("Authority         : "+uri.getAuthority());
        System.out.println("Fragment          : "+uri.getFragment());
        System.out.println("Host              : "+uri.getHost());
        System.out.println("UserInfo          : "+uri.getUserInfo());
    }
}
