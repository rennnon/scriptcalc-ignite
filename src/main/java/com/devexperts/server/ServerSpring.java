package com.devexperts.server;

import com.devexperts.common.Cache;
import com.devexperts.common.Heartbeat;
import com.devexperts.common.IgniteConfig;
import com.devexperts.common.Utils;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteEvents;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.cache.event.CacheEntryEvent;

public class ServerSpring {


    public static void main(String[] args) throws IgniteException {
        System.setProperty("spring.profiles.active", "server");
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(IgniteConfig.class);
        Ignite ignite = context.getBean(Ignite.class);
        Utils.printNodeStats(ignite, System.out);

        setupEventListeners(ignite);
        setupDistributedCacheListener(ignite);
    }

    private static void setupDistributedCacheListener(Ignite ignite) {
        ContinuousQuery<String, Heartbeat> query = new ContinuousQuery<>();
        query.setLocalListener(events -> {
            for (CacheEntryEvent<? extends String, ? extends Heartbeat> event : events)
                System.out.println("Cache change: " + event);
        });
        Cache.DISTRIBUTED.get(ignite).query(query);
    }

    private static void setupEventListeners(Ignite ignite) {
        IgniteEvents events = ignite.events(ignite.cluster().forLocal());
        IgnitePredicate<CacheEvent> cacheListener = evt -> {
            System.out.println("Received cache event: " + evt);
            return true;
        };
        events.localListen(cacheListener, EventType.EVT_CACHE_OBJECT_PUT, EventType.EVT_CACHE_OBJECT_REMOVED);

        IgnitePredicate<Event> discoveryListener = evt -> {
            System.out.println("Received discovery event:" + evt);
            return true;
        };
        events.localListen(discoveryListener, EventType.EVT_NODE_JOINED, EventType.EVT_NODE_LEFT);

        IgnitePredicate<Event> rebalanceListener = evt -> {
            System.out.println("Received rebalancing event:" + evt);
            return true;
        };
        events.localListen(rebalanceListener, EventType.EVT_CACHE_REBALANCE_PART_SUPPLIED,
                EventType.EVT_CACHE_REBALANCE_PART_UNLOADED, EventType.EVT_CACHE_REBALANCE_PART_LOADED);
    }
}
