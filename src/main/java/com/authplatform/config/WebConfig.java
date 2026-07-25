package com.authplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor loginRateLimitInterceptor;
    private final RateLimitInterceptor signupRateLimitInterceptor;

    public WebConfig(
            @Value("${app.ratelimit.login.capacity:10}") int loginCapacity,
            @Value("${app.ratelimit.login.refill-period-seconds:600}") int loginRefillPeriodSeconds,
            @Value("${app.ratelimit.signup.capacity:10}") int signupCapacity,
            @Value("${app.ratelimit.signup.refill-period-seconds:600}") int signupRefillPeriodSeconds) {
        // Separate bucket maps per endpoint so exhausting one endpoint's limit
        // does not block requests to the other.
        this.loginRateLimitInterceptor = new RateLimitInterceptor(
                loginCapacity, loginRefillPeriodSeconds, "Too many login attempts. Please try again later.");
        this.signupRateLimitInterceptor = new RateLimitInterceptor(
                signupCapacity, signupRefillPeriodSeconds, "Too many signup attempts. Please try again later.");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginRateLimitInterceptor)
                .addPathPatterns("/auth/login");
        registry.addInterceptor(signupRateLimitInterceptor)
                .addPathPatterns("/auth/signup");
    }
}
