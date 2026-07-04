import { useState, useEffect } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { getPoll, closePoll } from '../../api/poll'
import { useSSE } from '../../hooks/useSSE'

export default function Manage() {
  const { shareCode } = useParams()
  const [searchParams] = useSearchParams()
  const hostToken = searchParams.get('hostToken')
  const { counts, connected } = useSSE(shareCode, hostToken)

  const [poll, setPoll] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    // TODO: api/poll.js의 getPoll 호출 후 setPoll
    const fetch = async () => {
      try {
        const result = await getPoll(shareCode);
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
      // TODO: api/poll.js의 closePoll 호출
      const result = await closePoll(shareCode, hostToken)
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
              <span className={`inline-block w-2 h-2 rounded-full ${connected ? 'bg-green-400 animate-pulse' : 'bg-neutral-600'}`} />
              <span className="text-xs text-neutral-400">{connected ? '실시간 연결됨' : '연결 중...'}</span>
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
