import { computed } from 'vue'
import { useModelStore } from '@/stores/model'

// estado partilhado pelos separadores do painel de anotações: a classe/interface
// selecionada e derivados. Cada tab importa isto em vez de recalcular.
export function useSelectedClass() {
  const model = useModelStore()

  const selected = computed(() => model.selectedClass ?? model.selectedInterface)
  const isInterface = computed(() => model.selectedKind === 'interface')
  const className = computed(() => selected.value?.name ?? null)   // só para display
  // chave das anotações = xmi:id
  const classKey = computed(() => selected.value?.id ?? null)
  const attributes = computed(() => isInterface.value ? [] : model.selectedClass?.attributes ?? [])
  const operations = computed(() => isInterface.value ? [] : model.selectedClass?.operations ?? [])

  // chave "<classId>.attrName" usada nos mapas de anotação por atributo
  const key = (name: string) => `${classKey.value}.${name}`

  return { selected, isInterface, className, classKey, attributes, operations, key }
}
