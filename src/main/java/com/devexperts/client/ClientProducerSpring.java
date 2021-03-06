package com.devexperts.client;

import com.devexperts.common.CacheInfo;
import com.devexperts.common.Heartbeat;
import com.devexperts.common.IgniteConfig;
import com.devexperts.common.Utils;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ClientProducerSpring {
    private static final int HEARTBEAT_RATE_MILLIS = 5000;
    private static final List<String> keys = Arrays.asList("foo", "bar", "zip", "zap");

    @SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
    public static void main(String[] args) throws IgniteException {
        System.setProperty("spring.profiles.active", "client");
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(IgniteConfig.class);
        Ignite ignite = context.getBean(Ignite.class);
        Utils.printNodeStats(ignite, System.out);
        int clientId = UUID.randomUUID().toString().hashCode();
        IgniteCache<String, Heartbeat> distributedCache = CacheInfo.DISTRIBUTED.get(ignite);

        int i = 0;
        Random random = new Random();
        while (true) {
            long timeMillis = System.currentTimeMillis();
            String key = keys.get(i % keys.size()) + "_" + clientId;
            boolean insert = random.nextBoolean();
            if (insert) {
                distributedCache.put(key, new Heartbeat(key, timeMillis, "distributed"));
                System.out.println("Heartbeat is added");
            } else {
                distributedCache.remove(key);
                System.out.println("Hearbeat is removed");
            }
            i++;
            try {
                Thread.sleep(HEARTBEAT_RATE_MILLIS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}