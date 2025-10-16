package com.flow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * File Extension Blocker 애플리케이션
 * JPA Auditing 활성화: created_at, updated_at, created_by, updated_by 자동 관리
 */
@SpringBootApplication
@EnableJpaAuditing
@ComponentScan(basePackages = {"com.flow", "com.woo.core"})
public class FileBlockerApplication {

  public static void main(String[] args) {
    SpringApplication.run(FileBlockerApplication.class, args);
  }
}

