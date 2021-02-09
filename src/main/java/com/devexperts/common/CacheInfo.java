package com.devexperts.common;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.jetbrains.annotations.NotNull;

public enum CacheInfo {
    @SuppressWarnings("unused")
    LOCAL("LOCALCACHE", true),
    DISTRIBUTED("DISTRIBUTEDCACHE", false);

    private final CacheConfiguration<String, Heartbeat> config;
    private final String name;

    CacheInfo(String name, boolean local) {
        this.name = name;
        config = new CacheConfiguration<>(name);
        if (local) {
            config
                    .setBackups(0)
                    .setSqlSchema("LOCAL");
        } else {
            config
                    .setCacheMode(CacheMode.PARTITIONED)
                    .setBackups(0)
                    .setAffinity(new RendezvousAffinityFunction(false));
        }
        config
                .setAtomicityMode(CacheAtomicityMode.ATOMIC)
                .setIndexedTypes(String.class, Heartbeat.class);
    }

    public IgniteCache<String, Heartbeat> get(@NotNull Ignite ignite) {
        return ignite.getOrCreateCache(config);
    }

    public Affinity<String> getAffinity(@NotNull Ignite ignite) {
        return ignite.affinity(name);
    }

    @Override
    public String toString() {
        return "Cache{" +
                "name='" + name + '\'' +
                '}';
    }
}