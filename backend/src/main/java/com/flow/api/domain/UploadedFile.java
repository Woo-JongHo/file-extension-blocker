package com.flow.api.domain;

import com.woo.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Entity
@Table(name = "uploaded_file")
@Getter
@Setter
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFile extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long fileId;

  @Column(nullable = false)
  private Long spaceId;

  @Column(length = 255, nullable = false)
  private String originalName;

  @Column(length = 255, nullable = false, unique = true)
  private String storedName;

  @Column(length = 20, nullable = false)
  private String extension;

  @Column(nullable = false)
  private Long fileSize;

  @Column(length = 100)
  private String mimeType;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String filePath;
}

