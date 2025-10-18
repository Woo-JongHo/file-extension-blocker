package com.flow.api.domain.data;

import lombok.*;

/**
 * Space + Admin Member 생성 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceCreationResponse {
  
  private SpaceDto space;
  private MemberDto adminMember;
  private Integer fixedExtensionsCount;
}

