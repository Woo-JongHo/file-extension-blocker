package com.flow.api.domain.data;

import com.woo.core.util.common.Identifiable;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Space DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceDto implements Identifiable {
  
  private Long spaceId;
  private String spaceName;
  private String description;
  private LocalDateTime createdAt;

  @Override
  public Long getId() {
    return spaceId;
  }

  @Override
  public void setId(Long id) {
    this.spaceId = id;
  }
}

