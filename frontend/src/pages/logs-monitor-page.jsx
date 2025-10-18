import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '@/services/api';

export default function LogsMonitorPage() {
  const navigate = useNavigate();
  const [logContent, setLogContent] = useState('');
  const [loading, setLoading] = useState(false);
  const [logLines, setLogLines] = useState(200);
  const [autoRefresh, setAutoRefresh] = useState(false);
  const logContainerRef = useRef(null);
  const intervalRef = useRef(null);

  // 로그 파일 조회
  const loadLogs = async () => {
    try {
      setLoading(true);
      const { data } = await api.get(`/api/logs/file?lines=${logLines}`);
      
      // BaseResponse에서 data 추출
      setLogContent(data || '로그가 없습니다.');
      
      // 자동으로 맨 아래로 스크롤
      setTimeout(() => {
        if (logContainerRef.current) {
          logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
        }
      }, 100);
      
    } catch (error) {
      console.error('로그 조회 실패:', error);
      const errorMsg = error.errorDetail || error.message || '로그 조회 실패';
      setLogContent(`로그 파일을 불러오는데 실패했습니다.\n\n${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  // 자동 새로고침 설정
  useEffect(() => {
    if (autoRefresh) {
      loadLogs();
      intervalRef.current = setInterval(loadLogs, 3000); // 3초마다 갱신
    } else {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [autoRefresh, logLines]);

  // 초기 로드
  useEffect(() => {
    loadLogs();
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-600 to-blue-700 rounded-xl shadow-2xl p-8 mb-8">
          <button
            onClick={() => navigate('/')}
            className="text-blue-100 hover:text-white mb-4 flex items-center transition"
          >
            ← 홈으로 돌아가기
          </button>
          <h1 className="text-4xl font-bold text-white mb-2">📋 Application Logs</h1>
          <p className="text-blue-100 text-lg">파일 업로드 검증 로그 (app.log)</p>
        </div>

        {/* Controls */}
        <div className="bg-gray-800 rounded-lg shadow-lg p-6 mb-6">
          <div className="flex flex-wrap gap-3 items-center">
            <button
              onClick={loadLogs}
              disabled={loading}
              className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium transition disabled:bg-gray-600 disabled:cursor-not-allowed"
            >
              {loading ? '🔄 로딩 중...' : '🔄 새로고침'}
            </button>
            
            <label className="flex items-center gap-2 text-white">
              <input
                type="checkbox"
                checked={autoRefresh}
                onChange={(e) => setAutoRefresh(e.target.checked)}
                className="w-4 h-4"
              />
              <span>자동 새로고침 (3초)</span>
            </label>
            
            <div className="flex items-center gap-2 text-white ml-auto">
              <span>표시 라인:</span>
              <select
                value={logLines}
                onChange={(e) => setLogLines(Number(e.target.value))}
                className="px-3 py-2 bg-gray-700 text-white rounded border border-gray-600"
              >
                <option value={50}>50줄</option>
                <option value={100}>100줄</option>
                <option value={200}>200줄</option>
                <option value={500}>500줄</option>
                <option value={1000}>1000줄</option>
              </select>
            </div>
          </div>
        </div>

        {/* Log Container */}
        <div className="bg-gray-900 rounded-lg shadow-2xl overflow-hidden">
          <div className="bg-gray-800 px-6 py-3 border-b border-gray-700">
            <div className="flex items-center justify-between">
              <span className="text-gray-400 text-sm font-mono">
                📄 logs/app.log (최근 {logLines}줄)
              </span>
              <span className={`text-xs px-2 py-1 rounded ${
                autoRefresh ? 'bg-green-600 text-white' : 'bg-gray-700 text-gray-400'
              }`}>
                {autoRefresh ? '🟢 자동 갱신 중' : '⚪ 수동 모드'}
              </span>
            </div>
          </div>
          
          <div
            ref={logContainerRef}
            className="p-6 h-[calc(100vh-400px)] overflow-y-auto font-mono text-sm"
            style={{
              scrollbarWidth: 'thin',
              scrollbarColor: '#4B5563 #1F2937',
            }}
          >
            {logContent ? (
              <pre className="text-gray-300 whitespace-pre-wrap break-words">
                {logContent}
              </pre>
            ) : (
              <div className="text-gray-400 text-center py-20">
                <p className="text-xl">로그가 없습니다</p>
                <p className="text-sm mt-2">
                  파일 업로드를 시도하면 로그가 생성됩니다
                </p>
              </div>
            )}
          </div>
        </div>

        {/* 로그 색상 가이드 */}
        <div className="mt-6 bg-gray-800 rounded-lg p-6">
          <h3 className="text-white font-semibold mb-3">로그 레벨 가이드</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div className="flex items-center gap-2">
              <span className="px-2 py-1 bg-blue-600 text-white rounded text-xs font-mono">INFO</span>
              <span className="text-gray-400">일반 정보</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="px-2 py-1 bg-yellow-600 text-white rounded text-xs font-mono">WARN</span>
              <span className="text-gray-400">경고 (차단)</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="px-2 py-1 bg-red-600 text-white rounded text-xs font-mono">ERROR</span>
              <span className="text-gray-400">오류</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="px-2 py-1 bg-purple-600 text-white rounded text-xs font-mono">DEBUG</span>
              <span className="text-gray-400">디버그 정보</span>
            </div>
          </div>
        </div>

        {/* 검증 단계 가이드 */}
        <div className="mt-6 bg-gray-800 rounded-lg p-6">
          <h3 className="text-white font-semibold mb-3">📌 파일 검증 단계</h3>
          <div className="space-y-2 text-sm text-gray-300">
            <div className="flex items-start gap-2">
              <span className="text-blue-400 font-bold">1단계:</span>
              <span>확장자 Blacklist 검증 (.bat, .exe, .sh 등 차단)</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-green-400 font-bold">2단계:</span>
              <span>매직바이트 & MIME 타입 검증 (확장자 위장 차단)</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-orange-400 font-bold">3단계:</span>
              <span>압축 파일 내부 검증 (Zip Bomb, 중첩 압축 차단)</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-purple-400 font-bold">4단계:</span>
              <span>파일 저장 & chmod 644 권한 설정</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
