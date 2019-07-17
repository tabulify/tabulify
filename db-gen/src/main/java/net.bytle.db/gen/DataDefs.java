package net.bytle.db.gen;


import net.bytle.db.engine.Tables;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Retrieve a list of Data Def
 */
public class DataDefs {



    public static List<DataDef> load(Path path) {

        InputStream input;
        try {
            input = Files.newInputStream(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Transform the file in properties
        Constructor constructor = new Constructor(DataDef.class);
        Yaml yaml = new Yaml(constructor);
        Iterable<Object> dataObject = yaml.loadAll(input);
        List<DataDef> dataDefs = new ArrayList<>();
        for (Object data : dataObject) {
            final DataDef dataDef = (DataDef) data;
            dataDefs.add(dataDef);
        }

        return dataDefs;

    }




}
