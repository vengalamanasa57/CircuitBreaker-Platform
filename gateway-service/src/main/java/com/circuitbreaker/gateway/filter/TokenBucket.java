package com.circuitbreaker.gateway.filter;

import java.util.function.LongSupplier;

/**
 * Thread-safe token-bucket algorithm.
 *
 * <p>The bucket holds up to {@code capacity} tokens and is refilled continuously
 * at {@code refillPerSecond}. Each request consumes one token; with an empty
 * bucket the request is rejected. This allows short bursts (up to capacity) while
 * enforcing a sustained average rate.
 *
 * <p>The clock is injectable so the behaviour can be unit-tested deterministically.
 */
public final class TokenBucket {

    private final int capacity;
    private final double refillPerSecond;
    private final LongSupplier nanoClock;

    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(int capacity, double refillPerSecond) {
        this(capacity, refillPerSecond, System::nanoTime);
    }

    TokenBucket(int capacity, double refillPerSecond, LongSupplier nanoClock) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        if (refillPerSecond < 0) {
            throw new IllegalArgumentException("refillPerSecond must be >= 0");
        }
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.nanoClock = nanoClock;
        this.tokens = capacity;
        this.lastRefillNanos = nanoClock.getAsLong();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    public synchronized int remaining() {
        refill();
        return (int) Math.floor(tokens);
    }

    /** True when the bucket was not touched for {@code idleNanos}; safe to evict. */
    public synchronized boolean isIdleFor(long idleNanos) {
        return nanoClock.getAsLong() - lastRefillNanos > idleNanos;
    }

    private void refill() {
        long now = nanoClock.getAsLong();
        double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
        tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
        lastRefillNanos = now;
    }
}
