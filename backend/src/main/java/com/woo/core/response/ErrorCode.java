package com.woo.core.response;

import lombok.Getter;

/**
 * API 에러 코드와 메시지를 정의하는 Enum 클래스
 *
 * <p>각 에러는 에러 코드, 메시지, HTTP 상태 코드를 포함한다.
 * BaseResponse와 함께 사용하여 일관된 에러 응답을 제공한다.
 *
 * @see BaseResponse#errorResponse(ErrorCode)
 * @since 1.0
 */
@Getter
public enum ErrorCode {
  
  // ═══════════ Space 관련 에러 (400, 404) ═══════════
  SPACE_NOT_FOUND("SPACE_NOT_FOUND", "공간을 찾을 수 없습니다.", 404),
  SPACE_CREATE_FAILED("SPACE_CREATE_FAILED", "공간 생성에 실패했습니다.", 400),
  SPACE_UPDATE_FAILED("SPACE_UPDATE_FAILED", "공간 수정에 실패했습니다.", 400),
  SPACE_DELETE_FAILED("SPACE_DELETE_FAILED", "공간 삭제에 실패했습니다.", 400),
  
  // ═══════════ Member 관련 에러 (400, 404) ═══════════
  MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다.", 404),
  MEMBER_CREATE_FAILED("MEMBER_CREATE_FAILED", "회원 생성에 실패했습니다.", 400),
  MEMBER_UPDATE_FAILED("MEMBER_UPDATE_FAILED", "회원 수정에 실패했습니다.", 400),
  MEMBER_DELETE_FAILED("MEMBER_DELETE_FAILED", "회원 삭제에 실패했습니다.", 400),
  
  // ═══════════ BlockedExtension 관련 에러 (400, 404, 409) ═══════════
  BLOCKED_EXTENSION_NOT_FOUND("BLOCKED_EXTENSION_NOT_FOUND", "차단된 확장자를 찾을 수 없습니다.", 404),
  BLOCKED_EXTENSION_ALREADY_EXISTS("BLOCKED_EXTENSION_ALREADY_EXISTS", "이미 차단된 확장자입니다.", 409),
  BLOCKED_EXTENSION_CREATE_FAILED("BLOCKED_EXTENSION_CREATE_FAILED", "확장자 차단에 실패했습니다.", 400),
  BLOCKED_EXTENSION_DELETE_FAILED("BLOCKED_EXTENSION_DELETE_FAILED", "확장자 차단 해제에 실패했습니다.", 400),
  BLOCKED_EXTENSION_STATUS_UPDATE_FAILED("BLOCKED_EXTENSION_STATUS_UPDATE_FAILED", "확장자 상태 변경에 실패했습니다.", 400),
  FIXED_EXTENSION_CANNOT_DELETE("FIXED_EXTENSION_CANNOT_DELETE", "고정 확장자는 삭제할 수 없습니다.", 400),
  CUSTOM_EXTENSION_LIMIT_EXCEEDED("CUSTOM_EXTENSION_LIMIT_EXCEEDED", "커스텀 확장자는 최대 200개까지만 등록 가능합니다.", 400),
  
  // ═══════════ UploadedFile 관련 에러 (400, 403, 413) ═══════════
  FILE_NOT_FOUND("FILE_NOT_FOUND", "파일을 찾을 수 없습니다.", 404),
  FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "파일 업로드에 실패했습니다.", 400),
  FILE_DELETE_FAILED("FILE_DELETE_FAILED", "파일 삭제에 실패했습니다.", 400),
  FILE_EXTENSION_BLOCKED("FILE_EXTENSION_BLOCKED", "차단된 확장자의 파일은 업로드할 수 없습니다.", 403),
  FILE_SIZE_EXCEEDED("FILE_SIZE_EXCEEDED", "파일 크기가 제한을 초과했습니다.", 413),
  INVALID_FILE_EXTENSION("INVALID_FILE_EXTENSION", "유효하지 않은 파일 확장자입니다.", 400),
  
  // ═══════════ 인증 관련 에러 (401) ═══════════
  INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다.", 401),
  INVALID_REFRESH_TOKEN("INVALID_REFRESH_TOKEN", "RefreshToken이 유효하지 않거나 없습니다.", 401),
  LOGIN_FAILED("LOGIN_FAILED", "로그인에 실패했습니다.", 401),
  
  // ═══════════ 권한 관련 에러 (403) ═══════════
  ACCESS_DENIED("ACCESS_DENIED", "접근 권한이 없습니다.", 403),
  
  // ═══════════ 공통 에러 (400, 404, 500) ═══════════
  INVALID_REQUEST("INVALID_REQUEST", "잘못된 요청입니다.", 400),
  ENTITY_NOT_FOUND("ENTITY_NOT_FOUND", "요청한 데이터를 찾을 수 없습니다.", 404),
  INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.", 500);

  private final String code;
  private final String message;
  private final int httpStatus;

  ErrorCode(String code, String message, int httpStatus) {
    this.code = code;
    this.message = message;
    this.httpStatus = httpStatus;
  }
}

