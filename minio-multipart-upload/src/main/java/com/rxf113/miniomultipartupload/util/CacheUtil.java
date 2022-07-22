package com.rxf113.miniomultipartupload.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

/**
 * @author rxf113
 */
public class CacheUtil {

    private CacheUtil() {
    }

    private static final Cache<String, Object> CACHE_MAP = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .concurrencyLevel(1)
            .recordStats()
            .build();

    public static void put(String key, Object val) {
        CACHE_MAP.put(key, val);
    }

    public static Object get(String key) {
        return CACHE_MAP.getIfPresent(key);
    }
}
