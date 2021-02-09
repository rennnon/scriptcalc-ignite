package com.devexperts.server;

import com.devexperts.common.Cache;
import com.devexperts.common.Heartbeat;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cluster.ClusterNode;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.EventType;
import java.util.*;
import java.util.stream.Collectors;

public class CacheLocalKeysTracker {
    private final Ignite ignite;
    private final Cache cache;
    private final Affinity<String> affinity;
    /**
     * partition id -> set of keys of this partition
     * should be accessed under instance lock
     */
    private final Map<Integer, Set<String>> keysMapping = new HashMap<>();

    private CacheLocalKeysTracker(Ignite ignite, Cache cache) {
        this.ignite = ignite;
        this.cache = cache;
        affinity = cache.getAffinity(ignite);
    }

    public static CacheLocalKeysTracker start(Ignite ignite, Cache cache) {
        CacheLocalKeysTracker tracker = new CacheLocalKeysTracker(ignite, cache);
        tracker.startCacheListener();
        return tracker;
    }

    public synchronized Set<String> getLocalKeysSnapshot() {
        return getLocalPartitions().stream()
                .map(partition -> keysMapping.getOrDefault(partition, Collections.emptySet()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private void startCacheListener() {
        ContinuousQuery<String, Heartbeat> query = new ContinuousQuery<>();
        query.setLocalListener(events -> {
            for (CacheEntryEvent<? extends String, ? extends Heartbeat> event : events) {
                handleEvent(event.getEventType(), event.getKey());
            }
        });
        cache.get(ignite).query(query);
    }

    private synchronized void handleEvent(EventType type, String key) {
        switch (type) {
            case CREATED:
                getKnownPartitionKeys(key).add(key);
                System.out.println(cache + " added: " + key);
                break;
            case REMOVED:
                getKnownPartitionKeys(key).remove(key);
                System.out.println(cache + " removed: " + key);
                break;
            default:
                System.out.println(cache + " \"" + type + "\" event for: " + key);
                break;
        }
    }

    private Set<String> getKnownPartitionKeys(String key) {
        return keysMapping.computeIfAbsent(getPartition(key), partition -> new HashSet<>());
    }

    private int getPartition(String key) {
        return affinity.partition(key);
    }

    private List<Integer> getLocalPartitions() {
        ClusterNode localNode = ignite.cluster().localNode();
        int[] nodes = affinity.allPartitions(localNode);
        return Arrays.stream(nodes).boxed().collect(Collectors.toList());
    }
}
