package com.flow.api.domain.data;

import lombok.*;

/**
 * Space + Admin Member 동시 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceCreationRequest {
  
  private String spaceName;
  private String description;
  private String adminUsername;
  private String adminPassword;
}

