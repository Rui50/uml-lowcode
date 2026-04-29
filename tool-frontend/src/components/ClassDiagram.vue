<script setup lang="ts">
import { computed, markRaw } from 'vue'
import { useModelStore } from '@/stores/model'
import { VueFlow } from '@vue-flow/core';
import ClassNode from './ClassNode.vue';
import type { ClassDTO, AssociationDTO } from '@/types'
import type { NodeMouseEvent } from '@vue-flow/core'
import { MarkerType } from '@vue-flow/core'
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';


const store = useModelStore()

const classes = computed<ClassDTO[]>(() => store.model?.classes ?? [])
const associations = computed<AssociationDTO[]>(() => store.model?.associations ?? [])

/**
 * umlClass para as classes
 * vou precisar de edges para assoc normais e qualifiers
 */
const nodeTypes = { 
	umlClass: markRaw(ClassNode) 
} as any

// para os nodes nao ficarem em cima dos outros fazemos tipo 4 counas
const nodes = computed(() =>
	classes.value.map((c, i) => ({
		id: c.name,
		type: 'umlClass',
		position: { x: (i % 4) * 220, y: Math.floor(i / 4) * 160 },
		data: c,
	}))
)

const edges = computed(() =>
	associations.value.map((a, i) => ({
		id: `e${i}`,
    type: 'smoothstep',
		source: a.source,
		target: a.target,
		label: `${a.source} ${a.sourceMultiplicity ?? ''}  ──  ${a.targetMultiplicity ?? ''} ${a.target}`,
    markerEnd: a.targetNavigable
      ? { type: MarkerType.ArrowClosed, width: 20, height: 20, color: '#53fc18' }
      : undefined,
    markerStart: a.sourceNavigable
      ? { type: MarkerType.ArrowClosed, width: 20, height: 20, color: '#53fc18' }
      : undefined,
  }))
)

const inheritanceEdges = computed(() =>
  classes.value
    .filter(c => c.parent)
    .map(c => ({  
      id: `i-${c.name}`,
      type: 'smoothstep',
      source: c.name,
      target: c.parent!,
      markerEnd: { type: MarkerType.Arrow, width: 22, height: 22, color: '#888' },
      style: { stroke: '#facc15' },  
    }))
)

const allEdges = computed(() => [...edges.value, ...inheritanceEdges.value])

function onNodeClick(event: NodeMouseEvent) {
  store.selectClass(event.node.id)
}
</script>

<template>
	<div class="flow-wrap">
		<VueFlow
			:nodes="nodes"
      :edges="allEdges"
      :node-types="nodeTypes"
      fit-view-on-init
      @node-click="onNodeClick"
    >
      <Background />
      <Controls />
    </VueFlow>
	</div>
</template>

<style scoped>
.flow-wrap {
	flex: 1;
	min-height: 30vh;
	height: 100%;
}

:deep(.vue-flow__node.selected) .class-node {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px var(--color-primary);
}

:deep(.vue-flow__controls) {
  background: var(--color-secondary);
  border: 1px solid var(--color-border);
}
:deep(.vue-flow__controls-button) {
  background: var(--color-secondary);
  border-bottom: 1px solid var(--color-border);
  fill: var(--color-text);
}
:deep(.vue-flow__controls-button:hover) {
  background: var(--color-secondary-hover);
}
</style>
