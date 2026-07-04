// SSE(Server-Sent Events) 연결 커스텀 훅
// EventSource로 /votes/{shareCode}/stream 에 연결
// 서버에서 새 집계 데이터가 오면 counts 상태를 업데이트

import { useState, useEffect } from 'react'

export function useSSE(shareCode) {
  const [counts, setCounts] = useState({})
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    // TODO: EventSource 연결 구현
    // 1. EventSource 생성
    const eventSource = new EventSource(`/votes/${shareCode}/stream`)

    // 2. 세가지 핸들러 등록
    eventSource.onopen = () => { setConnected(true) }

    eventSource.onmessage = (event) => {
      const options = JSON.parse(event.data)
      const newCounts = options.reduce((acc, option) => {
        return { ...acc, [option.optionId]: option.count}
      }, {})
      setCounts(newCounts)
    }

    eventSource.onerror = () => { setConnected(false) }

    // cleanup 함수 (다른 페이지로 이동 시)
    return () => eventSource.close() // UseEffect의 return이 cleanup
  }, [shareCode])

  return { counts, connected }
}
