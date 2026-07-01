import { ref } from 'vue'
import { defineStore } from 'pinia'

// Estado de apresentação do canvas (não faz parte do modelo UML): posições dos nós
// arrastadas pelo utilizador, por id de nó. Persistidas em localStorage para sobreviverem
// a reload / re-upload do mesmo modelo. Guardamos só ao largar o nó (node-drag-stop),
// não a cada movimento.
const POSITIONS_KEY = 'uml-node-positions'

export const useLayoutStore = defineStore('layout', () => {
  const nodePositions = ref<Record<string, { x: number; y: number }>>(
    JSON.parse(localStorage.getItem(POSITIONS_KEY) ?? '{}')
  )

  function setNodePosition(id: string, pos: { x: number; y: number }) {
    nodePositions.value[id] = pos
    localStorage.setItem(POSITIONS_KEY, JSON.stringify(nodePositions.value))
  }

  function clear() {
    nodePositions.value = {}
    localStorage.removeItem(POSITIONS_KEY)
  }

  return { nodePositions, setNodePosition, clear }
})
