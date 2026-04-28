import type { ParseResponse } from '@/types'

export async function parseModel(file: File): Promise<ParseResponse> {
  const form = new FormData()
  form.append('file', file)
  
  const res = await fetch('/api/model/parse', {
    method: 'POST',
    body: form
  })

  if (!res.ok) {
    throw new Error(`Parse failed (${res.status}): ${res.statusText}`)
  }

  return res.json() as Promise<ParseResponse>
}

