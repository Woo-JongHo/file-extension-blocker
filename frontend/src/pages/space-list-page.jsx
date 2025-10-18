import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import spaceService from '@/services/space-service';
import memberService from '@/services/member-service';
import statsService from '@/services/stats-service';
import SpaceCard from '@/components/space-card';
import CreateSpaceModal from '@/components/create-space-modal';

const SpaceListPage = () => {
  const [spaces, setSpaces] = useState([]);
  const [spaceMembers, setSpaceMembers] = useState({});
  const [spaceStats, setSpaceStats] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    loadSpacesAndMembers();
  }, []);

  const loadSpacesAndMembers = async () => {
    try {
      setLoading(true);
      
      // Space 목록 조회
      const { data: spacesData, message } = await spaceService.getAllSpaces();
      setSpaces(spacesData);

      // 각 Space의 멤버 및 통계 조회
      const membersMap = {};
      const statsMap = {};
      for (const space of spacesData) {
        const { data: members } = await memberService.getMembersBySpace(space.spaceId);
        const stats = await statsService.getSpaceStats(space.spaceId);
        membersMap[space.spaceId] = members;
        statsMap[space.spaceId] = stats;
      }
      setSpaceMembers(membersMap);
      setSpaceStats(statsMap);

    } catch (err) {
      console.error('데이터 로딩 실패:', err);
      setError('Space 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleMemberClick = (spaceId, member) => {
    // Space 상세 페이지로 이동 (멤버 정보와 함께)
    navigate(`/space/${spaceId}`, { 
      state: { 
        member,
        spaceName: spaces.find(s => s.spaceId === spaceId)?.spaceName 
      } 
    });
  };

  const handleCreateSpace = async (formData) => {
    try {
      // Space + Admin Member + 고정 확장자 7개를 트랜잭션으로 한 번에 생성
      const { data } = await spaceService.createSpaceWithAdmin({
        spaceName: formData.spaceName,
        description: formData.description,
        adminUsername: formData.adminUsername,
        adminPassword: formData.adminPassword
      });

      alert(
        `✅ Space 생성 완료!\n\n` +
        `📁 Space: ${data.space.spaceName}\n` +
        `👤 관리자: ${data.adminMember.username}\n` +
        `🔑 비밀번호: ${formData.adminPassword}\n\n` +
        `🛡️ 고정 확장자 ${data.fixedExtensionsCount}개가 자동으로 추가되었습니다!\n` +
        `(기본 비활성화 상태 - 필요한 확장자만 체크하여 활성화하세요)`
      );

      setIsModalOpen(false);
      loadSpacesAndMembers(); // 목록 새로고침

    } catch (err) {
      console.error('Space 생성 실패:', err);
      const errorMsg = err.errorDetail || err.message || 'Space 생성에 실패했습니다.';
      alert('❌ ' + errorMsg);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">로딩 중...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <p className="text-red-600 text-lg">{error}</p>
          <button 
            onClick={loadSpacesAndMembers}
            className="mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            다시 시도
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* 헤더 */}
        <div className="mb-8 flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">
              파일 확장자 차단 기능 구현
            </h1>
            <p className="mt-2 text-gray-600">
              파일 확장자 차단 시스템 - Space 선택
            </p>
          </div>
          <div className="flex gap-3">
            <button
              onClick={() => navigate('/logs')}
              className="px-6 py-3 bg-gradient-to-r from-gray-700 to-gray-800 text-white font-semibold rounded-lg hover:from-gray-800 hover:to-gray-900 shadow-md hover:shadow-lg transition-all"
            >
              📋 로그 보기
            </button>
            <button
              onClick={() => setIsModalOpen(true)}
              className="px-6 py-3 bg-gradient-to-r from-purple-600 to-indigo-600 text-white font-semibold rounded-lg hover:from-purple-700 hover:to-indigo-700 shadow-md hover:shadow-lg transition-all"
            >
              + 새 Space 생성
            </button>
          </div>
        </div>

        {/* 설명 섹션 */}
        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-lg p-6 mb-8">
          <div className="flex items-start">
            <div className="flex-shrink-0">
              <svg className="h-6 w-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div className="ml-3 flex-1">
              <h3 className="text-lg font-semibold text-gray-900 mb-2">
                🛡️ 파일 확장자 차단 시스템
              </h3>
              <div className="text-sm text-gray-700 space-y-2">
                <p>
                  <strong>사용 방법:</strong> 아래 Space 카드에서 멤버 이름을 클릭하여 해당 권한으로 접속하세요.
                </p>
                <p>
                  <strong>권한:</strong>
                  <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-800">
                    관리자
                  </span> 확장자 관리 + 파일 업로드 |
                  <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800">
                    멤버
                  </span> 파일 업로드만 가능
                </p>
                <p>
                  <strong>방어 전략:</strong>
                  <span className="ml-2 text-blue-600">1단계 확장자 검증</span> →
                  <span className="ml-2 text-green-600">2단계 매직바이트 검증</span> →
                  <span className="ml-2 text-orange-600">3단계 압축 파일 검증</span>
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* 테스트 파일 다운로드 섹션 */}
        <div className="bg-gradient-to-r from-amber-50 to-yellow-50 border border-amber-200 rounded-lg p-6 mb-8">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            테스트용 업로드 파일
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* 1-normal */}
            <div className="bg-white rounded-lg p-4 border border-gray-200">
              <h4 className="font-semibold text-sm text-green-700 mb-3">정상 파일</h4>
              <div className="space-y-2">
                <div>
                  <a href="/test-files/1-normal/document.txt"
                     className="text-xs text-blue-600 hover:underline font-medium">document.txt</a>
                  <p className="text-xs text-gray-500 mt-0.5">통과 예상</p>
                </div>
                <div>
                  <a href="/test-files/1-normal/data.json"
                     className="text-xs text-blue-600 hover:underline font-medium">data.json</a>
                  <p className="text-xs text-gray-500 mt-0.5">통과 예상</p>
                </div>
              </div>
            </div>

            {/* 2-blocked-ext */}
            <div className="bg-white rounded-lg p-4 border border-gray-200">
              <h4 className="font-semibold text-sm text-red-700 mb-3">차단 확장자</h4>
              <div className="space-y-2">
                <div>
                  <a href="/test-files/2-blocked-ext/virus.bat"
                     className="text-xs text-blue-600 hover:underline font-medium">virus.bat</a>
                  <p className="text-xs text-gray-500 mt-0.5">1단계 차단 - 확장자 검증</p>
                </div>
                <div>
                  <a href="/test-files/2-blocked-ext/script.sh"
                     className="text-xs text-blue-600 hover:underline font-medium">script.sh</a>
                  <p className="text-xs text-gray-500 mt-0.5">1단계 차단 - 확장자 검증</p>
                </div>
                <div>
                  <a href="/test-files/2-blocked-ext/hack.php"
                     className="text-xs text-blue-600 hover:underline font-medium">hack.php</a>
                  <p className="text-xs text-gray-500 mt-0.5">1단계 차단 - 확장자 검증</p>
                </div>
              </div>
            </div>

            {/* 3-disguised */}
            <div className="bg-white rounded-lg p-4 border border-gray-200">
              <h4 className="font-semibold text-sm text-orange-700 mb-3">확장자 위장</h4>
              <div className="space-y-2">
                <div>
                  <a href="/test-files/3-disguised/fake-image.jpg"
                     className="text-xs text-blue-600 hover:underline font-medium">fake-image.jpg</a>
                  <p className="text-xs text-gray-500 mt-0.5">2단계 차단 - .sh 스크립트 위장 (MIME: application/x-sh)</p>
                </div>
                <div>
                  <a href="/test-files/3-disguised/fake-image-bat.jpg"
                     className="text-xs text-blue-600 hover:underline font-medium">fake-image-bat.jpg</a>
                  <p className="text-xs text-gray-500 mt-0.5">2단계 차단 - .bat 스크립트 위장 (MIME: text/plain)</p>
                </div>
              </div>
            </div>

            {/* 4-archive */}
            <div className="bg-white rounded-lg p-4 border border-gray-200">
              <h4 className="font-semibold text-sm text-purple-700 mb-3">압축 파일</h4>
              <div className="space-y-2">
                <div>
                  <a href="/test-files/4-archive/normal.zip"
                     className="text-xs text-blue-600 hover:underline font-medium">normal.zip</a>
                  <p className="text-xs text-gray-500 mt-0.5">통과 예상 - 정상 압축 파일</p>
                </div>
                <div>
                  <a href="/test-files/4-archive/malicious.zip"
                     className="text-xs text-blue-600 hover:underline font-medium">malicious.zip</a>
                  <p className="text-xs text-gray-500 mt-0.5">3단계 차단 - 내부 .bat 파일</p>
                </div>
                <div>
                  <a href="/test-files/4-archive/zipbomb.zip"
                     className="text-xs text-blue-600 hover:underline font-medium">zipbomb.zip</a>
                  <p className="text-xs text-gray-500 mt-0.5">3단계 차단 - 10MB 초과</p>
                </div>
                <div>
                  <a href="/test-files/4-archive/nested.zip"
                     className="text-xs text-blue-600 hover:underline font-medium">nested.zip</a>
                  <p className="text-xs text-gray-500 mt-0.5">3단계 차단 - 중첩 깊이 초과</p>
                </div>
              </div>
            </div>
          </div>
          <p className="text-xs text-gray-600 mt-4">
            이 파일들을 다운로드 받아 Space에 업로드하여 방어 전략을 테스트해보세요!
          </p>
        </div>

        {/* Space 카드 그리드 (3×3) */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {spaces.map((space) => (
            <SpaceCard
              key={space.spaceId}
              space={space}
              members={spaceMembers[space.spaceId] || []}
              stats={spaceStats[space.spaceId]}
              onMemberClick={handleMemberClick}
            />
          ))}
        </div>

        {/* Space가 없을 때 */}
        {spaces.length === 0 && (
          <div className="text-center py-12">
            <p className="text-gray-500">등록된 Space가 없습니다.</p>
          </div>
        )}

        {/* Space 생성 모달 */}
        <CreateSpaceModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSubmit={handleCreateSpace}
        />
      </div>
    </div>
  );
};

export default SpaceListPage;

