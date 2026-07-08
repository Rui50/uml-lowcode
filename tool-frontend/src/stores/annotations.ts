import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { useModelStore } from './model'
import type { ResponseDTOoverride } from '@/types'

export interface OclAnnotation {
  id: string
  name: string
  expression: string
  message: string
}

export interface IdConfig {
  attributeName: string
  attributeType: string
}

// config de coluna JPA por atributo (persistence tab) — só o que a OCL não exprime
export interface ColumnConfig {
  unique: boolean
  precision: number | null
  scale: number | null
}

export interface EndpointConfig {
  job: boolean
  endpointName: string
  httpMethod: string
  cron: string
}

export type DTORole = 'create' | 'update' | 'response'

// atributos ESCONDIDOS por DTO (nao ta la = nada escondido = DTOs completos)
export interface DTOHidden {
  create: string[]
  update: string[]
  response: string[]
}

// endpoints cujo response pode ter override (os 4 CRUD que devolvem a entidade).
// capitalizados de propósito: o valor É o fragmento do nome <Class><EndpointType>ResponseDTO
export type EndpointType = 'List' | 'Get' | 'Create' | 'Update'

//   {
//     Aluno: { List: { name: 'AlunoSummary', hidden: ['email'] } },
//     Turma: { Create: { name: '', hidden: [] } },
//   }
type OverridesByEndpointType = Partial<Record<EndpointType, ResponseDTOoverride>>  // endpointType -> override (nem todos existem)
type ResponseOverridesByClass = Record<string, OverridesByEndpointType>           // className -> os seus overrides

