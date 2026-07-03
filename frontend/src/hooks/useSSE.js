// SSE(Server-Sent Events) 연결 커스텀 훅
// EventSource로 /votes/{shareCode}/stream?hostToken={hostToken} 에 연결
// 서버에서 새 집계 데이터가 오면 counts 상태를 업데이트

import { useState, useEffect } from 'react'

export function useSSE(shareCode, hostToken) {
  const [counts, setCounts] = useState({})
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    // TODO: EventSource 연결 구현
  }, [shareCode, hostToken])

  return { counts, connected }
}
