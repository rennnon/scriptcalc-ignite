package com.devexperts.common;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

public class Heartbeat {
    @QuerySqlField(index = true)
    private final String sourceId;
    @QuerySqlField
    private final long timeMillis;
    @QuerySqlField
    private final String message;

    public Heartbeat(String sourceId, long timeMillis, String message) {
        this.sourceId = sourceId;
        this.timeMillis = timeMillis;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Heartbeat{" +
                "sourceId='" + sourceId + '\'' +
                ", timeMillis=" + timeMillis +
                ", message='" + message + '\'' +
                '}';
    }
}