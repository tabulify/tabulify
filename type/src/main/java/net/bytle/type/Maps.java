package net.bytle.type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    /**
     *
     * @param map
     * @param <K>
     * @param <V>
     * @return a list of the entry sorted in natural order
     */
    public static <K, V extends Comparable<? super V>> List<Map.Entry<K, V>> getListSortedByValue(Map<K, V> map) {

        // Making a list of entry
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        // Sorting
        list.sort(Map.Entry.comparingByValue());

        return list;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> getMapSortByValue(Map<K, V> map) {

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : getListSortedByValue(map)) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;

    }
}
