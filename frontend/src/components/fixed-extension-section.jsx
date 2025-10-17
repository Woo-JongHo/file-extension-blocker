import { useState, useEffect } from 'react';
import blockedExtensionService from '@/services/blocked-extension-service';

const FixedExtensionSection = ({ spaceId, refreshKey }) => {
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
    try {
      await blockedExtensionService.toggleFixedExtension(spaceId, extension);
      loadFixedExtensions();
    } catch (err) {
      console.error('상태 변경 실패:', err);
      alert('고정 확장자 상태 변경에 실패했습니다.');
    }
  };

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-bold text-gray-900 mb-4">
        📌 고정 확장자 (자주 차단하는 확장자)
      </h2>
      
      {loading ? (
        <p className="text-gray-500">로딩 중...</p>
      ) : fixedExtensions.length > 0 ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
          {fixedExtensions.map((item) => (
            <label
              key={item.blockedId}
              className="flex items-center space-x-2 cursor-pointer hover:bg-gray-50 p-2 rounded"
            >
              <input
                type="checkbox"
                checked={!item.isDeleted}
                onChange={() => handleToggle(item.extension)}
                className="h-4 w-4 text-blue-600 rounded focus:ring-blue-500"
              />
              <span className="text-sm text-gray-700">
                {item.extension}
              </span>
            </label>
          ))}
        </div>
      ) : (
        <p className="text-gray-400">고정 확장자가 없습니다.</p>
      )}
    </div>
  );
};

export default FixedExtensionSection;

