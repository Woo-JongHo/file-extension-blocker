package com.flow.api.domain.data;

import lombok.*;

/**
 * BlockedExtension 관련 DTO
 */
public class BlockedExtensionDto {

  /**
   * 커스텀 확장자 추가 요청
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CreateRequest {
    private Long spaceId;
    private String extension;  // 최대 20자
  }

  /**
   * 고정 확장자 체크/언체크 요청
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ToggleRequest {
    private Long spaceId;
    private String extension;
    private Boolean isBlocked;  // true: 차단 활성화, false: 차단 비활성화
  }

  /**
   * 차단 확장자 응답
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Response {
    private Long blockedId;
    private Long spaceId;
    private String extension;
    private Boolean isFixed;    // 고정 확장자 여부
    private Boolean isBlocked;  // 차단 활성화 여부 (is_deleted의 반대)
  }

  /**
   * 공간별 차단 확장자 목록 응답
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ListResponse {
    private java.util.List<Response> fixedExtensions;    // 고정 확장자 (6개)
    private java.util.List<Response> customExtensions;   // 커스텀 확장자 (최대 200개)
  }
}

