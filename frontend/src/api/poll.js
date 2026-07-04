// 백엔드 API 호출 함수 모음
// Vite proxy 설정으로 /votes/* 요청은 자동으로 http://localhost:8080으로 전달됨

export async function createPoll(title, options, expiresAt) {
  const res = await fetch('/votes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title, options, expiresAt })
  })

  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.message)
  }

  return res.json();
}

export async function getPoll(shareCode) {
  const res = await fetch(`/votes/${shareCode}`, {
    method: 'GET'
  })

  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.message)
  }

  return res.json()
}

export async function castVote(shareCode, optionId) {
  const res = await fetch(`/votes/${shareCode}/vote`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ optionId })
  })

  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.message)
  }
}

export async function closePoll(shareCode, hostToken) {
  const res = await fetch(`/votes/${shareCode}/close?hostToken=${hostToken}`, {
    method: 'PATCH'
  })

  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.message)
  }
}
