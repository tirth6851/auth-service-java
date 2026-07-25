package com.authplatform.config;

import com.authplatform.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Generic IP-keyed Bucket4j rate limiter for HTTP endpoints. Not a Spring
 * {@code @Component} because multiple independent instances (one per protected
 * endpoint, each with its own bucket map and limits) are wired up as separate
 * beans in {@link WebConfig} so that exhausting one endpoint's limit does not
 * affect another endpoint.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final int capacity;
    private final int refillPeriodSeconds;
    private final String errorMessage;
    // Instance field (not static) so DirtiesContext resets it between tests
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitInterceptor(int capacity, int refillPeriodSeconds, String errorMessage) {
        this.capacity = capacity;
        this.refillPeriodSeconds = refillPeriodSeconds;
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Keyed by remoteAddr (not X-Forwarded-For, which is attacker-controlled)
        String ip = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
            throw new RateLimitExceededException(errorMessage, retryAfterSeconds);
        }
        return true;
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofSeconds(refillPeriodSeconds))))
                .build();
    }
}
