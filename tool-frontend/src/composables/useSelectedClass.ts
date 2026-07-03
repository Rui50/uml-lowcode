import { computed } from 'vue'
import { useModelStore } from '@/stores/model'

// estado partilhado pelos separadores do painel de anotações: a classe/interface
// selecionada e derivados. Cada tab importa isto em vez de recalcular.
export function useSelectedClass() {
  const model = useModelStore()

  const selected = computed(() => model.selectedClass ?? model.selectedInterface)
  const isInterface = computed(() => model.selectedKind === 'interface')
  const className = computed(() => selected.value?.name ?? null)
  const attributes = computed(() => isInterface.value ? [] : model.selectedClass?.attributes ?? [])
  const operations = computed(() => isInterface.value ? [] : model.selectedClass?.operations ?? [])

  // chave "ClassName.attrName" usada nos mapas de anotação por atributo
  const key = (name: string) => `${className.value}.${name}`

  return { selected, isInterface, className, attributes, operations, key }
}
