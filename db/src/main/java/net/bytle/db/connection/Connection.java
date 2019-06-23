package net.bytle.db.connection;

public interface Connection {

    String getUrl() ;
    String getName() ;
    void close();

    Connection setUrl(String string);

}
