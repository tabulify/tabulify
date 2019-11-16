package net.bytle.type;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Clob;
import java.sql.SQLException;

public class Clobs {

    public String toString(Clob clob) {
        assert clob != null : "clob should not be null";

        try (
            Reader reader = clob.getCharacterStream();
            Writer writer = new StringWriter();
        ) {
            int c;
            while ((c = reader.read()) != -1) {
                writer.append((char) c);
            }
            clob.free();
            return writer.toString();

        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
