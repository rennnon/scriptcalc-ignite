package com.devexperts.client;

import com.devexperts.common.Cache;
import com.devexperts.common.Heartbeat;
import com.devexperts.common.IgniteConfig;
import com.devexperts.common.Utils;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.UUID;

public class ClientProducerSpring {
    private static final int HEARTBEAT_RATE_MILLIS = 5000;

    @SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
    public static void main(String[] args) throws IgniteException {
        System.setProperty("spring.profiles.active", "client");
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(IgniteConfig.class);
        Ignite ignite = context.getBean(Ignite.class);
        Utils.printNodeStats(ignite, System.out);
        String clientId = UUID.randomUUID().toString();
        IgniteCache<String, Heartbeat> distributedCache = Cache.DISTRIBUTED.get(ignite);

        while (true) {
            long timeMillis = System.currentTimeMillis();
            distributedCache.put(clientId, new Heartbeat(clientId, timeMillis, "distributed"));
            System.out.println("Heartbeat is sent");
            try {
                Thread.sleep(HEARTBEAT_RATE_MILLIS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}