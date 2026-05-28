import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { useModelStore } from './model'

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

export const useAnnotationsStore = defineStore('annotations', () => {
  // key: ClassName -> invariants
  const invariantsByClass = ref<Record<string, OclAnnotation[]>>({})

  // key: "ClassName.attrName" -> init expression
  const initByAttribute = ref<Record<string, string>>({})
  // key: "ClassName.attrName" -> derive expression
  const deriveByAttribute = ref<Record<string, string>>({})

  const operationToSeqDiagram = ref<Record<string, string>>({})

  const preconditionsByOperation = ref<Record<string, OclAnnotation[]>>({})

  const postconditionsByOperation = ref<Record<string, OclAnnotation[]>>({})

  // key: ClassName (superclasse) -> "SINGLE_TABLE" | "JOINED"
  const inheritanceStrategyByClass = ref<Record<string, 'SINGLE_TABLE' | 'JOINED'>>({})

  // key: ClassName (raiz) -> config do @Id (ausente = id por defeito auto-gerado)
  const idByClass = ref<Record<string, IdConfig>>({})

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
    [invariantsByClass, initByAttribute, deriveByAttribute, operationToSeqDiagram, preconditionsByOperation, postconditionsByOperation, inheritanceStrategyByClass, idByClass],
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

    // actions
    addInvariant,
    deleteInvariant,
    addInit,
    deleteInit,
    addDerive,
    deleteDerive,
    linkOperationToSeqDiagram,
    unlinkOperationFromSeqDiagram,
    addPrecondition,
    deletePrecondition,
    addPostcondition,
    deletePostcondition,
    setId,
    reset,
  }
})