package com.woo.core.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모든 JPA Entity의 공통 필드를 제공하는 추상 기본 클래스.
 * 이 클래스를 상속받는 모든 Entity는 생성/수정 이력 추적과 논리적 삭제 기능을 자동으로 갖는다.
 * 
 * <p>이 클래스의 공통 필드는 다음과 같이 5개다:
 * <ul>
 *   <li>{@code createdAt} - 엔티티 생성 일시 (자동 설정, 수정 불가)</li>
 *   <li>{@code updatedAt} - 엔티티 최종 수정 일시 (자동 갱신)</li>
 *   <li>{@code createdBy} - 생성자 Member ID (자동 설정)</li>
 *   <li>{@code updatedBy} - 최종 수정자 Member ID (자동 갱신)</li>
 *   <li>{@code isDeleted} - 논리적 삭제 여부 (기본값 false)</li>
 * </ul>
 * 이들 필드는 엔티티의 생명주기 동안 자동으로 관리되며, 명시적으로 설정할 필요가 없다.
 * 
 * <p>사용하는 어노테이션은 다음과 같이 적용된다:
 * <ul>
 *   <li>{@code @CreatedDate}, {@code @LastModifiedDate} - 생성 시점과 수정 시점을 {@link LocalDateTime}으로 자동 기록</li>
 *   <li>{@code @CreatedBy}, {@code @LastModifiedBy} - 생성자와 수정자 ID를 {@link Long} 타입으로 자동 설정</li>
 *   <li>{@code @EntityListeners} - JPA Auditing 활성화 (엔티티 생명주기 이벤트 감지)</li>
 *   <li>{@code @MappedSuperclass} - 이 클래스는 테이블로 생성되지 않고, 하위 Entity 테이블에 필드만 매핑</li>
 *   <li>{@code @SuperBuilder} - 상속 계층에서 부모 필드를 포함한 빌더 패턴 지원</li>
 *   <li>{@code @NoArgsConstructor} - JPA 프록시 생성용 기본 생성자 제공</li>
 * </ul>
 * 
 * <p>Soft Delete 패턴:
 * {@code isDeleted} 필드는 물리적 삭제 대신 논리적 삭제를 구현하여 데이터 복구와 이력 추적을 가능하게 한다.
 * 
 * <p>Serializable 구현:
 * {@link Serializable}을 구현하여 HTTP Session, 분산 캐시(Redis), JPA 2차 캐시 등 직렬화가 필요한 시나리오를 지원한다.
 * 
 * <p>Implementation note:
 * 이 클래스의 인스턴스는 가변(mutable) 상태를 가지므로, 동기화 잠금으로 사용하지 않아야 한다.
 * 
 * @since 1.0
 */
@Getter
@Setter
@ToString
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {

  @CreatedDate
  @Column(updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @CreatedBy
  private Long createdBy;

  @LastModifiedBy
  private Long updatedBy;

  @Column(nullable = false)
  @Builder.Default
  private Boolean isDeleted = false;
}

