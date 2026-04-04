package com.example.Tournament.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private SessionInterceptor sessionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/login", "/signup", "/auth/", "/",
                    "/matches", "/match/**",
                    "/commentry/**",
                    "/teams", "/teams/**",        // ✅ /teams/{teamId} ab public
                    "/players", "/players/**",    // ✅ players detail bhi public (future use)
                    "/tournaments", "/tournament/**",
                    "/api/public/**",
                    "/api/players/**",
                    "/api/score/**",
                    "/error",
                    "/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.ico", "/**/*.svg"
                );
    }
}