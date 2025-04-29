package fr.gouv.education.acrennes.alambic.monitoring;

import java.util.List;

public interface ActivityCache {
    List<Object> get(String key);

    int size(String key);

    List<List<Object>> getLists(String keyPattern);

    List<String> getKeys(String regex);

    boolean add(String key, Object value);

    boolean add(String key, List<Object> values);

    Object set(String key, int index, Object value);

    void remove(String key);

    void clear();

    void clear(String key);
}
