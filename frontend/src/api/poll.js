export async function createPoll(title, options, expiresAt) {
  const res = await fetch('/api/votes', {
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

export async function getHostPoll(shareCode) {
  const res = await fetch(`/api/votes/${shareCode}/host`)

  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.message)
  }

  return res.json()
}

export async function getPoll(shareCode) {
  const res = await fetch(`/api/votes/${shareCode}`)

  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.message)
  }

  return res.json()
}

export async function castVote(shareCode, optionId) {
  const res = await fetch(`/api/votes/${shareCode}/vote`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ optionId })
  })

  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.message)
  }
}

export async function closePoll(shareCode) {
  const res = await fetch(`/api/votes/${shareCode}/close`, {
    method: 'PATCH',
  })

  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.message)
  }
}
