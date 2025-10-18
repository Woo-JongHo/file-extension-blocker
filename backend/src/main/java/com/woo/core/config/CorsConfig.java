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
    registry.addMapping("/api/**")
        .allowedOriginPatterns(allowedOrigins.split(","))
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
  }
}

