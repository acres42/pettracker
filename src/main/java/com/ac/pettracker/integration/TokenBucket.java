package com.ac.pettracker.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Token Bucket Rate Limiter
 *
 * <p>This implements the Token Bucket algorithm for rate limiting:
 *
 * <p>- Bucket has a capacity (e.g., 3 tokens) - Tokens refill at a fixed rate (e.g., 1 token per
 * 1000ms) - Each operation consumes 1 token - If no tokens available, caller waits for refill
 *
 * <p>Interview talking points: - Used by AWS, Google, Netflix for API rate limiting - Allows
 * "bursty" traffic up to capacity - Fairness: everyone gets same refill rate - Better than
 * exponential backoff for APIs with known rate limits
 */
public class TokenBucket {

  private static final Logger logger = LoggerFactory.getLogger(TokenBucket.class);

  private final int capacity;
  private final long refillRateMs; // milliseconds between token refills
  private double tokens; // using double to handle fractional tokens
  private long lastRefillTime;

  /**
   * Create a token bucket
   *
   * @param capacity max tokens in bucket (e.g., 3)
   * @param refillRateMs milliseconds between refills (e.g., 1000 = 1 token per second)
   */
  public TokenBucket(int capacity, long refillRateMs) {
    this.capacity = capacity;
    this.refillRateMs = refillRateMs;
    this.tokens = capacity; // start with full bucket
    this.lastRefillTime = System.currentTimeMillis();
  }

  /**
   * Try to consume a token immediately. Returns how long to wait if bucket is empty.
   *
   * @return 0 if token was available; otherwise milliseconds to wait before retry
   */
  public synchronized long tryConsume() {
    refill();

    if (tokens >= 1.0) {
      tokens -= 1.0;
      logger.debug("Token consumed. Remaining tokens: {}", (int) tokens);
      return 0; // Success - no wait needed
    }

    // Calculate wait time until next token available
    long waitMs = calculateWaitTime();
    logger.warn("Token bucket empty. Must wait {}ms before next attempt", waitMs);
    return waitMs;
  }

  /** Refill tokens based on elapsed time since last refill */
  private void refill() {
    long now = System.currentTimeMillis();
    long elapsedMs = now - lastRefillTime;

    if (elapsedMs >= refillRateMs) {
      // Calculate how many complete refill intervals have passed
      long intervalsElapsed = elapsedMs / refillRateMs;
      tokens = Math.min(capacity, tokens + intervalsElapsed);
      lastRefillTime = now - (elapsedMs % refillRateMs);
      logger.debug("Bucket refilled. Current tokens: {}/{}", (int) tokens, capacity);
    }
  }

  private long calculateWaitTime() {
    // How long until we have 1 token?
    // tokens needed = 1 - current_tokens (fractional)
    // time = tokens_needed * refillRateMs
    double tokensNeeded = 1.0 - tokens;
    long waitMs = (long) Math.ceil(tokensNeeded * refillRateMs);
    return Math.max(1, waitMs); // at least 1ms
  }

  public int getCapacity() {
    return capacity;
  }

  public long getRefillRateMs() {
    return refillRateMs;
  }

  public synchronized int getAvailableTokens() {
    refill();
    return (int) tokens;
  }
}
