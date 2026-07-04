import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createPoll } from '../../api/poll'

export default function Home() {
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [options, setOptions] = useState(['', ''])
  const [expiresAt, setExpiresAt] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const addOption = () => {
    if (options.length < 5) setOptions([...options, ''])
  }

  const updateOption = (index, value) => {
    const next = [...options]
    next[index] = value
    setOptions(next)
  }

  const removeOption = (index) => {
    if (options.length <= 2) return
    setOptions(options.filter((_, i) => i !== index))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      // TODO: api/poll.js의 createPoll 호출
      const result = await createPoll(title, options, expiresAt)
      navigate(`/votes/${result.shareCode}/manage?hostToken=${result.hostToken}`)
    } catch (err) {
      setError(err.message || '투표 생성에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-lg">
        <div className="mb-10 text-center">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-violet-600 mb-4">
            <svg className="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
          </div>
          <h1 className="text-3xl font-bold text-white mb-2">새 투표 만들기</h1>
          <p className="text-neutral-400 text-sm">로그인 없이 URL만으로 공유하세요</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-neutral-300 mb-2">투표 제목</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="예) 점심 메뉴 투표"
              required
              className="w-full px-4 py-3 rounded-xl bg-neutral-900 border border-neutral-700 text-white placeholder-neutral-500 focus:outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500 transition"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-300 mb-2">
              선택지 <span className="text-neutral-500 font-normal">({options.length}/5)</span>
            </label>
            <div className="space-y-2">
              {options.map((opt, idx) => (
                <div key={idx} className="flex gap-2">
                  <input
                    type="text"
                    value={opt}
                    onChange={(e) => updateOption(idx, e.target.value)}
                    placeholder={`선택지 ${idx + 1}`}
                    required
                    className="flex-1 px-4 py-3 rounded-xl bg-neutral-900 border border-neutral-700 text-white placeholder-neutral-500 focus:outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500 transition"
                  />
                  {options.length > 2 && (
                    <button
                      type="button"
                      onClick={() => removeOption(idx)}
                      className="px-3 rounded-xl bg-neutral-900 border border-neutral-700 text-neutral-400 hover:text-red-400 hover:border-red-800 transition"
                    >
                      ✕
                    </button>
                  )}
                </div>
              ))}
            </div>
            {options.length < 5 && (
              <button
                type="button"
                onClick={addOption}
                className="mt-2 w-full py-2.5 rounded-xl border border-dashed border-neutral-700 text-neutral-400 hover:border-violet-600 hover:text-violet-400 text-sm transition"
              >
                + 선택지 추가
              </button>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-300 mb-2">마감 일시</label>
            <input
              type="datetime-local"
              value={expiresAt}
              onChange={(e) => setExpiresAt(e.target.value)}
              required
              className="w-full px-4 py-3 rounded-xl bg-neutral-900 border border-neutral-700 text-white focus:outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500 transition [color-scheme:dark]"
            />
          </div>

          {error && (
            <p className="text-red-400 text-sm bg-red-950 border border-red-800 rounded-xl px-4 py-3">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3.5 rounded-xl bg-violet-600 hover:bg-violet-500 disabled:bg-violet-800 disabled:cursor-not-allowed text-white font-semibold text-sm transition"
          >
            {loading ? '생성 중...' : '투표 생성하기'}
          </button>
        </form>
      </div>
    </div>
  )
}
