const SpaceCard = ({ space, members, stats, onMemberClick }) => {
  return (
    <div className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow overflow-hidden">
      {/* Space 이름 */}
      <div className="bg-gradient-to-r from-purple-600 to-indigo-600 text-white p-4">
        <h3 className="text-xl font-bold">
          {space.spaceName}
        </h3>
        {space.description && (
          <p className="text-sm text-purple-100 mt-1">
            {space.description}
          </p>
        )}
      </div>

      {/* 통계 섹션 */}
      {stats && (
        <div className="bg-gray-50 px-4 py-3 grid grid-cols-2 gap-4 border-b">
          <div className="text-center">
            <div className="text-2xl font-bold text-blue-600">
              {stats.fileCount}
            </div>
            <div className="text-xs text-gray-500">
              📂 업로드 파일
            </div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-red-600">
              {stats.extensionCount}
            </div>
            <div className="text-xs text-gray-500">
              🚫 차단 확장자
            </div>
          </div>
        </div>
      )}

      {/* 멤버 목록 */}
      <div className="p-4">
        <p className="text-sm font-semibold text-gray-700 mb-3">
          👥 멤버 ({members.length}명)
        </p>
        
        {members.length > 0 ? (
          <div className="space-y-1">
            {members.map((member) => (
              <button
                key={member.memberId}
                onClick={() => onMemberClick(space.spaceId, member)}
                className="w-full text-left px-3 py-2 rounded hover:bg-blue-50 transition-colors flex items-center justify-between group"
              >
                <span className="text-gray-700 group-hover:text-blue-600">
                  {member.username}
                </span>
                <span className={`text-xs px-2 py-1 rounded ${
                  member.role === 'ADMIN' 
                    ? 'bg-purple-100 text-purple-700' 
                    : 'bg-gray-100 text-gray-600'
                }`}>
                  {member.role === 'ADMIN' ? '관리자' : '멤버'}
                </span>
              </button>
            ))}
          </div>
        ) : (
          <p className="text-sm text-gray-400 text-center py-4">
            멤버가 없습니다.
          </p>
        )}
      </div>
    </div>
  );
};

export default SpaceCard;

