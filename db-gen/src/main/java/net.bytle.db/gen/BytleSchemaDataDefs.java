package net.bytle.db.gen;

import net.bytle.db.model.DataDef;
import net.bytle.db.model.DataDefs;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * This is a BytleSchemaLoad to help load data in the Bytle Schema
 * This class is not in the core module db to not introducte a dependency cycle
 * This is an internal test class
 * Let it here plase
 */
public class BytleSchemaDataDefs {


    public static List<DataDef> getDataDefs(){

        try {
            URL url = BytleSchemaDataDefs.class.getResource("/DataDef/BytleSchemaDataDef.yml");
            Path dataDef = Paths.get(url.toURI());
            return DataDefs.load(dataDef);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

}
