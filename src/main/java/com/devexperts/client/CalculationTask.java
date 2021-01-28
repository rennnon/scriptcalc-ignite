package com.devexperts.client;

import com.devexperts.common.Cache;
import com.devexperts.common.Utils;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.resources.IgniteInstanceResource;

import java.time.Instant;
import java.util.List;

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
        printAllHeartbeats(Cache.DISTRIBUTED, true);
        System.out.println("---");
        printAllHeartbeats(Cache.DISTRIBUTED, false);
        System.out.println("Finish calculation task");
    }

    @SuppressWarnings("SameParameterValue") // it's nice to have an ability to pass Cache.LOCAL here
    private void printAllHeartbeats(Cache cache, boolean localQuery) {
        SqlFieldsQuery query = new SqlFieldsQuery("select sourceId, timeMillis, message from Heartbeat");
        query.setLocal(localQuery);
        try (FieldsQueryCursor<List<?>> cursor = cache.get(ignite).query(query)) {
            StringBuilder builder = new StringBuilder(cache.name());
            builder.append(localQuery ? " local query\n" : " global query\n");
            for (List<?> row : cursor) {
                builder.append("\tsrc: ").append(row.get(0)).append(", ");
                String time = Instant.ofEpochMilli((Long) row.get(1)).toString();
                builder.append("time: ").append(time).append(", ");
                builder.append("msg: \"").append(row.get(2)).append("\"\n");
            }
            System.out.println(builder.toString());
        }
    }
}
