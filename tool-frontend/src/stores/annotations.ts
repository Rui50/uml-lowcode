import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { useModelStore } from './model'

export interface OclAnnotation {
  id: string
  name: string
  expression: string
  message: string
}

export const useAnnotationsStore = defineStore('annotations', () => {
  // key: ClassName -> invariants
  const invariantsByClass = ref<Record<string, OclAnnotation[]>>({})

  // key: "ClassName.attrName" -> init expression
  const initByAttribute = ref<Record<string, string>>({})
  // key: "ClassName.attrName" -> derive expression
  const deriveByAttribute = ref<Record<string, string>>({})

  const operationToSeqDiagram = ref<Record<string, string>>({})

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

  // Storage
  const STORAGE_KEY = 'annotations'

  function loadForModel(name: string | null) {
    if(!name) {
      invariantsByClass.value = {}
      initByAttribute.value = {}
      deriveByAttribute.value = {}
      operationToSeqDiagram.value = {}
      return
    }
    const all = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    const data = all[name] || {}
    invariantsByClass.value = data.invariantsByClass || {}
    initByAttribute.value = data.initByAttribute || {}
    deriveByAttribute.value = data.deriveByAttribute || {}
    operationToSeqDiagram.value = data.operationToSeqDiagram || {}
  }

  function saveForModel(name: string | null) {
    if(!name) return
    const all = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    all[name] = {
      invariantsByClass: invariantsByClass.value,
      initByAttribute: initByAttribute.value,
      deriveByAttribute: deriveByAttribute.value,
      operationToSeqDiagram: operationToSeqDiagram.value,
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
    [invariantsByClass, initByAttribute, deriveByAttribute, operationToSeqDiagram],
    () => saveForModel(modelStore.fileName),
    { deep: true }
  )

  function reset() {
    invariantsByClass.value = {}
    initByAttribute.value = {}
    deriveByAttribute.value = {}
    operationToSeqDiagram.value = {}
  }

  return {
    // state
    invariantsByClass,
    initByAttribute,
    deriveByAttribute,
    operationToSeqDiagram,

    // actions
    addInvariant,
    deleteInvariant,
    addInit,
    deleteInit,
    addDerive,
    deleteDerive,
    linkOperationToSeqDiagram,
    unlinkOperationFromSeqDiagram,
    reset,
  }
})