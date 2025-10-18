import { useState, useEffect, useRef } from 'react';
import { auditLogService } from '../services/audit-log-service';

export default function LogsMonitorPage() {
  const [logs, setLogs] = useState([]);
  const [isConnected, setIsConnected] = useState(false);
  const [stats, setStats] = useState({
    total: 0,
    uploads: 0,
    deletes: 0,
    blocked: 0,
  });
  const [filters, setFilters] = useState({
    info: true,
    warn: true,
    error: true,
  });
  const [logLimit, setLogLimit] = useState(100);
  const eventSourceRef = useRef(null);
  const logContainerRef = useRef(null);

  // SSE 연결
  const connect = () => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    const eventSource = auditLogService.connectToLogStream();
    eventSourceRef.current = eventSource;

    eventSource.addEventListener('connected', (e) => {
      setIsConnected(true);
      addLog('info', e.data);
    });

    eventSource.addEventListener('log', (e) => {
      addLog('info', e.data);
    });

    eventSource.onerror = () => {
      setIsConnected(false);
      addLog('error', '연결 오류 발생');
    };
  };

  // SSE 연결 해제
  const disconnect = () => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
      setIsConnected(false);
      addLog('warn', '연결이 해제되었습니다');
    }
  };

  // 로그 추가
  const addLog = (level, message) => {
    const now = new Date();
    const timestamp = now.toLocaleTimeString('ko-KR', { hour12: false });

    const newLog = { level, message, timestamp, id: Date.now() + Math.random() };
    setLogs((prev) => [newLog, ...prev]);

    // 통계 업데이트
    setStats((prev) => {
      const updated = { ...prev, total: prev.total + 1 };
      if (message.includes('FILE_UPLOAD')) updated.uploads++;
      if (message.includes('FILE_DELETE')) updated.deletes++;
      if (message.includes('차단')) updated.blocked++;
      return updated;
    });
  };

  // 최근 로그 불러오기
  const loadRecentLogs = async () => {
    try {
      const response = await auditLogService.getRecentLogs(logLimit);
      if (response.success && response.data) {
        response.data.forEach((log) => {
          addLog(
            'info',
            `[${log.action}] ${log.userName} - ${log.fileName || 'N/A'} (${log.ipAddress})`
          );
        });
      }
    } catch (error) {
      addLog('error', '로그 불러오기 실패: ' + error.message);
    }
  };

  // 로그 초기화
  const clearLogs = () => {
    if (confirm('모든 로그를 지우시겠습니까?')) {
      setLogs([]);
      setStats({ total: 0, uploads: 0, deletes: 0, blocked: 0 });
      addLog('info', '로그가 초기화되었습니다');
    }
  };

  // 로그 다운로드
  const downloadLogs = () => {
    const text = logs
      .map((log) => `[${log.timestamp}] ${log.level.toUpperCase()}: ${log.message}`)
      .join('\n');
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `logs-${new Date().toISOString().split('T')[0]}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // 필터링된 로그
  const filteredLogs = logs.filter((log) => {
    if (log.level === 'info' && !filters.info) return false;
    if (log.level === 'warn' && !filters.warn) return false;
    if (log.level === 'error' && !filters.error) return false;
    return true;
  });

  // 자동 연결
  useEffect(() => {
    connect();
    return () => disconnect();
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-600 to-blue-700 rounded-xl shadow-2xl p-8 mb-8">
          <h1 className="text-4xl font-bold text-white mb-2">🛡️ File Blocker</h1>
          <p className="text-blue-100 text-lg">실시간 로그 모니터</p>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <StatCard title="Total Logs" value={stats.total} color="blue" />
          <StatCard title="File Uploads" value={stats.uploads} color="green" />
          <StatCard title="File Deletes" value={stats.deletes} color="yellow" />
          <StatCard title="Blocked Attempts" value={stats.blocked} color="red" />
        </div>

        {/* Controls */}
        <div className="bg-gray-800 rounded-lg shadow-lg p-6 mb-6">
          <div className="flex flex-wrap gap-3 items-center">
            <span
              className={`px-4 py-2 rounded-full text-sm font-semibold ${
                isConnected
                  ? 'bg-green-500 text-white'
                  : 'bg-red-500 text-white'
              }`}
            >
              {isConnected ? '🟢 Connected' : '🔴 Disconnected'}
            </span>
            <button
              onClick={connect}
              className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg font-medium transition"
            >
              🔌 연결
            </button>
            <button
              onClick={disconnect}
              className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg font-medium transition"
            >
              🔌 연결 해제
            </button>
            <button
              onClick={clearLogs}
              className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg font-medium transition"
            >
              🗑️ 로그 지우기
            </button>
            <button
              onClick={downloadLogs}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium transition"
            >
              💾 로그 다운로드
            </button>
            <button
              onClick={loadRecentLogs}
              className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg font-medium transition"
            >
              🔄 최근 로그 불러오기
            </button>
          </div>
        </div>

        {/* Filters */}
        <div className="bg-gray-800 rounded-lg shadow-lg p-6 mb-6">
          <div className="flex flex-wrap gap-4 items-center">
            <label className="flex items-center gap-2 text-white">
              <input
                type="checkbox"
                checked={filters.info}
                onChange={(e) =>
                  setFilters({ ...filters, info: e.target.checked })
                }
                className="w-4 h-4"
              />
              <span>INFO</span>
            </label>
            <label className="flex items-center gap-2 text-white">
              <input
                type="checkbox"
                checked={filters.warn}
                onChange={(e) =>
                  setFilters({ ...filters, warn: e.target.checked })
                }
                className="w-4 h-4"
              />
              <span>WARN</span>
            </label>
            <label className="flex items-center gap-2 text-white">
              <input
                type="checkbox"
                checked={filters.error}
                onChange={(e) =>
                  setFilters({ ...filters, error: e.target.checked })
                }
                className="w-4 h-4"
              />
              <span>ERROR</span>
            </label>
            <label className="flex items-center gap-2 text-white ml-auto">
              <span>최근 로그:</span>
              <input
                type="number"
                value={logLimit}
                onChange={(e) => setLogLimit(Number(e.target.value))}
                min="10"
                max="1000"
                step="10"
                className="w-24 px-3 py-1 bg-gray-700 text-white rounded border border-gray-600"
              />
            </label>
          </div>
        </div>

        {/* Log Container */}
        <div
          ref={logContainerRef}
          className="bg-gray-900 rounded-lg shadow-2xl p-6 h-[600px] overflow-y-auto font-mono text-sm"
          style={{
            scrollbarWidth: 'thin',
            scrollbarColor: '#4B5563 #1F2937',
          }}
        >
          {filteredLogs.length === 0 ? (
            <div className="text-gray-400 text-center py-20">
              <p className="text-xl">로그가 없습니다</p>
              <p className="text-sm mt-2">
                연결 버튼을 클릭하거나 최근 로그를 불러오세요
              </p>
            </div>
          ) : (
            filteredLogs.map((log) => (
              <LogEntry key={log.id} log={log} />
            ))
          )}
        </div>
      </div>
    </div>
  );
}

// 통계 카드 컴포넌트
function StatCard({ title, value, color }) {
  const colors = {
    blue: 'from-blue-600 to-blue-700',
    green: 'from-green-600 to-green-700',
    yellow: 'from-yellow-600 to-yellow-700',
    red: 'from-red-600 to-red-700',
  };

  return (
    <div
      className={`bg-gradient-to-br ${colors[color]} rounded-lg shadow-lg p-6 text-white`}
    >
      <div className="text-4xl font-bold mb-2">{value}</div>
      <div className="text-sm opacity-90 uppercase tracking-wider">{title}</div>
    </div>
  );
}

// 로그 엔트리 컴포넌트
function LogEntry({ log }) {
  const levelColors = {
    info: 'border-blue-500 bg-blue-500/5',
    warn: 'border-yellow-500 bg-yellow-500/10',
    error: 'border-red-500 bg-red-500/10',
    success: 'border-green-500 bg-green-500/10',
  };

  return (
    <div
      className={`mb-2 p-3 rounded border-l-4 ${
        levelColors[log.level] || levelColors.info
      } hover:bg-gray-800 transition animate-fadeIn`}
    >
      <span className="text-gray-400 mr-3">[{log.timestamp}]</span>
      <span className="text-gray-200">{log.message}</span>
    </div>
  );
}

