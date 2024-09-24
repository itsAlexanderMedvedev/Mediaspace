package com.amedvedev.mediaspace.redis;

import java.util.concurrent.TimeUnit;

public record TtlUpdateEntry(String key, int ttl, TimeUnit timeUnit) {}
