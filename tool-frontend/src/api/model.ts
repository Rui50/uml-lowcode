import { useAnnotationsStore, type OclAnnotation, type ColumnConfig } from '@/stores/annotations'
import type { ParseResponse, ProjectSettings } from '@/types'

// remove entradas OCL com expressão vazia (linhas adicionadas mas não preenchidas)
// se enviasse aparecia "Expressão OCL vazia"
function trimEmptyOclStuff(map: Record<string, OclAnnotation[]>): Record<string, OclAnnotation[]> {
  const out: Record<string, OclAnnotation[]> = {}
  for (const [key, list] of Object.entries(map)) {
    const kept = list.filter(o => o.expression && o.expression.trim() !== '')
    if (kept.length > 0) out[key] = kept
  }
  return out
}

// init/derive: classId -> (attr -> expressão OCL); remove expressões vazias e classes sem nada
function trimEmptyStrStuff(map: Record<string, Record<string, string>>): Record<string, Record<string, string>> {
  const out: Record<string, Record<string, string>> = {}
  for (const [classId, byAttr] of Object.entries(map)) {
    const inner: Record<string, string> = {}
    for (const [name, value] of Object.entries(byAttr)) {
      if (value && value.trim() !== '') inner[name] = value
    }
    if (Object.keys(inner).length) out[classId] = inner
  }
  return out
}

// remove entradas de coluna totalmente vazias (linha seed mas nada definido pelo user)
function trimEmptyColumnStuff(map: Record<string, Record<string, ColumnConfig>>): Record<string, Record<string, ColumnConfig>> {
  const out: Record<string, Record<string, ColumnConfig>> = {}
  for (const [classId, byAttr] of Object.entries(map)) {
    const inner: Record<string, ColumnConfig> = {}
    for (const [name, v] of Object.entries(byAttr)) {
      if (v.unique || v.precision != null || v.scale != null || (v.name && v.name.trim() !== '')) inner[name] = v
    }
    if (Object.keys(inner).length) out[classId] = inner
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
    responseOverrideByClass: annotations.responseOverrideByClass,
    columnByAttribute: trimEmptyColumnStuff(annotations.columnByAttribute),
    orderByCollection: annotations.orderByCollection,
    fetchByRelation: annotations.fetchByRelation,
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
