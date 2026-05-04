package com.ac.pettracker.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenBucketTest {

  @Test
  void consumeTokenWhenAvailable() {
    TokenBucket bucket = new TokenBucket(3, 1000); // 3 tokens, refill 1 token per 1000ms

    // Should succeed immediately (no wait)
    assertThat(bucket.tryConsume()).isEqualTo(0);
    assertThat(bucket.tryConsume()).isEqualTo(0);
    assertThat(bucket.tryConsume()).isEqualTo(0);

    // 4th attempt should require wait
    long waitTimeMs = bucket.tryConsume();
    assertThat(waitTimeMs).isGreaterThan(0).isLessThanOrEqualTo(1000);
  }

  @Test
  void refillTokenAfterDelay() throws InterruptedException {
    TokenBucket bucket = new TokenBucket(1, 100); // 1 token capacity, refill every 100ms

    // Consume the single token
    assertThat(bucket.tryConsume()).isEqualTo(0);

    // Next attempt should fail (need to wait ~100ms)
    long waitTimeMs = bucket.tryConsume();
    assertThat(waitTimeMs).isGreaterThan(0).isLessThanOrEqualTo(100);

    // Wait for refill
    Thread.sleep(150); // Wait for token to refill

    // Now should succeed
    assertThat(bucket.tryConsume()).isEqualTo(0);
  }

  @Test
  void respectCapacity() throws InterruptedException {
    TokenBucket bucket = new TokenBucket(2, 500); // Shorter refill for test

    // Consume all tokens
    bucket.tryConsume();
    bucket.tryConsume();

    // Should have 0 tokens
    assertThat(bucket.getAvailableTokens()).isZero();

    // Wait 1.5 seconds with 500ms refill = 3 refills
    Thread.sleep(1500);

    // Should still be capped at capacity (2), not 3 or 6
    assertThat(bucket.getAvailableTokens()).isEqualTo(2);
  }

  @Test
  void returnsZeroWaitWhenBucketHasTokens() {
    TokenBucket bucket = new TokenBucket(10, 500);

    for (int i = 0; i < 10; i++) {
      assertThat(bucket.tryConsume()).isZero(); // All should be immediate (no wait)
    }
  }

  @Test
  void simulatesRateLimitedApiRetries() throws InterruptedException {
    // Scenario: API allows 3 requests per second
    TokenBucket bucket = new TokenBucket(3, 1000); // 3 tokens, 1 refill per second

    // Simulate 3 fast retries (all immediate - within rate limit)
    System.out.println("=== Attempt 1 ===");
    assertThat(bucket.tryConsume()).isZero(); // immediate
    System.out.println("✓ Success (no wait)\n");

    System.out.println("=== Attempt 2 ===");
    assertThat(bucket.tryConsume()).isZero(); // immediate
    System.out.println("✓ Success (no wait)\n");

    System.out.println("=== Attempt 3 ===");
    assertThat(bucket.tryConsume()).isZero(); // immediate
    System.out.println("✓ Success (no wait)\n");

    // 4th retry exceeds rate limit
    System.out.println("=== Attempt 4 ===");
    long waitMs = bucket.tryConsume();
    System.out.println("! Rate limit hit. Must wait " + waitMs + "ms");
    assertThat(waitMs).isGreaterThan(0);

    // Wait for refill
    System.out.println("\nWaiting " + waitMs + "ms for token refill...");
    Thread.sleep(waitMs + 100); // Add 100ms buffer

    // Now 5th attempt should work
    System.out.println("=== Attempt 5 (after wait) ===");
    assertThat(bucket.tryConsume()).isZero(); // immediate
    System.out.println("✓ Success (token refilled)\n");
  }
}
