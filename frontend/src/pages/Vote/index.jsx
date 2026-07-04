import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { castVote, getPoll } from '../../api/poll'

export default function Vote() {
  const { shareCode } = useParams()
  const [poll, setPoll] = useState(null)
  const [selected, setSelected] = useState(null)
  const [submitted, setSubmitted] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    // TODO: api/poll.js의 getPoll 호출
    const fetch = async () => {
      try {
        const result = await getPoll(shareCode);
        if (result.isHost) {
          navigate(`/votes/${shareCode}/manage`, { replace: true }) // 뒤로가기로 투표페이지 돌아오는 것 방지
          return
        }
        setPoll(result)
        if (result.hasVoted) setSubmitted(true)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }
    fetch()
  }, [shareCode])

  const handleVote = async () => {
    if (!selected) return
    try {
      // TODO: api/poll.js의 castVote 호출
      const result = await castVote(shareCode, selected)
      const updated = await getPoll(shareCode) // 재조회
      setPoll(updated) // 최신 집계로 갱신
      setSubmitted(true)
    } catch (err) {
      setError(err.message || '투표에 실패했습니다.')
    }
  }

  if (loading) return <LoadingScreen />
  if (error) return <ErrorScreen message={error} />
  if (!poll) return null

  const total = poll.options.reduce((sum, o) => sum + o.count, 0)

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-lg">
        <div className="mb-8">
          <span className="inline-block text-xs font-medium text-violet-400 bg-violet-950 border border-violet-800 px-3 py-1 rounded-full mb-4">
            {poll.status === 'CLOSED' ? '마감됨' : '진행 중'}
          </span>
          <h1 className="text-2xl font-bold text-white mb-1">{poll.title}</h1>
          <p className="text-neutral-400 text-sm">총 {total}명 참여</p>
        </div>

        {submitted || poll.status === 'CLOSED' ? (
          <ResultView
            options={poll.options}
            total={total}
            submitted={submitted}
            onRefresh={async () => {
              const updated = await getPoll(shareCode)
              setPoll(updated)
            }}
          />
        ) : (
          <VoteView
            options={poll.options}
            selected={selected}
            onSelect={setSelected}
            onSubmit={handleVote}
          />
        )}
      </div>
    </div>
  )
}

function VoteView({ options, selected, onSelect, onSubmit }) {
  return (
    <div className="space-y-3">
      {options.map((opt) => (
        <button
          key={opt.optionId}
          onClick={() => onSelect(opt.optionId)}
          className={`w-full px-5 py-4 rounded-xl border text-left transition font-medium text-sm cursor-pointer
            ${selected === opt.optionId
              ? 'border-violet-500 bg-violet-950 text-white'
              : 'border-neutral-700 bg-neutral-900 text-neutral-300 hover:border-neutral-500'
            }
          `}
        >
          {opt.content}
        </button>
      ))}
      <div className="mt-4">
        <button
          onClick={onSubmit}
          disabled={!selected}
          className="w-full py-3.5 rounded-xl bg-violet-600 hover:bg-violet-500 disabled:bg-neutral-800 disabled:text-neutral-500 disabled:cursor-not-allowed text-white font-semibold text-sm transition"
        >
          투표하기
        </button>
      </div>
    </div>
  )
}

function ResultView({ options, total, submitted, onRefresh }) {
  const [refreshing, setRefreshing] = useState(false)

  const handleRefresh = async () => {
    setRefreshing(true)
    try {
      await onRefresh()
    } finally {
      setRefreshing(false)
    }
  }

  return (
    <div className="space-y-3">
      {submitted && (
        <p className="text-center text-violet-400 text-sm font-medium mb-6">투표가 완료되었습니다</p>
      )}
      {options.map((opt) => {
        const pct = total === 0 ? 0 : Math.round((opt.count / total) * 100)
        return (
          <div key={opt.optionId} className="rounded-xl border border-neutral-700 bg-neutral-900 overflow-hidden">
            <div className="px-5 py-3 flex justify-between items-center">
              <span className="text-sm font-medium text-white">{opt.content}</span>
              <span className="text-sm text-violet-400 font-semibold">{pct}%</span>
            </div>
            <div className="h-1 bg-neutral-800">
              <div
                className="h-full bg-violet-600 transition-all duration-700"
                style={{ width: `${pct}%` }}
              />
            </div>
          </div>
        )
      })}
      {submitted && onRefresh && (
        <button
          onClick={handleRefresh}
          disabled={refreshing}
          className="w-full py-3.5 rounded-xl border border-neutral-700 bg-neutral-900 hover:border-neutral-500 disabled:opacity-50 disabled:cursor-not-allowed text-neutral-300 text-sm transition"
        >
          {refreshing ? '새로고침 중...' : '↻ 새로고침'}
        </button>
      )}
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
        <p className="text-red-400 mb-2">{message}</p>
      </div>
    </div>
  )
}
