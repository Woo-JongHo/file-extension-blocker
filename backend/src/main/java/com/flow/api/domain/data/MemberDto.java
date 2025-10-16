package com.flow.api.domain.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flow.api.domain.Member.MemberRole;
import com.woo.core.util.common.Identifiable;
import lombok.*;

/**
 * Member DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDto implements Identifiable {
  
  private Long memberId;
  private String username;
  
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)  // 요청에서만 사용, 응답에서는 제외
  private String password;
  
  private Long spaceId;
  private String spaceName; 
  private MemberRole role;

  @Override
  @JsonIgnore
  public Long getId() {
    return memberId;
  }

  @Override
  @JsonIgnore
  public void setId(Long id) {
    this.memberId = id;
  }
}

