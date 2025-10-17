import { useState } from 'react';

const CreateSpaceModal = ({ isOpen, onClose, onSubmit }) => {
  const [formData, setFormData] = useState({
    spaceName: '',
    adminUsername: '',
  });

  const [errors, setErrors] = useState({});

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    // 에러 초기화
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const validate = () => {
    const newErrors = {};
    
    if (!formData.spaceName.trim()) {
      newErrors.spaceName = 'Space 이름을 입력해주세요.';
    }
    
    if (!formData.adminUsername.trim()) {
      newErrors.adminUsername = 'Admin 계정명을 입력해주세요.';
    } else if (formData.adminUsername.length < 3) {
      newErrors.adminUsername = '계정명은 최소 3자 이상이어야 합니다.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    if (!validate()) {
      return;
    }

    // 비밀번호는 자동으로 1234, description은 null
    onSubmit({
      spaceName: formData.spaceName.trim(),
      adminUsername: formData.adminUsername.trim(),
      adminPassword: '1234',
      description: null
    });

    // 폼 초기화
    setFormData({
      spaceName: '',
      adminUsername: '',
    });
    setErrors({});
  };

  const handleClose = () => {
    setFormData({
      spaceName: '',
      adminUsername: '',
    });
    setErrors({});
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md mx-4">
        {/* 헤더 */}
        <div className="bg-gradient-to-r from-purple-600 to-indigo-600 text-white px-6 py-4 rounded-t-lg">
          <h2 className="text-xl font-bold">새 Space 생성</h2>
          <p className="text-sm text-purple-100 mt-1">
            Space 이름과 관리자 계정을 입력하세요
          </p>
        </div>

        {/* 폼 */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Space 이름 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Space 이름 <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              name="spaceName"
              value={formData.spaceName}
              onChange={handleChange}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 ${
                errors.spaceName ? 'border-red-500' : 'border-gray-300'
              }`}
              placeholder="예: 프론트엔드팀"
            />
            {errors.spaceName && (
              <p className="text-red-500 text-sm mt-1">{errors.spaceName}</p>
            )}
          </div>

          {/* Admin Username */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              관리자 계정명 <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              name="adminUsername"
              value={formData.adminUsername}
              onChange={handleChange}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 ${
                errors.adminUsername ? 'border-red-500' : 'border-gray-300'
              }`}
              placeholder="예: admin_frontend"
            />
            {errors.adminUsername && (
              <p className="text-red-500 text-sm mt-1">{errors.adminUsername}</p>
            )}
          </div>

          {/* 자동 설정 안내 */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
            <p className="text-sm text-blue-800">
              <span className="font-semibold">자동 설정:</span>
            </p>
            <ul className="text-sm text-blue-700 mt-1 ml-4 list-disc">
              <li>비밀번호: <code className="bg-blue-100 px-1 rounded">1234</code></li>
              <li>Top-6 고정 확장자 자동 추가</li>
            </ul>
          </div>

          {/* 버튼 */}
          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={handleClose}
              className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
            >
              취소
            </button>
            <button
              type="submit"
              className="flex-1 px-4 py-2 bg-gradient-to-r from-purple-600 to-indigo-600 text-white rounded-lg hover:from-purple-700 hover:to-indigo-700 transition-colors"
            >
              생성
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateSpaceModal;

