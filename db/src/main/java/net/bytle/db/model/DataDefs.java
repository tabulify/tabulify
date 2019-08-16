package net.bytle.db.model;


import org.yaml.snakeyaml.Yaml;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        Yaml yaml = new Yaml();

        // Every document is one dataDef
        List<Map<String, Object>> documents = new ArrayList<>();
        for (Object data : yaml.loadAll(input)) {
            documents.add((Map<String, Object>) data);
        }

        List<DataDef> dataDefs = new ArrayList<>();
        switch (documents.size()) {
            case 0:
                break;
            default:
                for (int i = 0; i < documents.size(); i++) {

                    Map<String, Object> document = documents.get(i);
                    Object o = getCaseInsensitiveKey(document,"name");
                    String name;
                    if (o == null) {
                        name = path.getFileName().toString();
                    } else {
                        name = (String) o;
                    }
                    DataDef dataDef = DataDefs.get(name);
                    dataDefs.add(dataDef);
                    o = getCaseInsensitiveKey(document,"columns");
                    if (o != null) {
                        try {
                            Map<String, Object> columns = (Map<String, Object>) o;
                        } catch (ClassCastException e){
                            String message = "The columns of the data def ("+name+") must be in a map format. ";
                            if (o.getClass().equals(java.util.ArrayList.class)){
                                message += "They are in a list format. You should suppress the minus if they are present.";
                            }
                            message += "Bad Columns Values are: "+ o;
                            throw new RuntimeException(message,e);
                        }
                    }

                }
                break;
        }
        return dataDefs;

    }

    private static Object getCaseInsensitiveKey(Map<String, Object> maps, String insensitiveKey) {
        String sensitiveKeyInMap = null;
        for (String key:maps.keySet()){
            if (key.toLowerCase().equals(insensitiveKey.toLowerCase())){
                sensitiveKeyInMap=key;
                break;
            }
        }
        return maps.get(sensitiveKeyInMap);
    }


    private static DataDef get(String name) {

        return new DataDef(name);
    }


}
