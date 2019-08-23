package net.bytle.type;

import java.util.Map;

public class Maps {

    /**
     * Search the map for a key where the case does not matter
     * @param properties
     * @param key
     * @return an object
     */
    public static Object getPropertyCaseIndependent(Map<String, Object> properties, String key) {
        for (Map.Entry<String,Object> entry:properties.entrySet()){
            if (key.toLowerCase().equals(entry.getKey().toLowerCase())){
                return entry.getValue();
            }
        }
        return null;
    }
}
