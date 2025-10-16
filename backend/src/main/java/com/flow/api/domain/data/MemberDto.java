package com.flow.api.domain.data;

import com.flow.api.domain.Member.MemberRole;
import lombok.*;

/**
 * Member 관련 DTO
 */
public class MemberDto {

  /**
   * 회원 가입 요청
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CreateRequest {
    private String username;
    private String password;
    private Long spaceId;        // 소속 공간
    private MemberRole role;     // 권한 (ADMIN/MEMBER)
  }

  /**
   * 회원 정보 수정 요청
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class UpdateRequest {
    private String password;
    private Long spaceId;        // 공간 변경 가능
    private MemberRole role;     // 권한 변경 가능
  }

  /**
   * 회원 응답
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Response {
    private Long memberId;
    private String username;
    private Long spaceId;
    private String spaceName;    // Space 조인
    private MemberRole role;
  }
}

