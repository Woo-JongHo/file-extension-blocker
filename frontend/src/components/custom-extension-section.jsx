import { useState, useEffect } from 'react';
import blockedExtensionService from '@/services/blocked-extension-service';

const CustomExtensionSection = ({ spaceId, onRefresh, refreshKey }) => {
  const [customExtensions, setCustomExtensions] = useState([]);
  const [newExtension, setNewExtension] = useState('');
  const [loading, setLoading] = useState(false);
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    loadCustomExtensions();
  }, [spaceId, refreshKey]);

  const loadCustomExtensions = async () => {
    try {
      setLoading(true);
      const { data } = await blockedExtensionService.getCustomBlockList(spaceId);
      setCustomExtensions(data);
    } catch (err) {
      console.error('커스텀 확장자 로딩 실패:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = async () => {
    const extension = newExtension.trim().toLowerCase().replace(/^\.+/, '');
    
    if (!extension) {
      alert('확장자를 입력해주세요.');
      return;
    }

    if (extension.length > 20) {
      alert('확장자는 최대 20자까지 입력 가능합니다.');
      return;
    }

    try {
      setAdding(true);
      const { message } = await blockedExtensionService.addCustomExtension({
        spaceId: parseInt(spaceId),
        extension: extension,
        isFixed: false
      });
      
      alert(message || '확장자가 추가되었습니다.');
      setNewExtension('');
      loadCustomExtensions();
      onRefresh?.();
    } catch (err) {
      console.error('확장자 추가 실패:', err);
      const errorMsg = err.errorDetail || err.message || '확장자 추가에 실패했습니다.';
      alert(errorMsg);
    } finally {
      setAdding(false);
    }
  };

  const handleDelete = async (blockedId) => {
    if (!confirm('이 확장자를 삭제하시겠습니까?')) {
      return;
    }

    try {
      await blockedExtensionService.deleteCustomExtension(blockedId);
      loadCustomExtensions();
      onRefresh?.();
    } catch (err) {
      console.error('확장자 삭제 실패:', err);
      alert('확장자 삭제에 실패했습니다.');
    }
  };

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-bold text-gray-900 mb-4">
        ➕ 커스텀 확장자 (최대 200개)
      </h2>

      {/* 확장자 추가 */}
      <div className="mb-4 flex gap-2">
        <input
          type="text"
          value={newExtension}
          onChange={(e) => setNewExtension(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && handleAdd()}
          placeholder="예: php, jsp, asp"
          maxLength={20}
          className="flex-1 px-4 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <button
          onClick={handleAdd}
          disabled={adding}
          className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
        >
          {adding ? '추가 중...' : '추가'}
        </button>
      </div>

      {/* 커스텀 확장자 목록 */}
      {loading ? (
        <p className="text-gray-500">로딩 중...</p>
      ) : customExtensions.length > 0 ? (
        <div className="flex flex-wrap gap-2">
          {customExtensions.map((item) => (
            <div
              key={item.blockedId}
              className="flex items-center gap-2 bg-gray-100 px-3 py-2 rounded"
            >
              <span className="text-sm text-gray-700">
                {item.extension}
              </span>
              <button
                onClick={() => handleDelete(item.blockedId)}
                className="text-red-600 hover:text-red-800"
                title="삭제"
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      ) : (
        <p className="text-gray-400 text-sm">커스텀 확장자가 없습니다.</p>
      )}
    </div>
  );
};

export default CustomExtensionSection;

