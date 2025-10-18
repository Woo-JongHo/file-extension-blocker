import { useState, useEffect } from 'react';
import blockedExtensionService from '@/services/blocked-extension-service';

const FixedExtensionSection = ({ spaceId, refreshKey, isAdmin = true }) => {
  const [fixedExtensions, setFixedExtensions] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadFixedExtensions();
  }, [spaceId, refreshKey]);

  const loadFixedExtensions = async () => {
    try {
      setLoading(true);
      const { data } = await blockedExtensionService.getFixedBlockList(spaceId);
      setFixedExtensions(data);
    } catch (err) {
      console.error('고정 확장자 로딩 실패:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = async (extension) => {
    if (!isAdmin) return;
    
    // 낙관적 업데이트: 먼저 로컬 상태를 변경
    setFixedExtensions(prev => 
      prev.map(item => 
        item.extension === extension 
          ? { ...item, isDeleted: !item.isDeleted }
          : item
      )
    );
    
    try {
      await blockedExtensionService.toggleFixedExtension(spaceId, extension);
    } catch (err) {
      console.error('상태 변경 실패:', err);
      alert('고정 확장자 상태 변경에 실패했습니다.');
      // 실패 시 원래 상태로 복구
      loadFixedExtensions();
    }
  };

  // 활성화된 확장자만 필터링 (일반 멤버용)
  const activeExtensions = isAdmin 
    ? fixedExtensions 
    : fixedExtensions.filter(item => !item.isDeleted);

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-bold text-gray-900">
          📌 고정 확장자 (자주 차단하는 확장자)
        </h2>
        {!isAdmin && (
          <span className="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded">
            읽기 전용
          </span>
        )}
      </div>
      
      <div className="min-h-[100px] flex items-center">
        {loading ? (
          <p className="text-gray-500">로딩 중...</p>
        ) : activeExtensions.length > 0 ? (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3 w-full">
            {activeExtensions.map((item) => (
              <label
                key={item.blockedId}
                className={`flex items-center space-x-2 p-2 rounded ${
                  isAdmin ? 'cursor-pointer hover:bg-gray-50' : 'cursor-default'
                }`}
              >
                <input
                  type="checkbox"
                  checked={!item.isDeleted}
                  onChange={() => handleToggle(item.extension)}
                  disabled={!isAdmin}
                  className={`h-4 w-4 text-blue-600 rounded focus:ring-blue-500 ${
                    !isAdmin ? 'cursor-not-allowed' : ''
                  }`}
                />
                <span className="text-sm text-gray-700">
                  {item.extension}
                </span>
              </label>
            ))}
          </div>
        ) : (
          <p className="text-gray-400">
            {isAdmin ? '고정 확장자가 없습니다.' : '차단 중인 고정 확장자가 없습니다.'}
          </p>
        )}
      </div>
    </div>
  );
};

export default FixedExtensionSection;

