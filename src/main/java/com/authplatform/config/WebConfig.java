package com.authplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginRateLimitInterceptor loginRateLimitInterceptor;

    public WebConfig(LoginRateLimitInterceptor loginRateLimitInterceptor) {
        this.loginRateLimitInterceptor = loginRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginRateLimitInterceptor)
                .addPathPatterns("/auth/login");
    }
}
