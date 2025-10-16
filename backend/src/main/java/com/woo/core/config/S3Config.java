package com.woo.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

// import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Configuration
public class S3Config {

  @Value("${aws.s3.access-key:}")
  private String accessKey;

  @Value("${aws.s3.secret-key:}")
  private String secretKey;

  @Value("${aws.s3.region:ap-northeast-2}")
  private String region;

  @Value("${aws.s3.bucket:default-bucket}")
  private String bucketName;

  // S3Client는 AWS 키가 설정되어 있을 때만 활성화
  // TODO: AWS 자격증명 설정 후 주석 해제
  /*
  @Bean
  @ConditionalOnProperty(name = "aws.s3.access-key")
  public S3Client s3Client() {
    log.info("S3Client Bean 생성 - Region: {}, Bucket: {}", region, bucketName);
    return S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .httpClient(UrlConnectionHttpClient.builder().build())
        .build();
  }
  */

  // S3Presigner는 별도 의존성이 필요하므로 일단 주석 처리
  // @Bean
  // public S3Presigner s3Presigner() {
  //     return S3Presigner.builder()
  //             .region(Region.of(region))
  //             .credentialsProvider(StaticCredentialsProvider.create(
  //                     AwsBasicCredentials.create(accessKey, secretKey)))
  //             .build();
  // }
}

