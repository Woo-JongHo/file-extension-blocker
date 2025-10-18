package com.woo.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS(Cross-Origin Resource Sharing) 설정
 * 
 * <p>프론트엔드(Vercel, localhost)에서 백엔드 API 호출을 허용한다.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Value("${cors.allowed-origins:http://localhost:3000,https://*.vercel.app}")
  private String allowedOrigins;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String[] origins = allowedOrigins.split(",");
    String[] patterns = new String[origins.length + 1];
    System.arraycopy(origins, 0, patterns, 0, origins.length);
    patterns[origins.length] = "https://*.vercel.app";
    
    registry.addMapping("/api/**")
        .allowedOriginPatterns(patterns)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .exposedHeaders("Content-Disposition")
        .maxAge(3600);
  }
}

