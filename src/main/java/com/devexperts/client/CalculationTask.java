package com.devexperts.client;

import com.devexperts.common.Utils;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.Query;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.resources.IgniteInstanceResource;

import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A compute tasks that prints out a node ID and some details about its OS and JRE.
 * Plus, the code shows how to access data stored in a cache from the compute task.
 */
class CalculationTask implements IgniteRunnable {
    @IgniteInstanceResource
    Ignite ignite;

    @Override
    public void run() {
        System.out.println("Start calculation task");
        Utils.printNodeStats(ignite, System.out);
        IgniteCache<String, Long> cache = ignite.cache("dpoCache");
        String heartbeats = StreamSupport.stream(cache.spliterator(), false)
                .map(entry -> {
                    String time = Instant.ofEpochMilli(entry.getValue()).toString();
                    return "Server \"" + entry.getKey() + "\": " + time;
                })
                .collect(Collectors.joining("\n", "Last heartbeats:\n", "\n"));
        System.out.println(heartbeats);
        System.out.println("Finish calculation task");
    }
}
