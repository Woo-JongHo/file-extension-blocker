package com.flow.api.domain.data;

import com.woo.core.util.common.Identifiable;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedExtensionDto implements Identifiable {
  
  private Long blockedId;
  private Long spaceId;
  private String extension;
  private Boolean isFixed;
  private Boolean isBlocked;

  @Override
  public Long getId() { return blockedId; }

  @Override
  public void setId(Long id) { this.blockedId = id; }
}

