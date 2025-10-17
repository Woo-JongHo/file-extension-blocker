import { useState } from 'react';
import uploadedFileService from '@/services/uploaded-file-service';

const FileUploadSection = ({ spaceId, onUploadSuccess }) => {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0];
    setFile(selectedFile);
    setMessage({ type: '', text: '' });
  };

  const handleUpload = async () => {
    if (!file) {
      setMessage({ type: 'error', text: '파일을 선택해주세요.' });
      return;
    }

    try {
      setUploading(true);
      setMessage({ type: '', text: '' });

      const { message } = await uploadedFileService.uploadFile(spaceId, file);
      
      setMessage({ type: 'success', text: message || '파일이 성공적으로 업로드되었습니다!' });
      setFile(null);
      
      // 파일 input 초기화
      const fileInput = document.getElementById('file-input');
      if (fileInput) {
        fileInput.value = '';
      }

      // 파일 목록 새로고침
      onUploadSuccess?.();

    } catch (err) {
      console.error('파일 업로드 실패:', err);
      const errorMessage = err.errorDetail || err.message || '파일 업로드에 실패했습니다.';
      setMessage({ type: 'error', text: errorMessage });
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-bold text-gray-900 mb-4">
        📤 파일 업로드
      </h2>

      <div className="space-y-4">
        {/* 파일 선택 */}
        <div>
          <input
            id="file-input"
            type="file"
            onChange={handleFileChange}
            className="block w-full text-sm text-gray-500
              file:mr-4 file:py-2 file:px-4
              file:rounded file:border-0
              file:text-sm file:font-semibold
              file:bg-blue-50 file:text-blue-700
              hover:file:bg-blue-100"
          />
          {file && (
            <p className="text-sm text-gray-600 mt-2">
              선택된 파일: {file.name} ({(file.size / 1024).toFixed(2)} KB)
            </p>
          )}
        </div>

        {/* 업로드 버튼 */}
        <button
          onClick={handleUpload}
          disabled={!file || uploading}
          className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
        >
          {uploading ? '업로드 중...' : '업로드'}
        </button>

        {/* 메시지 */}
        {message.text && (
          <div className={`p-3 rounded ${
            message.type === 'success' 
              ? 'bg-green-50 text-green-700 border border-green-200' 
              : 'bg-red-50 text-red-700 border border-red-200'
          }`}>
            {message.text}
          </div>
        )}
      </div>
    </div>
  );
};

export default FileUploadSection;

