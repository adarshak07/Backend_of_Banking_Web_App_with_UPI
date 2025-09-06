package com.alien.bank.management.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class RateLimitingConfig implements WebMvcConfigurer {

    @Bean
    public RateLimitingInterceptor rateLimitingInterceptor() {
        return new RateLimitingInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor())
                .addPathPatterns("/auth/login", "/auth/link/confirm", "/oauth2/authorization/google");
    }

    public static class RateLimitingInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
        private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Long> lastResetTimes = new ConcurrentHashMap<>();
        
        private static final int MAX_REQUESTS = 5; // 5 requests
        private static final long WINDOW_SIZE = 60 * 1000; // 1 minute

        @Override
        public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, 
                               jakarta.servlet.http.HttpServletResponse response, 
                               Object handler) throws Exception {
            String clientIp = getClientIpAddress(request);
            String key = clientIp + ":" + request.getRequestURI();
            
            long currentTime = System.currentTimeMillis();
            long lastReset = lastResetTimes.getOrDefault(key, 0L);
            
            // Reset counter if window has passed
            if (currentTime - lastReset > WINDOW_SIZE) {
                requestCounts.put(key, new AtomicInteger(0));
                lastResetTimes.put(key, currentTime);
            }
            
            AtomicInteger count = requestCounts.computeIfAbsent(key, k -> new AtomicInteger(0));
            int currentCount = count.incrementAndGet();
            
            if (currentCount > MAX_REQUESTS) {
                response.setStatus(429); // Too Many Requests
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
                response.setContentType("application/json");
                return false;
            }
            
            return true;
        }
        
        private String getClientIpAddress(jakarta.servlet.http.HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
    }
}
