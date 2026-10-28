package com.circuitbreaker.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenBucketTest {

    private final AtomicLong nanos = new AtomicLong();

    @BeforeEach
    void resetClock() {
        nanos.set(0);
    }

    private void advanceSeconds(double seconds) {
        nanos.addAndGet((long) (seconds * TimeUnit.SECONDS.toNanos(1)));
    }

    @Test
    void allowsBurstUpToCapacityThenRejects() {
        TokenBucket bucket = new TokenBucket(3, 1, nanos::get);

        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        assertFalse(bucket.tryConsume(), "4th request exceeds the burst capacity");
    }

    @Test
    void refillsAtConfiguredRate() {
        TokenBucket bucket = new TokenBucket(2, 2, nanos::get); // 2 tokens / second
        bucket.tryConsume();
        bucket.tryConsume();
        assertFalse(bucket.tryConsume());

        advanceSeconds(0.5); // +1 token
        assertTrue(bucket.tryConsume());
        assertFalse(bucket.tryConsume());
    }

    @Test
    void neverRefillsBeyondCapacity() {
        TokenBucket bucket = new TokenBucket(5, 100, nanos::get);
        advanceSeconds(60);
        assertEquals(5, bucket.remaining());
    }

    @Test
    void remainingReflectsConsumedTokens() {
        TokenBucket bucket = new TokenBucket(10, 0, nanos::get);
        bucket.tryConsume();
        bucket.tryConsume();
        assertEquals(8, bucket.remaining());
    }

    @Test
    void detectsIdleBucketsForEviction() {
        TokenBucket bucket = new TokenBucket(5, 1, nanos::get);
        bucket.tryConsume();
        assertFalse(bucket.isIdleFor(TimeUnit.SECONDS.toNanos(10)));

        advanceSeconds(11);
        assertTrue(bucket.isIdleFor(TimeUnit.SECONDS.toNanos(10)));
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(0, 1, nanos::get));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(1, -1, nanos::get));
    }
}