export const useAnnotationsStore = defineStore('annotations', () => {
  // key: ClassName -> invariants
  const invariantsByClass = ref<Record<string, OclAnnotation[]>>({})

  // key: "ClassName.attrName" -> init expression
  const initByAttribute = ref<Record<string, string>>({})
  // key: "ClassName.attrName" -> derive expression
  const deriveByAttribute = ref<Record<string, string>>({})
  // key: "ClassName.attrName" -> config de coluna JPA (ausente = @Column só com name-default)
  const columnByAttribute = ref<Record<string, ColumnConfig>>({})

  // key "ClassName.relationName" -> ordenação da coleção (ausente = sem @OrderBy, ordem indefinida)
  const orderByCollection = ref<Record<string, { attribute: string; direction: 'ASC' | 'DESC' }>>({})

  const operationToSeqDiagram = ref<Record<string, string>>({})

  const preconditionsByOperation = ref<Record<string, OclAnnotation[]>>({})

  const postconditionsByOperation = ref<Record<string, OclAnnotation[]>>({})

  // key: ClassName (superclasse) -> "SINGLE_TABLE" | "JOINED"
  const inheritanceStrategyByClass = ref<Record<string, 'SINGLE_TABLE' | 'JOINED'>>({})

  // key: ClassName (raiz) -> config do @Id (ausente = id por defeito auto-gerado)
  const idByClass = ref<Record<string, IdConfig>>({})

  // key: operationId -> config do endpoint REST / job (ausente = POST /<opName>, não-job)
  const endpointByOperation = ref<Record<string, EndpointConfig>>({})

  // key: ClassName -> gerar controller REST? (ausente = true)
  const generateControllerByClass = ref<Record<string, boolean>>({})

  // key: ClassName -> invariantes OCL viram anotações Bean Validation? (ausente = true)
  const beanValidationByClass = ref<Record<string, boolean>>({})

  // key: ClassName -> override do nome da tabela JPA (ausente/vazio = nome derivado da classe)
  const tableNameByClass = ref<Record<string, string>>({})

  // key: ClassName -> atributos escondidos por DTO (ausente = DTOs completos, output atual)
  const dtoHiddenByClass = ref<Record<string, DTOHidden>>({})

  // key: ClassName -> endpointType -> override do response DESSE endpoint (lazy: ausente = usa o
  // ResponseDTO da classe). A seleção é independente do default (não é subtração em camadas).
  // override do response por endpoint. Estrutura: className -> endpointType -> override.
  // (ausente em qualquer nível = esse endpoint usa o ResponseDTO normal da classe)
  const responseOverrideByClass = ref<ResponseOverridesByClass>({})

  function addInvariant(className: string) {
    if (!invariantsByClass.value[className]) invariantsByClass.value[className] = []
    invariantsByClass.value[className].push({
      id: crypto.randomUUID(),
      name: '',
      expression: '',
      message: '',
    })
  }

  function deleteInvariant(className: string, id: string) {
    const invs = invariantsByClass.value[className]
    if (invs) {
      const idx = invs.findIndex(i => i.id === id)
      if (idx !== -1) invs.splice(idx, 1)
    }
  }

  function addInit(key: string) {
    initByAttribute.value[key] = ''
  }

  function deleteInit(key: string) {
    delete initByAttribute.value[key]
  }

  function addDerive(key: string) {
    deriveByAttribute.value[key] = ''
  }

  function deleteDerive(key: string) {
    delete deriveByAttribute.value[key]
  }

  function addColumnConfig(key: string) {
    columnByAttribute.value[key] = { unique: false, precision: null, scale: null }
  }

  function deleteColumnConfig(key: string) {
    delete columnByAttribute.value[key]
  }

  function orderAttribute(key: string): string {
    return orderByCollection.value[key]?.attribute ?? ''
  }

  function orderDirection(key: string): 'ASC' | 'DESC' {
    return orderByCollection.value[key]?.direction ?? 'ASC'
  }

  function setOrderAttribute(key: string, attr: string) {
    if (!attr) { delete orderByCollection.value[key]; return }   // — none — = sem @OrderBy
    const cur = orderByCollection.value[key]
    orderByCollection.value[key] = { attribute: attr, direction: cur?.direction ?? 'ASC' }
  }

  function toggleOrderDirection(key: string) {
    const cur = orderByCollection.value[key]
    if (cur) cur.direction = cur.direction === 'ASC' ? 'DESC' : 'ASC'
  }

  function linkOperationToSeqDiagram(operationKey: string, diagramId: string) {
    operationToSeqDiagram.value[operationKey] = diagramId
  }

  function unlinkOperationFromSeqDiagram(operationKey: string) {
    delete operationToSeqDiagram.value[operationKey]
  }

  function addPrecondition(opId: string) {
    if (!preconditionsByOperation.value[opId]) preconditionsByOperation.value[opId] = []
    preconditionsByOperation.value[opId].push({
      id: crypto.randomUUID(),
      name: '',
      expression: '',
      message: '',
    })
  }

  function deletePrecondition(opId: string, id: string) {
    const arr = preconditionsByOperation.value[opId]
    if (arr) {
      const idx = arr.findIndex(p => p.id === id)
      if (idx !== -1) arr.splice(idx, 1)
    }
  }

  function addPostcondition(opId: string) {
    if (!postconditionsByOperation.value[opId]) postconditionsByOperation.value[opId] = []
    postconditionsByOperation.value[opId].push({
      id: crypto.randomUUID(),
      name: '',
      expression: '',
      message: '',
    })
  }

  function deletePostcondition(opId: string, id: string) {
    const arr = postconditionsByOperation.value[opId]
    if (arr) {
      const idx = arr.findIndex(p => p.id === id)
      if (idx !== -1) arr.splice(idx, 1)
    }
  }

  function setId(className: string, config: IdConfig | null) {
    if (config) idByClass.value[className] = config
    else delete idByClass.value[className]
  }

  function isDTOHidden(className: string, role: DTORole, attr: string): boolean {
    return dtoHiddenByClass.value[className]?.[role].includes(attr) ?? false
  }

  function toggleDTOHidden(className: string, role: DTORole, attr: string) {
    const entry = (dtoHiddenByClass.value[className] ??= { create: [], update: [], response: [] })
    const list = entry[role]
    const i = list.indexOf(attr)
    if (i === -1) list.push(attr)
    else list.splice(i, 1)
    // sem nada escondido, remove a entrada (payload mínimo; ausente = default)
    if (!entry.create.length && !entry.update.length && !entry.response.length) {
      delete dtoHiddenByClass.value[className]
    }
  }

  function hasResponseOverride(className: string, endpointType: EndpointType): boolean {
    return responseOverrideByClass.value[className]?.[endpointType] !== undefined
  }

  // liga o override do endpointType (se ainda não existe); começa com tudo visível e nome auto
  function ensureResponseOverride(className: string, endpointType: EndpointType) {
    const entry = (responseOverrideByClass.value[className] ??= {})
    if (entry[endpointType] === undefined) entry[endpointType] = { name: '', hidden: [] }
  }

  function removeResponseOverride(className: string, endpointType: EndpointType) {
    const entry = responseOverrideByClass.value[className]
    if (!entry) return
    delete entry[endpointType]
    if (Object.keys(entry).length === 0) delete responseOverrideByClass.value[className]
  }

  function overrideName(className: string, endpointType: EndpointType): string {
    return responseOverrideByClass.value[className]?.[endpointType]?.name ?? ''
  }

  function setOverrideName(className: string, endpointType: EndpointType, name: string) {
    const ovr = responseOverrideByClass.value[className]?.[endpointType]
    if (ovr) ovr.name = name
  }

  function isOverrideHidden(className: string, endpointType: EndpointType, attr: string): boolean {
    return responseOverrideByClass.value[className]?.[endpointType]?.hidden.includes(attr) ?? false
  }

  function toggleOverrideHidden(className: string, endpointType: EndpointType, attr: string) {
    const list = responseOverrideByClass.value[className]?.[endpointType]?.hidden
    if (!list) return
    const i = list.indexOf(attr)
    if (i === -1) list.push(attr)
    else list.splice(i, 1)
  }


  // Storage
  const STORAGE_KEY = 'annotations'

  function loadForModel(name: string | null) {
    if(!name) {
      invariantsByClass.value = {}
      initByAttribute.value = {}
      deriveByAttribute.value = {}
      operationToSeqDiagram.value = {}
      preconditionsByOperation.value = {}
      postconditionsByOperation.value = {}
      inheritanceStrategyByClass.value = {}
      idByClass.value = {}
      endpointByOperation.value = {}
      generateControllerByClass.value = {}
      beanValidationByClass.value = {}
      tableNameByClass.value = {}
      dtoHiddenByClass.value = {}
      responseOverrideByClass.value = {}
      columnByAttribute.value = {}
      orderByCollection.value = {}
      return
    }
    const all = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    const data = all[name] || {}
    invariantsByClass.value = data.invariantsByClass || {}
    initByAttribute.value = data.initByAttribute || {}
    deriveByAttribute.value = data.deriveByAttribute || {}
    operationToSeqDiagram.value = data.operationToSeqDiagram || {}
    preconditionsByOperation.value = data.preconditionsByOperation || {}
    postconditionsByOperation.value = data.postconditionsByOperation || {}
    inheritanceStrategyByClass.value = data.inheritanceStrategyByClass || {}
    idByClass.value = data.idByClass || {}
    endpointByOperation.value = data.endpointByOperation || {}
    generateControllerByClass.value = data.generateControllerByClass || {}
    beanValidationByClass.value = data.beanValidationByClass || {}
    tableNameByClass.value = data.tableNameByClass || {}
    dtoHiddenByClass.value = data.dtoHiddenByClass || {}
    responseOverrideByClass.value = data.responseOverrideByClass || {}
    columnByAttribute.value = data.columnByAttribute || {}
    orderByCollection.value = data.orderByCollection || {}
  }

  function saveForModel(name: string | null) {
    if(!name) return
    const all = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    all[name] = {
      invariantsByClass: invariantsByClass.value,
      initByAttribute: initByAttribute.value,
      deriveByAttribute: deriveByAttribute.value,
      operationToSeqDiagram: operationToSeqDiagram.value,
      preconditionsByOperation: preconditionsByOperation.value,
      postconditionsByOperation: postconditionsByOperation.value,
      inheritanceStrategyByClass: inheritanceStrategyByClass.value,
      idByClass: idByClass.value,
      endpointByOperation: endpointByOperation.value,
      generateControllerByClass: generateControllerByClass.value,
      beanValidationByClass: beanValidationByClass.value,
      tableNameByClass: tableNameByClass.value,
      dtoHiddenByClass: dtoHiddenByClass.value,
      responseOverrideByClass: responseOverrideByClass.value,
      columnByAttribute: columnByAttribute.value,
      orderByCollection: orderByCollection.value,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(all))
  }

  const modelStore = useModelStore()
  // initial load (if model already loaded somehow)
  loadForModel(modelStore.fileName)

  // reload bucket whenever the model's file name changes
  watch(() => modelStore.fileName, (name) => {
    loadForModel(name)
  })

  // save current bucket on any annotation change
  watch(
    [invariantsByClass, initByAttribute, deriveByAttribute, operationToSeqDiagram, preconditionsByOperation, postconditionsByOperation, inheritanceStrategyByClass, idByClass, endpointByOperation, generateControllerByClass, beanValidationByClass, tableNameByClass, dtoHiddenByClass, responseOverrideByClass, columnByAttribute, orderByCollection],
    () => saveForModel(modelStore.fileName),
    { deep: true }
  )

  function reset() {
    invariantsByClass.value = {}
    initByAttribute.value = {}
    deriveByAttribute.value = {}
    operationToSeqDiagram.value = {}
    preconditionsByOperation.value = {}
    postconditionsByOperation.value = {}
    inheritanceStrategyByClass.value = {}
    idByClass.value = {}
    endpointByOperation.value = {}
    generateControllerByClass.value = {}
    beanValidationByClass.value = {}
    tableNameByClass.value = {}
    dtoHiddenByClass.value = {}
    responseOverrideByClass.value = {}
    columnByAttribute.value = {}
    orderByCollection.value = {}
  }

  return {
    // state
    invariantsByClass,
    initByAttribute,
    deriveByAttribute,
    operationToSeqDiagram,
    preconditionsByOperation,
    postconditionsByOperation,
    inheritanceStrategyByClass,
    idByClass,
    endpointByOperation,
    generateControllerByClass,
    beanValidationByClass,
    tableNameByClass,
    dtoHiddenByClass,
    responseOverrideByClass,
    columnByAttribute,
    orderByCollection,

    // actions
    addInvariant,
    deleteInvariant,
    addInit,
    deleteInit,
    addDerive,
    deleteDerive,
    addColumnConfig,
    deleteColumnConfig,
    orderAttribute,
    orderDirection,
    setOrderAttribute,
    toggleOrderDirection,
    linkOperationToSeqDiagram,
    unlinkOperationFromSeqDiagram,
    addPrecondition,
    deletePrecondition,
    addPostcondition,
    deletePostcondition,
    setId,
    isDTOHidden,
    toggleDTOHidden,
    hasResponseOverride,
    ensureResponseOverride,
    removeResponseOverride,
    overrideName,
    setOverrideName,
    isOverrideHidden,
    toggleOverrideHidden,
    reset,
  }
})
