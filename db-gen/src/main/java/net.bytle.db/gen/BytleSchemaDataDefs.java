package net.bytle.db.gen;

import net.bytle.db.model.DataDefs;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a BytleSchemaLoad to help load data in the Bytle Schema
 * This class is not in the core module db to not introduce a dependency cycle
 * This is an internal test class
 * Let it here please
 */
public class BytleSchemaDataDefs {


    public static List<DataGenDef> getDataDefs(){

        try {
            URL url = BytleSchemaDataDefs.class.getResource("/DataDef/BytleSchemaDataDef.yml");
            Path dataDef = Paths.get(url.toURI());
            return DataDefs.load(dataDef)
                    .stream()
                    .map(s-> (DataGenDef.get(s)))
                    .collect(Collectors.toList());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

}
