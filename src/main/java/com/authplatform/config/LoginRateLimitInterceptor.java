package com.authplatform.config;

import com.authplatform.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class LoginRateLimitInterceptor implements HandlerInterceptor {

    private final int capacity;
    private final int refillPeriodSeconds;
    // Instance field (not static) so DirtiesContext resets it between tests
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public LoginRateLimitInterceptor(
            @Value("${app.ratelimit.login.capacity:10}") int capacity,
            @Value("${app.ratelimit.login.refill-period-seconds:600}") int refillPeriodSeconds) {
        this.capacity = capacity;
        this.refillPeriodSeconds = refillPeriodSeconds;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Keyed by remoteAddr (not X-Forwarded-For, which is attacker-controlled)
        String ip = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
            throw new RateLimitExceededException(retryAfterSeconds);
        }
        return true;
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofSeconds(refillPeriodSeconds))))
                .build();
    }
}
