package com.devexperts.common;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.jetbrains.annotations.NotNull;

public enum Cache {
    @SuppressWarnings("unused")
    LOCAL("LOCALCACHE", true),
    DISTRIBUTED("DISTRIBUTEDCACHE", false);

    private final CacheConfiguration<String, Heartbeat> config;

    Cache(String name, boolean local) {
        config = new CacheConfiguration<>(name);
        if (local) {
            config
                    .setBackups(0)
                    .setSqlSchema("LOCAL");
        } else {
            config
                    .setCacheMode(CacheMode.PARTITIONED)
                    .setBackups(1)
                    .setAffinity(new RendezvousAffinityFunction(false));
        }
        config
                .setAtomicityMode(CacheAtomicityMode.ATOMIC)
                .setIndexedTypes(String.class, Heartbeat.class);
    }

    public IgniteCache<String, Heartbeat> get(@NotNull Ignite ignite) {
        return ignite.getOrCreateCache(config);
    }
}