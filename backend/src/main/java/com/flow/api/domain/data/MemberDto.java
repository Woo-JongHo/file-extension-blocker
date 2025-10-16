package com.flow.api.domain.data;

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
  private String password;
  private Long spaceId;
  private String spaceName; 
  private MemberRole role;

  @Override
  public Long getId() {
    return memberId;
  }

  @Override
  public void setId(Long id) {
    this.memberId = id;
  }
}

