import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getHostPoll, closePoll } from '../../api/poll'
import { useSSE } from '../../hooks/useSSE'

export default function Manage() {
  const { shareCode } = useParams()
  const navigate = useNavigate()
  const { counts, connected, disconnectReason } = useSSE(shareCode)

  const [poll, setPoll] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showNewPollModal, setShowNewPollModal] = useState(false)

  useEffect(() => {
    const fetch = async () => {
      try {
        const result = await getHostPoll(shareCode);
        setPoll(result)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }
    fetch()
  }, [shareCode])

  const handleClose = async () => {
    try {
      const result = await closePoll(shareCode)
      setPoll(prev => ({ ...prev, status: 'CLOSED' }))
    } catch (err) {
      setError(err.message)
    }
  }

  if (loading) return <LoadingScreen />
  if (error) return <ErrorScreen message={error} />
  if (!poll) return null

  const total = poll.options.reduce((sum, o) => sum + (counts[o.optionId] ?? o.count), 0)

  return (
    <div className="min-h-screen px-4 py-12">
      <div className="max-w-2xl mx-auto">
        <div className="flex items-start justify-between mb-8">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className={`inline-block w-2 h-2 rounded-full ${
                connected ? 'bg-green-400 animate-pulse' :
                disconnectReason === 'replaced' ? 'bg-blue-400' :
                'bg-neutral-600'
              }`} />
              <span className="text-xs text-neutral-400">
                {connected ? '실시간 연결됨' : disconnectReason === 'replaced' ? '다른 창에서 관리 중' : '연결 중...'}
              </span>
            </div>
            <h1 className="text-2xl font-bold text-white">{poll.title}</h1>
            <p className="text-neutral-400 text-sm mt-1">총 {total}명 참여</p>
          </div>
          {poll.status === 'OPEN' && (
            <button
              onClick={handleClose}
              className="px-4 py-2 rounded-xl border border-red-800 text-red-400 hover:bg-red-950 text-sm transition"
            >
              투표 마감
            </button>
          )}
        </div>

        <div className="space-y-4">
          {poll.options.map((opt) => {
            const count = counts[opt.optionId] ?? opt.count
            const pct = total === 0 ? 0 : Math.round((count / total) * 100)
            return (
              <div key={opt.optionId} className="rounded-2xl border border-neutral-800 bg-neutral-900 p-5">
                <div className="flex justify-between items-center mb-3">
                  <span className="font-medium text-white">{opt.content}</span>
                  <div className="text-right">
                    <span className="text-2xl font-bold text-violet-400">{pct}%</span>
                    <span className="text-xs text-neutral-500 ml-2">{count}표</span>
                  </div>
                </div>
                <div className="h-2 bg-neutral-800 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-violet-600 rounded-full transition-all duration-500"
                    style={{ width: `${pct}%` }}
                  />
                </div>
              </div>
            )
          })}
        </div>

        <div className="mt-8 p-4 rounded-xl bg-neutral-900 border border-neutral-800">
          <p className="text-xs text-neutral-500 mb-1">공유 링크</p>
          <p className="text-sm text-neutral-300 font-mono break-all">
            {window.location.origin}/votes/{shareCode}
          </p>
        </div>

        <button
          onClick={() => setShowNewPollModal(true)}
          className="mt-4 w-full py-3 rounded-xl border border-dashed border-neutral-700 text-neutral-400 hover:border-violet-600 hover:text-violet-400 text-sm transition"
        >
          + 새 투표 만들기
        </button>

        {disconnectReason === 'replaced' ? (
          <div className="mt-4 px-4 py-4 rounded-xl border border-blue-800/60 bg-blue-950/30">
            <p className="text-xs text-neutral-300 mb-3">다른 창에서 실시간 연결이 유지되고 있습니다.</p>
            <button
              onClick={() => window.location.reload()}
              className="text-xs px-3 py-1.5 rounded-lg bg-blue-900/60 hover:bg-blue-800/60 text-blue-300 transition"
            >
              이 창으로 가져오기
            </button>
          </div>
        ) : (
          <div className="mt-4 px-4 py-3 rounded-xl border-l-2 border-neutral-700 bg-neutral-900/50">
            <p className="text-xs text-neutral-500 leading-relaxed">
              실시간 연결은 한 번에 하나의 창에서만 유지됩니다.
            </p>
          </div>
        )}
      </div>

      {showNewPollModal && (
        <NewPollModal
          shareCode={shareCode}
          onConfirm={() => navigate('/')}
          onCancel={() => setShowNewPollModal(false)}
        />
      )}
    </div>
  )
}

function NewPollModal({ shareCode, onConfirm, onCancel }) {
  const manageUrl = `${window.location.origin}/votes/${shareCode}/manage`
  const [copied, setCopied] = useState(false)

  const handleCopy = () => {
    navigator.clipboard.writeText(manageUrl)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center px-4 z-50">
      <div className="w-full max-w-sm bg-neutral-900 border border-neutral-700 rounded-2xl p-6">
        <h2 className="text-lg font-semibold text-white mb-2">새 투표 만들기</h2>
        <p className="text-sm text-neutral-400 mb-4">
          관리 페이지는 <span className="text-neutral-200">이 기기에서만</span> 접근할 수 있습니다.<br />
          이동 전에 관리 URL을 저장해두세요.
        </p>
        <div className="flex items-center gap-2 mb-6 p-3 rounded-xl bg-neutral-800 border border-neutral-700">
          <p className="flex-1 text-xs text-neutral-300 font-mono break-all">{manageUrl}</p>
          <button
            onClick={handleCopy}
            className="shrink-0 px-3 py-1.5 rounded-lg bg-neutral-700 hover:bg-neutral-600 text-xs text-neutral-300 transition"
          >
            {copied ? '복사됨 ✓' : '복사'}
          </button>
        </div>
        <div className="flex gap-3">
          <button
            onClick={onCancel}
            className="flex-1 py-2.5 rounded-xl border border-neutral-700 text-neutral-300 hover:bg-neutral-800 text-sm transition"
          >
            취소
          </button>
          <button
            onClick={onConfirm}
            className="flex-1 py-2.5 rounded-xl bg-violet-600 hover:bg-violet-500 text-white font-medium text-sm transition"
          >
            이동하기
          </button>
        </div>
      </div>
    </div>
  )
}

function LoadingScreen() {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="w-8 h-8 border-2 border-violet-500 border-t-transparent rounded-full animate-spin" />
    </div>
  )
}

function ErrorScreen({ message }) {
  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <div className="text-center">
        <p className="text-red-400">{message}</p>
      </div>
    </div>
  )
}
