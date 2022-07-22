package com.rxf113.miniomultipartupload.util.okhttp;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * @author rxf113
 */

public enum OkhttpEnum {
    //**//
    INSTANCE;

    private final OkHttpClient client;

    OkhttpEnum() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // dispatcher
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(200);
        dispatcher.setMaxRequestsPerHost(200);

        // connectionPool
        ConnectionPool connectionPool = new ConnectionPool(2000, 5, TimeUnit.MINUTES);

        builder.dispatcher(dispatcher);
        builder.connectionPool(connectionPool);

        client = builder.build();
    }

    public OkHttpClient getClient() {
        return client;
    }
}