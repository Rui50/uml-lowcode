import { useAnnotationsStore } from '@/stores/annotations'
import type { ParseResponse, ProjectSettings } from '@/types'

export async function parseModel(file: File): Promise<ParseResponse> {
  const form = new FormData()
  form.append('file', file)
  
  const res = await fetch('/api/model/parse', {
    method: 'POST',
    body: form
  })

  if (!res.ok) {
    const msg = (await res.text()) || `Erro ao processar o modelo (${res.status})`
    throw new Error(msg)
  }

  return res.json() as Promise<ParseResponse>
}

export async function generateCode(target: string, settings?: ProjectSettings): Promise<Blob> {
  const annotations = useAnnotationsStore()
  const annotationsPayload = {
    invariantsByClass: annotations.invariantsByClass,
    initByAttribute: annotations.initByAttribute,
    deriveByAttribute: annotations.deriveByAttribute,
    operationToSeqDiagram: annotations.operationToSeqDiagram,
    preconditionsByOperation: annotations.preconditionsByOperation,
    postconditionsByOperation: annotations.postconditionsByOperation,
    inheritanceStrategyByClass: annotations.inheritanceStrategyByClass,
    idByClass: annotations.idByClass,
    endpointByOperation: annotations.endpointByOperation,
  }
  const body = JSON.stringify({ annotations: annotationsPayload, settings: settings ?? null })
  const res = await fetch(`/api/model/generate?target=${encodeURIComponent(target)}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body
  })

  if (!res.ok) {
    const msg = (await res.text()) || `Erro ao gerar código (${res.status})`
    throw new Error(msg)
  }

  return res.blob()
}