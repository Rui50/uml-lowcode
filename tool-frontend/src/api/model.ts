import { useAnnotationsStore, type OclAnnotation } from '@/stores/annotations'
import type { ParseResponse, ProjectSettings } from '@/types'

// remove entradas OCL com expressão vazia (linhas adicionadas mas não preenchidas) para não
// enviarem garbage ao backend nem abortarem a geração com "Expressão OCL vazia"
function trimEmptyOclStuff(map: Record<string, OclAnnotation[]>): Record<string, OclAnnotation[]> {
  const out: Record<string, OclAnnotation[]> = {}
  for (const [key, list] of Object.entries(map)) {
    const kept = list.filter(o => o.expression && o.expression.trim() !== '')
    if (kept.length > 0) out[key] = kept
  }
  return out
}

// versão para init/derive, que são Record<string, string> (valor = expressão OCL)
function trimEmptyStrStuff(map: Record<string, string>): Record<string, string> {
  const out: Record<string, string> = {}
  for (const [key, value] of Object.entries(map)) {
    if (value && value.trim() !== '') out[key] = value
  }
  return out
}

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
    invariantsByClass: trimEmptyOclStuff(annotations.invariantsByClass),
    initByAttribute: trimEmptyStrStuff(annotations.initByAttribute),
    deriveByAttribute: trimEmptyStrStuff(annotations.deriveByAttribute),
    operationToSeqDiagram: annotations.operationToSeqDiagram,
    preconditionsByOperation: trimEmptyOclStuff(annotations.preconditionsByOperation),
    postconditionsByOperation: trimEmptyOclStuff(annotations.postconditionsByOperation),
    inheritanceStrategyByClass: annotations.inheritanceStrategyByClass,
    idByClass: annotations.idByClass,
    endpointByOperation: annotations.endpointByOperation,
    generateControllerByClass: annotations.generateControllerByClass,
    beanValidationByClass: annotations.beanValidationByClass,
    tableNameByClass: annotations.tableNameByClass,
    dtoHiddenByClass: annotations.dtoHiddenByClass,
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