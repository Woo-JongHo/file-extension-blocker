package com.flow.api.service.impl;

import com.flow.api.domain.BlockedExtension;
import com.flow.api.domain.Member;
import com.flow.api.domain.UploadedFile;
import com.flow.api.domain.data.UploadedFileDto;
import com.flow.api.repository.MemberRepository;
import com.flow.api.repository.UploadedFileRepository;
import com.flow.api.service.BlockedExtensionService;
import com.flow.api.service.UploadedFileService;
import com.flow.util.fileDefence.ZipValidator;
import com.woo.core.service.BaseServiceImpl;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UploadedFileServiceImpl extends BaseServiceImpl<UploadedFile> implements UploadedFileService {

  private final UploadedFileRepository uploadedFileRepository;
  private final BlockedExtensionService blockedExtensionService;
  private final MemberRepository memberRepository;
  private final Tika tika;

  //10MB TJFWJD
  @Value("${file.upload.max-size:10485760}")
  private long maxFileSize;

  // 개인 pc 디렉토리입니다
  @Value("${file.upload.directory:./uploads}")
  private String uploadDirectory;

  public UploadedFileServiceImpl(
      UploadedFileRepository uploadedFileRepository,
      BlockedExtensionService blockedExtensionService,
      MemberRepository memberRepository) {
    super(uploadedFileRepository);
    this.uploadedFileRepository = uploadedFileRepository;
    this.blockedExtensionService = blockedExtensionService;
    this.memberRepository = memberRepository;
    this.tika = new Tika();
  }

  @Override
  @Transactional(readOnly = true)
  public List<UploadedFile> getFilesBySpace(Long spaceId) {
    return uploadedFileRepository.findBySpaceIdAndIsDeletedFalse(spaceId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UploadedFileDto> getFilesBySpaceWithUploader(Long spaceId) {
    List<UploadedFile> files = uploadedFileRepository.findBySpaceIdAndIsDeletedFalse(spaceId);
    
    return files.stream()
        .map(file -> {
          String uploaderName = "알 수 없음";
          
          if (file.getCreatedBy() != null) {
            Member member = memberRepository.findById(file.getCreatedBy()).orElse(null);
            if (member != null) {
              uploaderName = member.getUsername();
            }
          }
          
          return UploadedFileDto.builder()
              .fileId(file.getFileId())
              .spaceId(file.getSpaceId())
              .originalName(file.getOriginalName())
              .storedName(file.getStoredName())
              .extension(file.getExtension())
              .fileSize(file.getFileSize())
              .mimeType(file.getMimeType())
              .filePath(file.getFilePath())
              .createdAt(file.getCreatedAt())
              .uploaderName(uploaderName)
              .build();
        })
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<UploadedFile> getFilesByUploader(Long memberId) {
    return uploadedFileRepository.findByCreatedByAndIsDeletedFalse(memberId);
  }

  @Override
  @Transactional(readOnly = true)
  public UploadedFile getFileById(Long fileId) {
    return uploadedFileRepository.findById(fileId)
        .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다: " + fileId));
  }

  @Override
  @Transactional(readOnly = true)
  public Long countFilesBySpace(Long spaceId) {
    return uploadedFileRepository.countBySpaceIdAndIsDeletedFalse(spaceId);
  }


  /**
   * 파일 업로드 (방어 전략 4단계 적용)
   *
   * <p>strategy.md의 방어 전략을 단계별로 적용하여 파일을 검증 후 업로드
   *
   * <p>방어 전략:
   * <ul>
   *   <li>1,2단계: FileValidator (확장자 + 매직 넘버) 구현 완료</li>
   *   <li>3단계: ZipValidator (압축 파일 내부 검증) 구현 완료</li>
   *   <li>4단계: chmod 644 (실행 권한 제거) 구현 완료</li>
   * </ul>
   *
   * <p>현재 적용 단계:
   * <ul>
   *   <li>1단계: 확장자 Blacklist로 텍스트 스크립트(.bat, .cmd, .sh 등) 차단</li>
   *   <li>2단계: 매직 넘버로 바이너리 실행 파일(.exe, .elf) 차단</li>
   *   <li>3단계: 압축 파일 내부 재귀 검증, Zip Bomb 차단</li>
   *   <li>4단계: chmod 644로 실행 권한 제거 (최종 방어선)</li>
   * </ul>
   */
  @Override
  public UploadedFile uploadFile(Long spaceId, MultipartFile file) {
    String extension = validate1stDefense(spaceId, file);
    validate2ndDefense(spaceId, file, extension);
    validate3rdDefense(spaceId, file, extension);
    
    UploadedFile result = validate4thDefense(spaceId, file, extension);
    
    return result;
  }

  /**
   * 확장자 차단 여부 확인
   * 
   * <p>검증 흐름: DB 조회 → List를 Set으로 변환 → 차단 여부 확인
   * 
   * <p>시간 복잡도: O(n)
   * <ul>
   *   <li>매 호출마다 Set 변환: O(n)</li>
   *   <li>Set 검증: O(1)</li>
   *   <li>전체: O(n)</li>
   * </ul>
   * 
   * <p>최적화:
   * <ul>
   *   <li>캐싱으로 O(1) 가능하지만, 차단 확장자 최대 206개(고정 6 + 커스텀 200)로 성능 영향 미미하여 제외</li>
   * </ul>
   */
  @Override
  @Transactional(readOnly = true)
  public boolean isExtensionBlocked(Long spaceId, String extension) {
    if (extension == null || extension.isEmpty()) {
      return true;
    }
    
    List<BlockedExtension> blockedExtensions = blockedExtensionService.getBlockedExtensions(spaceId);
    
    Set<String> blockedSet = new HashSet<>();
    for (BlockedExtension be : blockedExtensions) {
      blockedSet.add(be.getExtension().toLowerCase());
    }
    
    String normalizedExtension = extension.toLowerCase().trim();
    return blockedSet.contains(normalizedExtension);
  }

  // ═══════════════════════════════════════════════════════════
  // Private 검증 메서드들
  // ═══════════════════════════════════════════════════════════


  /**
   * 2단계: Apache Tika 매직 넘버 검증 (상세: docs/strategy.md)
   *
   * <p>1단계를 통과한 파일의 실제 내용을 매직 넘버(파일 시그니처)로 검증한다.
   * <p>1단계의 한계(확장자만 변경한 공격)를 극복한다.
   *
   * <p>방어 시나리오:
   * <pre>
   * 공격: malware.exe -> malware.jpg (확장자만 변경)
   * 1단계: 확장자 .jpg -> 통과 (Blacklist에 없음)
   * 2단계: Tika 매직 넘버 분석 -> "4D 5A" (PE 실행 파일) 감지 -> 차단됨
   * </pre>
   *
   * <p>한계:
   * <pre>
   * 한계: 텍스트 기반 스크립트(.bat, .cmd, .sh)는 매직 넘버가 없어 감지 불가
   * 보완: 1단계 확장자 Blacklist가 주 방어선 역할
   * </pre>
   *
   * @param spaceId 공간 ID
   * @param file 업로드 파일
   * @param extension 1단계에서 검증된 확장자
   * @throws IllegalArgumentException 실행 파일 감지 시
   * @throws RuntimeException Tika 분석 실패 시
   */
  private void validate2ndDefense(Long spaceId, MultipartFile file, String extension) {
    try (InputStream inputStream = file.getInputStream()) {
      String detectedMimeType = tika.detect(inputStream, file.getOriginalFilename());

      // 2-1. 실행 파일 감지 (바이너리 실행 파일 차단)
      if (isExecutableFile(detectedMimeType)) {
        throw new IllegalArgumentException(
            String.format("실행 파일은 업로드할 수 없습니다. 감지된 타입: %s", detectedMimeType));
      }

      // 2-2. 확장자 위장 검증 (감지된 MIME 타입이 차단 대상인지 확인)
      validateDisguisedExtension(spaceId, extension, detectedMimeType, file.getOriginalFilename());

    } catch (IOException e) {
      throw new RuntimeException("파일 형식 검증 중 오류가 발생했습니다.", e);
    }
  }

  /**
   * 1단계: 확장자 Blacklist 검증 (상세: docs/strategy.md)
   *
   * <p>파일명의 확장자를 추출하여 Blacklist와 비교하여 차단한다.
   *
   * <p>방어 시나리오:
   * <pre>
   * 공격: virus.bat 업로드
   * 검증: 확장자 .bat 추출 -> Blacklist 조회 -> 차단됨
   * </pre>
   *
   * <p>한계:
   * <pre>
   * 공격: malware.exe -> malware.jpg (확장자만 변경)
   * 검증: 확장자 .jpg -> Blacklist에 없음 -> 통과
   * 한계: 파일명만 보고 판단하므로 실제 내용은 검증 불가
   * </pre>
   *
   * @param spaceId 공간 ID
   * @param file 업로드 파일
   * @return 추출된 확장자
   * @throws IllegalArgumentException 차단된 확장자인 경우
   */
  private String validate1stDefense(Long spaceId, MultipartFile file) {
    // 파일 기본 검증
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("파일이 비어있습니다.");
    }
    
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.isEmpty()) {
      throw new IllegalArgumentException("파일명이 없습니다.");
    }
    
    // 확장자 추출
    int lastDotIndex = originalFilename.lastIndexOf('.');
    String extension = (lastDotIndex == -1) ? "" : originalFilename.substring(lastDotIndex + 1).toLowerCase();
    
    // 확장자 Blacklist 검증
    if (isExtensionBlocked(spaceId, extension)) {
      throw new IllegalArgumentException(
          String.format("'%s' 확장자는 차단되어 업로드할 수 없습니다.", extension));
    }
    
    // 파일 크기 검증
    if (file.getSize() > maxFileSize) {
      throw new IllegalArgumentException(
          String.format("파일 크기가 너무 큽니다. 최대 크기: %dMB", maxFileSize / (1024 * 1024)));
    }
    
    return extension;
  }

  /**
   * 실행 파일 여부 확인
   *
   * <p>Tika가 감지한 MIME 타입이 실행 파일인지 검증한다.
   *
   * <p>차단 대상:
   * <ul>
   *   <li>application/x-msdownload: Windows PE 실행 파일 (.exe, .dll)</li>
   *   <li>application/x-executable: Unix/Linux ELF 실행 파일</li>
   *   <li>application/x-dosexec: DOS 실행 파일</li>
   *   <li>application/x-mach-binary: macOS Mach-O 실행 파일</li>
   *   <li>application/x-sharedlib: 공유 라이브러리 (.so, .dylib)</li>
   * </ul>
   *
   * @param mimeType Tika가 감지한 MIME 타입
   * @return 실행 파일이면 true
   */
  private boolean isExecutableFile(String mimeType) {
    if (mimeType == null) {
      return false;
    }

    // 실행 파일 MIME 타입 목록
    Set<String> executableMimeTypes = Set.of(
        "application/x-msdownload",      // Windows .exe, .dll
        "application/x-executable",      // Linux ELF
        "application/x-dosexec",         // DOS executable
        "application/x-mach-binary",     // macOS Mach-O
        "application/x-sharedlib",       // Shared libraries (.so, .dylib)
        "application/vnd.microsoft.portable-executable"  // PE format
    );

    return executableMimeTypes.contains(mimeType.toLowerCase());
  }

  /**
   * 확장자 위장 검증 (2-2단계)
   *
   * <p>감지된 MIME 타입이 해당 Space의 차단 확장자에 해당하는지 동적으로 확인합니다.
   * <p>예: .jpg로 위장했지만 실제 MIME이 application/x-sh (.sh)인 경우
   *      → .sh가 해당 Space의 차단 목록에 있으면 차단!
   *
   * <p>검증 흐름:
   * <ol>
   *   <li>해당 Space의 차단 확장자 목록 조회 (isDeleted = false)</li>
   *   <li>각 차단 확장자에 대한 예상 MIME 타입 계산</li>
   *   <li>감지된 MIME 타입이 차단 확장자의 MIME과 일치하는지 확인</li>
   *   <li>일치하면 차단 (위험한 파일을 위장한 것)</li>
   * </ol>
   *
   * <p>Space별 동적 검증:
   * <ul>
   *   <li>Space A (bat, cmd, sh 차단): fake.jpg(실제 sh) → 차단</li>
   *   <li>Space B (php만 차단): fake.jpg(실제 sh) → 통과 (sh는 차단 목록에 없음)</li>
   * </ul>
   *
   * @param spaceId 공간 ID
   * @param declaredExtension 파일명에서 추출한 확장자
   * @param detectedMimeType Tika가 감지한 실제 MIME 타입
   * @param filename 원본 파일명
   * @throws IllegalArgumentException 차단 대상 확장자로 위장한 경우
   */
  private void validateDisguisedExtension(Long spaceId, String declaredExtension, 
                                          String detectedMimeType, String filename) {
    if (detectedMimeType == null) {
      return;
    }
    
    // 해당 Space의 차단 확장자 목록 조회 (isDeleted = false만)
    List<BlockedExtension> blockedExtensions = blockedExtensionService.getBlockedExtensions(spaceId);
    
    // 각 차단 확장자의 예상 MIME 타입과 비교
    for (BlockedExtension blocked : blockedExtensions) {
      String blockedExt = blocked.getExtension().toLowerCase();
      
      // 차단 확장자에 대한 예상 MIME 타입 계산
      String dummyFileName = "file." + blockedExt;
      String expectedMimeForBlockedExt = tika.detect(dummyFileName);
      
      // 감지된 MIME 타입이 차단 확장자의 MIME 타입과 일치하는지 확인
      if (isMimeTypeMatch(detectedMimeType, expectedMimeForBlockedExt)) {
        throw new IllegalArgumentException(
            String.format("확장자 위장 파일이 감지되었습니다. (파일명: .%s, 실제: .%s - 차단 대상)", 
                declaredExtension, blockedExt));
      }
    }
  }
  
  /**
   * MIME 타입 일치 여부 확인
   *
   * <p>두 MIME 타입이 동일하거나 같은 카테고리에 속하는지 확인
   *
   * @param detected 감지된 MIME 타입
   * @param expected 예상 MIME 타입
   * @return 일치 여부
   */
  private boolean isMimeTypeMatch(String detected, String expected) {
    if (detected == null || expected == null) {
      return false;
    }
    
    // 정확히 일치
    if (detected.equalsIgnoreCase(expected)) {
      return true;
    }
    
    // 카테고리 일치 (예: application/x-sh 와 text/x-sh 모두 sh로 간주)
    // sh, bat, cmd 등 스크립트 파일은 다양한 MIME 타입으로 감지될 수 있음
    String detectedLower = detected.toLowerCase();
    String expectedLower = expected.toLowerCase();
    
    // sh 관련 MIME 타입
    if ((detectedLower.contains("sh") || detectedLower.contains("shell")) &&
        (expectedLower.contains("sh") || expectedLower.contains("shell"))) {
      return true;
    }
    
    // bat/cmd 관련 MIME 타입
    if ((detectedLower.contains("bat") || detectedLower.contains("msdos")) &&
        (expectedLower.contains("bat") || expectedLower.contains("msdos"))) {
      return true;
    }
    
    // php 관련
    if (detectedLower.contains("php") && expectedLower.contains("php")) {
      return true;
    }
    
    // 기타 스크립트는 정확히 일치해야 함
    return false;
  }

  /**
   * 3단계: 압축 파일 내부 검증 (상세: docs/strategy.md)
   * 
   * <p>ZipValidator를 사용하여 압축 파일 내부를 재귀적으로 검증한다.
   * 
   * <p>검증 항목:
   * <ul>
   *   <li>압축 파일 내부의 차단된 확장자 검증</li>
   *   <li>Zip Bomb 감지 (압축률, 파일 개수, 총 크기)</li>
   *   <li>중첩 압축 깊이 제한 (최대 1단계)</li>
   *   <li>암호화된 압축 파일 차단</li>
   * </ul>
   * 
   * @param spaceId 공간 ID
   * @param file 업로드 파일
   * @param extension 파일 확장자
   * @throws IllegalArgumentException 압축 파일 검증 실패 시
   */
  private void validate3rdDefense(Long spaceId, MultipartFile file, String extension) {
    // 압축 파일 확장자 확인
    Set<String> archiveExtensions = Set.of("zip", "tar", "gz", "tgz", "7z");
    
    if (archiveExtensions.contains(extension.toLowerCase())) {
      // 차단된 확장자 목록 조회
      List<BlockedExtension> blockedExtensions = blockedExtensionService.getBlockedExtensions(spaceId);
      Set<String> blockedSet = blockedExtensions.stream()
          .map(be -> be.getExtension().toLowerCase())
          .collect(Collectors.toSet());
      
      // ZipValidator를 사용한 압축 파일 내부 재귀 검증
      ZipValidator zipValidator = new ZipValidator(blockedSet, tika);
      zipValidator.validateZipFile(file, 0);
    }
  }

  /**
   * 4단계: Polyglot 공격 방어 (상세: docs/strategy.md)
   * 
   * <p>파일을 로컬에 저장하고 chmod 644를 적용하여 실행 권한을 제거한다.
   * 
   * <p>방어 원리:
   * <pre>
   * 공격: 정상 이미지 + EXIF 메타데이터에 PHP 코드 삽입 (Polyglot)
   * 1~2단계: 확장자(.jpg)와 매직 넘버 모두 정상 → 통과
   * 3단계 : 압축 파일 내부 검증
   * 4단계: chmod 644 적용 → 실행 권한 없음 → 악성 코드 실행 불가
   * </pre>
   * 
   * <p>chmod 644 권한:
   * <ul>
   *   <li>6 (owner): rw- (읽기, 쓰기)</li>
   *   <li>4 (group): r-- (읽기만)</li>
   *   <li>4 (others): r-- (읽기만)</li>
   *   <li>실행 권한(x) 없음</li>
   * </ul>
   * 
   * @param spaceId 공간 ID
   * @param file 업로드 파일
   * @param extension 파일 확장자
   * @return 저장된 파일 정보
   * @throws RuntimeException 파일 저장 또는 권한 설정 실패 시
   */
  private UploadedFile validate4thDefense(Long spaceId, MultipartFile file, String extension) {
    
    try {
      // 업로드 디렉토리 생성
      Path spacePath = Paths.get(uploadDirectory, spaceId.toString());
      Files.createDirectories(spacePath);
      
      // 고유한 파일명 생성
      String storedName = UUID.randomUUID().toString() + "." + extension;
      Path targetPath = spacePath.resolve(storedName);
      
      // 파일 저장
      file.transferTo(targetPath.toFile());
      
      // 4단계 방어: chmod 644 적용 (실행 권한 제거)
      Set<PosixFilePermission> perms = new HashSet<>();
      perms.add(PosixFilePermission.OWNER_READ);
      perms.add(PosixFilePermission.OWNER_WRITE);
      perms.add(PosixFilePermission.GROUP_READ);
      perms.add(PosixFilePermission.OTHERS_READ);
      Files.setPosixFilePermissions(targetPath, perms);  // chmod 644
      
      // 메타데이터 저장
      UploadedFile uploadedFile = UploadedFile.builder()
          .spaceId(spaceId)
          .originalName(file.getOriginalFilename())
          .storedName(storedName)
          .extension(extension)
          .fileSize(file.getSize())
          .mimeType(file.getContentType())
          .filePath(targetPath.toString())
          .build();
      
      UploadedFile saved = uploadedFileRepository.save(uploadedFile);
      
      return saved;
      
    } catch (IOException e) {
      throw new RuntimeException("파일 저장 실패: " + e.getMessage(), e);
    }
  }
}

