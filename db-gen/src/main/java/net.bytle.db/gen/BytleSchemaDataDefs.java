package net.bytle.db.gen;

import net.bytle.db.model.DataDefs;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a BytleSchemaLoad to help load data in the Bytle Schema
 * This class is not in the core module db to not introduce a dependency cycle
 * This is an internal test class
 * Let it here please
 */
public class BytleSchemaDataDefs {


    public static List<DataPath> getDataDefs(){

        try {
            List<DataPath> dataPaths = new ArrayList<>();
            List<URL> urls = new ArrayList<>();
            urls.add(BytleSchemaDataDefs.class.getResource("/DataDef/F_SALES--datadef.yml"));
            urls.add(BytleSchemaDataDefs.class.getResource("/DataDef/D_CATEGORY--datadef.yml"));
            urls.add(BytleSchemaDataDefs.class.getResource("/DataDef/D_TIME--datadef.yml"));

            for (URL url: urls) {
                dataPaths.addAll(DataDefs.load(Paths.get(url.toURI())));
            }
            return dataPaths;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

}
