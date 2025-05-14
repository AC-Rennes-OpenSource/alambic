package fr.gouv.education.acrennes.alambic.monitoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ActivityCacheImpl implements ActivityCache {

    private final Map<String, List<Object>> cache = new ConcurrentHashMap<>();

    @Override
    public List<Object> get(final String key) {
        cache.putIfAbsent(key, new ArrayList<>());
        return cache.get(key);
    }

    @Override
    public int size(final String key) {
        return get(key).size();
    }

    @Override
    public List<List<Object>> getLists(final String keyPattern) {
        return cache.entrySet().stream()
                .filter(entry -> entry.getKey().matches(keyPattern))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getKeys(final String regex) {
        return cache.keySet().stream()
                .filter(k -> k.matches(regex))
                .collect(Collectors.toList());
    }

    @Override
    public boolean add(final String key, final Object value) {
        cache.putIfAbsent(key, new ArrayList<>());
        return cache.get(key).add(value);
    }

    @Override
    public boolean add(final String key, final List<Object> values) {
        cache.putIfAbsent(key, new ArrayList<>());
        return cache.get(key).addAll(values);
    }

    @Override
    public Object set(final String key, final int index, final Object value) {
        return cache.get(key).set(index, value);
    }

    @Override
    public void remove(final String key) {
        cache.remove(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void clear(final String key) {
        if (cache.containsKey(key)) {
            cache.get(key).clear();
        }
    }
}
