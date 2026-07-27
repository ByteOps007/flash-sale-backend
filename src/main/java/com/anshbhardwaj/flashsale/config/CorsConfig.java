package com.anshbhardwaj.flashsale.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Without this, the browser silently blocks every fetch() call
    // from the Next.js frontend (localhost:3000) to this API
    // (localhost:8080), since they're different origins. This shows up
    // as a generic "network error" in the frontend with no obvious
    // clue that CORS is the cause - so it's easy to mistake for the
    // backend simply not running.
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
