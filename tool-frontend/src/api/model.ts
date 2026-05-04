import { useAnnotationsStore } from '@/stores/annotations'
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

export async function generateCode(target: string): Promise<Blob> {
  const annotations = useAnnotationsStore()
  const payload = {
    invariantsByClass: annotations.invariantsByClass,
    initByAttribute: annotations.initByAttribute,
    deriveByAttribute: annotations.deriveByAttribute,
    operationToSeqDiagram: annotations.operationToSeqDiagram,
    preconditionsByOperation: annotations.preconditionsByOperation,
  }
  const res = await fetch(`/api/model/generate?target=${encodeURIComponent(target)}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  }) 

  if (!res.ok) {
    throw new Error(`Code generation failed (${res.status}): ${res.statusText}`)
  }

  return res.blob()
}