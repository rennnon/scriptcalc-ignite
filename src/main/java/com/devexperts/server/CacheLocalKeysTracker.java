package com.devexperts.server;

import com.devexperts.common.CacheInfo;
import com.devexperts.common.Heartbeat;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cache.query.ContinuousQueryWithTransformer;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.lang.IgniteClosure;
import org.jetbrains.annotations.NotNull;

import javax.cache.Cache;
import javax.cache.configuration.Factory;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.EventType;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class CacheLocalKeysTracker {
    private final Ignite ignite;
    private final CacheInfo cacheInfo;
    private final Affinity<String> affinity;
    /**
     * partition id -> set of keys of this partition
     * should be accessed under instance lock
     */
    private final Map<Integer, Set<String>> keysMapping = new HashMap<>();

    private CacheLocalKeysTracker(Ignite ignite, CacheInfo cacheInfo) {
        this.ignite = ignite;
        this.cacheInfo = cacheInfo;
        affinity = cacheInfo.getAffinity(ignite);
    }

    public static CacheLocalKeysTracker start(Ignite ignite, CacheInfo cache) {
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

    public CacheInfo getTrackedCacheInfo() {
        return cacheInfo;
    }

    private synchronized void startCacheListener() {
        ContinuousQueryWithTransformer<String, Heartbeat, CacheTrackerEvent> query =
                new ContinuousQueryWithTransformer<>();
        Factory<IgniteClosure<CacheEntryEvent<? extends String, ? extends Heartbeat>, CacheTrackerEvent>> factory =
                FactoryBuilder.factoryOf(
                        (IgniteClosure<CacheEntryEvent<? extends String, ? extends Heartbeat>, CacheTrackerEvent>)
                        CacheTrackerEvent::new);
        query.setRemoteTransformerFactory(factory);
        query.setInitialQuery(new ScanQuery<>());
        query.setLocalListener(events -> {
            for (CacheTrackerEvent event : events) {
                processKey(event.getKey(), event.getType());
            }
        });
        QueryCursor<Cache.Entry<String, Heartbeat>> cursor = cacheInfo.get(ignite).query(query);
        for (javax.cache.Cache.Entry<String, Heartbeat> entry : cursor) {
            System.out.println(entry);
            processKey(entry.getKey(), EventType.CREATED);
        }
    }

    private synchronized void processKey(String key, EventType action) {
        switch (action) {
            case CREATED:
                getKnownPartitionKeys(key).add(key);
                System.out.println(cacheInfo + " added: " + key);
                break;
            case REMOVED:
                getKnownPartitionKeys(key).remove(key);
                System.out.println(cacheInfo + " removed: " + key);
                break;
            default:
                System.out.println(cacheInfo + " \"" + action + "\" event for: " + key);
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

    private static class CacheTrackerEvent implements Serializable {
        @NotNull
        private final String key;
        @NotNull
        private final EventType type;

        private CacheTrackerEvent(CacheEntryEvent<? extends String, ? extends Heartbeat> event) {
            key = event.getKey();
            type = event.getEventType();
        }

        public @NotNull String getKey() {
            return key;
        }

        public @NotNull EventType getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheTrackerEvent that = (CacheTrackerEvent) o;
            return key.equals(that.key) && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, type);
        }
    }
}
