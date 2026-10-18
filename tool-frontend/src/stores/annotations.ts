import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { useModelStore } from './model'
import { useProjectSettingsStore } from './projectSettings'
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
  name: string  // @Column(name) override
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
type ResponseOverridesByClass = Record<string, OverridesByEndpointType>           // classId (xmi:id) -> os seus overrides

export const useAnnotationsStore = defineStore('annotations', () => {
  // key: classId (xmi:id) -> invariants
  const invariantsByClass = ref<Record<string, OclAnnotation[]>>({})

  // classId -> (attrName -> init expression)
  const initByAttribute = ref<Record<string, Record<string, string>>>({})
  // classId -> (attrName -> derive expression)
  const deriveByAttribute = ref<Record<string, Record<string, string>>>({})
  // classId -> (attrName -> config de coluna JPA),  ausente = @Column só com name-default
  const columnByAttribute = ref<Record<string, Record<string, ColumnConfig>>>({})

  // classId -> (relationName -> ordenação da coleção),  ausente = sem @OrderBy, ordem indefinida
  const orderByCollection = ref<Record<string, Record<string, { attribute: string; direction: 'ASC' | 'DESC' }>>>({})
  // classId -> (relationName -> fetch de uma relação singular), ausente = LAZY
  const fetchByRelation = ref<Record<string, Record<string, 'LAZY' | 'EAGER'>>>({})

  const operationToSeqDiagram = ref<Record<string, string>>({})

  const preconditionsByOperation = ref<Record<string, OclAnnotation[]>>({})

  const postconditionsByOperation = ref<Record<string, OclAnnotation[]>>({})

  // key: classId (xmi:id, superclasse) -> "SINGLE_TABLE" | "JOINED"
  const inheritanceStrategyByClass = ref<Record<string, 'SINGLE_TABLE' | 'JOINED'>>({})

  // key: classId (xmi:id, raiz) -> config do @Id (ausente = id por defeito auto-gerado)
  const idByClass = ref<Record<string, IdConfig>>({})

  // key: operationId -> config do endpoint REST / job (ausente = POST /<opName>, não-job)
  const endpointByOperation = ref<Record<string, EndpointConfig>>({})

  // key: classId (xmi:id) -> gerar controller REST? (ausente = true)
  const generateControllerByClass = ref<Record<string, boolean>>({})

  // key: classId (xmi:id) -> invariantes OCL viram anotações Bean Validation? (ausente = true)
  const beanValidationByClass = ref<Record<string, boolean>>({})

  // key: classId (xmi:id) -> override do nome da tabela JPA (ausente/vazio = nome derivado da classe)
  const tableNameByClass = ref<Record<string, string>>({})

  // key: classId (xmi:id) -> atributos escondidos por DTO (ausente = DTOs completos, output atual)
  const dtoHiddenByClass = ref<Record<string, DTOHidden>>({})

  // classId -> endpointType -> override do response desse endpoint (ausente = usa o ResponseDTO normal da classe)
  const responseOverrideByClass = ref<ResponseOverridesByClass>({})

  function addInvariant(classId: string) {
    if (!invariantsByClass.value[classId]) invariantsByClass.value[classId] = []
    invariantsByClass.value[classId].push({
      id: crypto.randomUUID(),
      name: '',
      expression: '',
      message: '',
    })
  }

  function deleteInvariant(classId: string, id: string) {
    const invs = invariantsByClass.value[classId]
    if (invs) {
      const idx = invs.findIndex(i => i.id === id)
      if (idx !== -1) invs.splice(idx, 1)
    }
  }

  // pré-preenche uma invariante "<> null" por relação single obrigatória (mult "1") da classe.
  function prefillMandatoryRelations(classId: string) {
    const cls = modelStore.model?.classes.find(c => c.id === classId)
    if (!cls) return
    for (const rel of cls.relations) {
      if (rel.collection || !rel.mandatory) continue
      const invs = (invariantsByClass.value[cls.id] ??= [])
      const expr = `self.${rel.name} <> null`
      if (invs.some(i => i.expression.replace(/\s/g, '') === expr.replace(/\s/g, ''))) continue
      invs.push({
        id: crypto.randomUUID(),
        name: `${rel.name}Required`,
        expression: expr,
        message: `Relação '${rel.name}' é obrigatória (multiplicidade 1)`,
      })
    }
  }

  function addInit(classId: string, name: string) {
    const m = (initByAttribute.value[classId] ??= {})
    m[name] = ''
  }

  function deleteInit(classId: string, name: string) {
    const m = initByAttribute.value[classId]
    if (m) delete m[name]
  }

  function initExpr(classId: string, name: string): string {
    return initByAttribute.value[classId]?.[name] ?? ''
  }

  function setInit(classId: string, name: string, value: string) {
    const m = (initByAttribute.value[classId] ??= {})
    m[name] = value
  }

  function addDerive(classId: string, name: string) {
    const m = (deriveByAttribute.value[classId] ??= {})
    m[name] = ''
  }

  function deleteDerive(classId: string, name: string) {
    const m = deriveByAttribute.value[classId]
    if (m) delete m[name]
  }

  function deriveExpr(classId: string, name: string): string {
    return deriveByAttribute.value[classId]?.[name] ?? ''
  }

  function setDerive(classId: string, name: string, value: string) {
    const m = (deriveByAttribute.value[classId] ??= {})
    m[name] = value
  }

  function addColumnConfig(classId: string, name: string) {
    const m = (columnByAttribute.value[classId] ??= {})
    m[name] = { unique: false, precision: null, scale: null, name: '' }
  }

  function deleteColumnConfig(classId: string, name: string) {
    const m = columnByAttribute.value[classId]
    if (m) delete m[name]
  }

  function orderAttribute(classId: string, name: string): string {
    return orderByCollection.value[classId]?.[name]?.attribute ?? ''
  }

  function orderDirection(classId: string, name: string): 'ASC' | 'DESC' {
    return orderByCollection.value[classId]?.[name]?.direction ?? 'ASC'
  }

  function setOrderAttribute(classId: string, name: string, attr: string) {
    const m = (orderByCollection.value[classId] ??= {})
    if (!attr) { delete m[name]; return }   // — none — = sem @OrderBy
    m[name] = { attribute: attr, direction: m[name]?.direction ?? 'ASC' }
  }

  function toggleOrderDirection(classId: string, name: string) {
    const cur = orderByCollection.value[classId]?.[name]
    if (cur) cur.direction = cur.direction === 'ASC' ? 'DESC' : 'ASC'
  }

  function fetchStrategy(classId: string, name: string): 'LAZY' | 'EAGER' {
    return fetchByRelation.value[classId]?.[name] ?? 'LAZY'
  }

  function setFetchStrategy(classId: string, name: string, value: 'LAZY' | 'EAGER') {
    const m = (fetchByRelation.value[classId] ??= {})
    m[name] = value
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

  function setId(classId: string, config: IdConfig | null) {
    if (config) idByClass.value[classId] = config
    else delete idByClass.value[classId]
  }

  function isDTOHidden(classId: string, role: DTORole, attr: string): boolean {
    return dtoHiddenByClass.value[classId]?.[role]?.includes(attr) ?? false
  }

  function toggleDTOHidden(classId: string, role: DTORole, attr: string) {
    const entry = (dtoHiddenByClass.value[classId] ??= { create: [], update: [], response: [] })
    const list = entry[role]
    const i = list.indexOf(attr)
    if (i === -1) list.push(attr)
    else list.splice(i, 1)
    // sem nada escondido, remove a entrada (payload mínimo; ausente = default)
    if (!entry.create.length && !entry.update.length && !entry.response.length) {
      delete dtoHiddenByClass.value[classId]
    }
  }

  function hasResponseOverride(classId: string, endpointType: EndpointType): boolean {
    return responseOverrideByClass.value[classId]?.[endpointType] !== undefined
  }

  // liga o override do endpointType (se ainda não existe); começa com tudo visível e nome auto
  function ensureResponseOverride(classId: string, endpointType: EndpointType) {
    const entry = (responseOverrideByClass.value[classId] ??= {})
    if (entry[endpointType] === undefined) entry[endpointType] = { name: '', hidden: [] }
  }

  function removeResponseOverride(classId: string, endpointType: EndpointType) {
    const entry = responseOverrideByClass.value[classId]
    if (!entry) return
    delete entry[endpointType]
    if (Object.keys(entry).length === 0) delete responseOverrideByClass.value[classId]
  }

  function overrideName(classId: string, endpointType: EndpointType): string {
    return responseOverrideByClass.value[classId]?.[endpointType]?.name ?? ''
  }

  function setOverrideName(classId: string, endpointType: EndpointType, name: string) {
    const ovr = responseOverrideByClass.value[classId]?.[endpointType]
    if (ovr) ovr.name = name
  }

  function isOverrideHidden(classId: string, endpointType: EndpointType, attr: string): boolean {
    return responseOverrideByClass.value[classId]?.[endpointType]?.hidden.includes(attr) ?? false
  }

  function toggleOverrideHidden(classId: string, endpointType: EndpointType, attr: string) {
    const list = responseOverrideByClass.value[classId]?.[endpointType]?.hidden
    if (!list) return
    const i = list.indexOf(attr)
    if (i === -1) list.push(attr)
    else list.splice(i, 1)
  }


  // Storage
  const STORAGE_KEY = 'annotations'

  // todas as anotações (para o localStorage e para exportar)
  function toJSON() {
    return {
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
      fetchByRelation: fetchByRelation.value,
    }
  }

  // o invers
  function fromJSON(data: Record<string, any>) {
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
    fetchByRelation.value = data.fetchByRelation || {}
  }

  function loadForModel(name: string | null) {
    if(!name) {
      fromJSON({})
      return
    }
    const all = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    fromJSON(all[name] || {})
  }

  function saveForModel(name: string | null) {
    if(!name) return
    const all = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    all[name] = toJSON()
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
    [invariantsByClass, initByAttribute, deriveByAttribute, operationToSeqDiagram, preconditionsByOperation, postconditionsByOperation, inheritanceStrategyByClass, idByClass, endpointByOperation, generateControllerByClass, beanValidationByClass, tableNameByClass, dtoHiddenByClass, responseOverrideByClass, columnByAttribute, orderByCollection, fetchByRelation],
    () => saveForModel(modelStore.fileName),
    { deep: true }
  )

  function reset() {
    fromJSON({})
  }

  // ficheiro de anotações: model + anotações + settings
  function exportToFile(fileName: string | null) {
    const settings = useProjectSettingsStore()
    const data = {
      version: 1,
      model: fileName ?? '',
      exportedAt: new Date().toISOString(),
      annotations: toJSON(),
      settings: {
        basePackage: settings.basePackage,
        applicationName: settings.applicationName,
        databaseType: settings.databaseType,
        serverPort: settings.serverPort,
        openApi: settings.openApi,
        dockerfile: settings.dockerfile,
        timestamps: settings.timestamps,
        demoFrontend: settings.demoFrontend,
      },
    }
    const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }))
    const a = document.createElement('a')
    a.href = url
    a.download = (fileName?.replace(/\.[^.]+$/, '') || 'model') + '-annotations.json'
    a.click()
    URL.revokeObjectURL(url)
  }

  // devolve o nome do modelo que estava no ficheiro, para quem chama avisar se não for o mesmo
  async function importFromFile(file: File, fileName: string | null): Promise<string> {
    const data = JSON.parse(await file.text())
    fromJSON(data.annotations || {})
    if (data.settings) {
      const settings = useProjectSettingsStore()
      Object.assign(settings, data.settings)
    }
    saveForModel(fileName)
    return data.model || ''
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
    fetchByRelation,

    // actions
    addInvariant,
    deleteInvariant,
    prefillMandatoryRelations,
    addInit,
    deleteInit,
    initExpr,
    setInit,
    addDerive,
    deleteDerive,
    deriveExpr,
    setDerive,
    addColumnConfig,
    deleteColumnConfig,
    orderAttribute,
    orderDirection,
    setOrderAttribute,
    toggleOrderDirection,
    fetchStrategy,
    setFetchStrategy,
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
    exportToFile,
    importFromFile,
  }
})
