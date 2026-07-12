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

    // 서버가 새 탭 연결을 감지해 기존 탭을 의도적으로 교체할 때 전송하는 이벤트
    // name: "close"로 지정된 이벤트라 addEventListener로만 수신 가능 (onmessage는 name 없는 기본 이벤트만 수신)
    // eventSource.close()를 직접 호출해야 브라우저의 자동 재연결을 막을 수 있음
    // close() 없이 그냥 두면 서버가 complete()해도 브라우저가 재연결을 시도 → 핑퐁 현상 발생
    eventSource.addEventListener('close', () => {
      setConnected(false)
      eventSource.close()
    })

    // cleanup 함수 (다른 페이지로 이동 시)
    return () => eventSource.close() // UseEffect의 return이 cleanup
  }, [shareCode])

  return { counts, connected }
}
