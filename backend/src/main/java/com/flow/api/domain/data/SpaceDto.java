package com.flow.api.domain.data;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Space 관련 DTO
 */
public class SpaceDto {

  /**
   * 공간 생성 요청
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CreateRequest {
    private String spaceName;
    private String description;
  }

  /**
   * 공간 수정 요청
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class UpdateRequest {
    private String spaceName;
    private String description;
  }

  /**
   * 공간 응답
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Response {
    private Long spaceId;
    private String spaceName;
    private String description;
    private LocalDateTime createdAt;
  }
}

