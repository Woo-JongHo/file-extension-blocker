package com.woo.core.controller;

import com.woo.core.response.BaseResponse;
import com.woo.core.response.SuccessCode;
import com.woo.core.service.BaseService;
import com.woo.core.util.common.Identifiable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 공통 CRUD 기능을 제공하는 추상 컨트롤러 클래스
 *
 * <p>제네릭을 활용하여 다양한 도메인(Entity/DTO)에 대해 재사용 가능하며,
 * 공통 로직(create, update, delete, soft delete, get, search)을 제공하여 중복 코드를 제거
 *
 * <p>{@code E}는 JPA Entity 클래스, {@code D}는 Identifiable을 구현한 DTO 클래스
 * ModelMapper를 활용하여 Entity ↔ DTO 간 변환을 처리
 *
 * <p>기본 제공 엔드포인트:
 * <ul>
 *   <li>POST /create - 엔티티 생성</li>
 *   <li>PATCH /update - 엔티티 수정</li>
 *   <li>DELETE /{id} - 엔티티 Hard Delete (물리적 삭제)</li>
 *   <li>PATCH /soft-delete/{id} - 엔티티 Soft Delete (논리적 삭제)</li>
 *   <li>GET /{id} - ID로 단건 조회</li>
 *   <li>GET /search - 검색 조건 + 페이징 조회</li>
 * </ul>
 *
 * <p>상속 시 필수 구현 메서드:
 * <ul>
 *   <li>{@link #getDtoClass()} - DTO 클래스 타입 반환</li>
 *   <li>{@link #getEntityClass()} - Entity 클래스 타입 반환</li>
 * </ul>
 *
 * <p>DTO 요구사항:
 * <ul>
 *   <li>{@link Identifiable} 인터페이스 구현 필수 (getId() 메서드 제공)</li>
 * </ul>
 *
 * @param <E> Entity 타입
 * @param <D> DTO 타입 (Identifiable 구현 필수)
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseController<E, D extends Identifiable> {

  protected final BaseService<E> service;
  protected final ModelMapper modelMapper;

  @PostMapping("/create")
  public ResponseEntity<BaseResponse<D>> create(@RequestBody D dto) {
    E entity = toEntity(dto);
    E saved = service.create(entity);
    D savedDto = toDto(saved);
    return successResponse(savedDto);
  }

  @PatchMapping("/update")
  public ResponseEntity<BaseResponse<D>> update(@RequestBody D dto) {
    E existing = service.findById(dto.getId());
    modelMapper.map(dto, existing);
    E updated = service.update(existing);
    D updatedDto = toDto(updated);
    return successResponse(updatedDto);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<String>> delete(@PathVariable Long id) {
    service.delete(id);
    return successResponse(SuccessCode.DELETE_SUCCESS.getMessage());
  }

  @PatchMapping("/soft-delete/{id}")
  public ResponseEntity<BaseResponse<String>> isDelete(@PathVariable Long id) {
    E entity = service.findById(id);
    service.softDelete(entity);
    return successResponse("Soft delete completed successfully");
  }

  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<D>> get(@PathVariable Long id) {
    E entity = service.findById(id);
    if (entity == null) {
      return errorResponse("NOT_FOUND", "Not found with id=" + id);
    }
    return successResponse(toDto(entity));
  }

  @GetMapping("/search")
  public ResponseEntity<BaseResponse<Page<D>>> search(
      @RequestParam Map<String, Object> filters, Pageable pageable) {
    Page<E> result = service.search(filters, pageable);
    Page<D> dtoResult = toDtoPage(result);
    return successResponse(dtoResult);
  }

  protected D toDto(E entity) {
    return modelMapper.map(entity, getDtoClass());
  }

  protected E toEntity(D dto) {
    return modelMapper.map(dto, getEntityClass());
  }

  protected List<D> toDtoList(List<E> entities) {
    List<D> dtos = new ArrayList<>();
    for (E entity : entities) {
      dtos.add(toDto(entity));
    }
    return dtos;
  }

  protected Page<D> toDtoPage(Page<E> entityPage) {
    return entityPage.map(this::toDto);
  }

  protected abstract Class<D> getDtoClass();

  protected abstract Class<E> getEntityClass();

  protected <T> ResponseEntity<BaseResponse<T>> successResponse(T data) {
    return BaseResponse.successResponse(data);
  }

  protected <T> ResponseEntity<BaseResponse<T>> errorResponse(String errorCode, String errorDetail) {
    return BaseResponse.errorResponse(400, errorCode, errorDetail);
  }
}

