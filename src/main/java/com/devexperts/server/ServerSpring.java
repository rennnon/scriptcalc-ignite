package com.devexperts.server;

import com.devexperts.common.Cache;
import com.devexperts.common.IgniteConfig;
import com.devexperts.common.Utils;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteEvents;
import org.apache.ignite.IgniteException;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Timer;
import java.util.TimerTask;

public class ServerSpring {
    private final static long LOCAL_KEYS_CHECK_INTERVAL_MILLIS = 5000;

    public static void main(String[] args) throws IgniteException {
        System.setProperty("spring.profiles.active", "server");
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(IgniteConfig.class);
        Ignite ignite = context.getBean(Ignite.class);
        Utils.printNodeStats(ignite, System.out);

        setupEventListeners(ignite);
        CacheLocalKeysTracker tracker = CacheLocalKeysTracker.start(ignite, Cache.DISTRIBUTED);
        Timer localKeysCheck = new Timer("localKeysCheck");
        localKeysCheck.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Local keys: " + tracker.getLocalKeysSnapshot());
            }
        }, 0, LOCAL_KEYS_CHECK_INTERVAL_MILLIS);
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
