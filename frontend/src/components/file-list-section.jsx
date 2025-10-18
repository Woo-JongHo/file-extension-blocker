import { useState, useEffect } from 'react';
import uploadedFileService from '@/services/uploaded-file-service';

const FileListSection = ({ spaceId, refreshKey }) => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadFiles();
  }, [spaceId, refreshKey]);

  const loadFiles = async () => {
    try {
      setLoading(true);
      const { data } = await uploadedFileService.getFilesBySpace(spaceId);
      setFiles(data || []);
    } catch (err) {
      console.error('파일 목록 로딩 실패:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (fileId) => {
    if (!confirm('이 파일을 삭제하시겠습니까?')) {
      return;
    }

    try {
      await uploadedFileService.deleteFile(fileId);
      loadFiles();
    } catch (err) {
      console.error('파일 삭제 실패:', err);
      alert('파일 삭제에 실패했습니다.');
    }
  };

  const handleDownload = async (fileId, fileName) => {
    try {
      await uploadedFileService.downloadFile(fileId, fileName);
    } catch (err) {
      console.error('파일 다운로드 실패:', err);
      alert('파일 다운로드에 실패했습니다.');
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('ko-KR');
  };

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-bold text-gray-900 mb-4">
        📁 업로드된 파일 ({files.length}개)
      </h2>

      <div className="min-h-[120px] flex items-center">
        {loading ? (
          <p className="text-gray-500">로딩 중...</p>
        ) : files.length > 0 ? (
          <div className="overflow-x-auto w-full">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    파일명
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    확장자
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    크기
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    업로더
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    업로드 일시
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    액션
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {files.map((file) => (
                  <tr key={file.fileId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-900">
                      {file.originalName}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      .{file.extension}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      {formatFileSize(file.fileSize)}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                        {file.uploaderName || '알 수 없음'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      {formatDate(file.createdAt)}
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleDownload(file.fileId, file.originalName)}
                          className="text-blue-600 hover:text-blue-800"
                        >
                          다운로드
                        </button>
                        <button
                          onClick={() => handleDelete(file.fileId)}
                          className="text-red-600 hover:text-red-800"
                        >
                          삭제
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-gray-400">업로드된 파일이 없습니다.</p>
        )}
      </div>
    </div>
  );
};

export default FileListSection;

