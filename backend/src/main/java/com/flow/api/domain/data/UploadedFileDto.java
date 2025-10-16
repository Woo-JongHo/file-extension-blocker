package com.flow.api.domain.data;

import lombok.*;
import java.time.LocalDateTime;

/**
 * UploadedFile 관련 DTO
 */
public class UploadedFileDto {

  /**
   * 파일 업로드 요청 (Multipart)
   * 실제로는 MultipartFile을 Controller에서 받음
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CreateRequest {
    private Long spaceId;
    // MultipartFile file은 Controller에서 처리
  }

  /**
   * 파일 응답
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Response {
    private Long fileId;
    private Long spaceId;
    private String originalName;
    private String storedName;
    private String extension;
    private Long fileSize;
    private String mimeType;
    private String filePath;
    private LocalDateTime createdAt;
    private String uploaderName;  // created_by Member 조인
  }

  /**
   * 파일 목록 응답
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ListResponse {
    private Long totalCount;
    private java.util.List<Response> files;
  }
}

