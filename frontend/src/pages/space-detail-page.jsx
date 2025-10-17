import { useState, useEffect } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import FixedExtensionSection from '@/components/fixed-extension-section';
import CustomExtensionSection from '@/components/custom-extension-section';
import FileUploadSection from '@/components/file-upload-section';
import FileListSection from '@/components/file-list-section';

const SpaceDetailPage = () => {
  const { spaceId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { member, spaceName } = location.state || {};

  const [refreshKey, setRefreshKey] = useState(0);

  // 멤버 정보가 없으면 메인으로 리다이렉트
  useEffect(() => {
    if (!member) {
      navigate('/');
    }
  }, [member, navigate]);

  if (!member) {
    return null;
  }

  const isAdmin = member.role === 'ADMIN';

  // 새로고침 트리거
  const handleRefresh = () => {
    setRefreshKey(prev => prev + 1);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 헤더 */}
      <div className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div>
              <button
                onClick={() => navigate('/')}
                className="text-blue-600 hover:text-blue-800 mb-2 flex items-center"
              >
                ← 뒤로 가기
              </button>
              <h1 className="text-2xl font-bold text-gray-900">
                {spaceName || 'Space 관리'}
              </h1>
              <p className="text-sm text-gray-600 mt-1">
                사용자: {member.username} ({isAdmin ? '관리자' : '멤버'})
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 메인 컨텐츠 */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        
        {/* 고정 확장자 섹션 (관리자만) */}
        {isAdmin && (
          <FixedExtensionSection 
            spaceId={spaceId} 
            refreshKey={refreshKey}
          />
        )}

        {/* 커스텀 확장자 섹션 (관리자만) */}
        {isAdmin && (
          <CustomExtensionSection 
            spaceId={spaceId} 
            onRefresh={handleRefresh}
            refreshKey={refreshKey}
          />
        )}

        {/* 파일 업로드 섹션 (모든 사용자) */}
        <FileUploadSection 
          spaceId={spaceId} 
          onUploadSuccess={handleRefresh}
        />

        {/* 업로드된 파일 목록 (모든 사용자) */}
        <FileListSection 
          spaceId={spaceId}
          refreshKey={refreshKey}
        />
      </div>
    </div>
  );
};

export default SpaceDetailPage;

