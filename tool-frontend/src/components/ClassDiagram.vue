<script setup lang="ts">
import { computed, markRaw } from 'vue'
import { useModelStore } from '@/stores/model'
import { VueFlow } from '@vue-flow/core';
import ClassNode from './ClassNode.vue';
import EnumNode from './EnumNode.vue';
import UmlEdge from './UmlEdge.vue';
import type { ClassDTO, AssociationDTO, EnumerationDTO } from '@/types'
import type { NodeMouseEvent } from '@vue-flow/core'
import { MarkerType } from '@vue-flow/core'
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import { associationName } from '@/utils/associationName'

import InterfaceNode from './InterfaceNode.vue'
import type { InterfaceDTO } from '@/types'


const store = useModelStore()

const classes = computed<ClassDTO[]>(() => store.model?.classes ?? [])
const associations = computed<AssociationDTO[]>(() => store.model?.associations ?? [])
const enumerations = computed<EnumerationDTO[]>(() => store.model?.enumerations ?? [])
  const interfaces = computed<InterfaceDTO[]>(() => store.model?.interfaces ?? [])

/**
 * umlClass para as classes
 * vou precisar de edges para assoc normais e qualifiers
 */
const nodeTypes = {
	umlClass: markRaw(ClassNode),
	umlEnum: markRaw(EnumNode),
  umlInterface: markRaw(InterfaceNode),
} as any

const edgeTypes = {
  uml: markRaw(UmlEdge),
} as any

// para os nodes nao ficarem em cima dos outros fazemos tipo 4 counas
const classNodes = computed(() =>
	classes.value.map((c, i) => ({
		id: c.name,
		type: 'umlClass',
		position: { x: (i % 4) * 220, y: Math.floor(i / 4) * 160 },
		data: c,
	}))
)

const enumNodes = computed(() => {
	const baseY = Math.ceil(classes.value.length / 4) * 160
	return enumerations.value.map((e, i) => ({
		id: `enum-${e.name}`,
		type: 'umlEnum',
		position: { x: (i % 4) * 220, y: baseY + Math.floor(i / 4) * 140 },
		data: e,
	}))
})

const interfaceNodes = computed(() => {
  const baseY = Math.ceil(classes.value.length / 4) * 160
              + Math.ceil(enumerations.value.length / 4) * 140
  return interfaces.value.map((it, i) => ({
    id: `iface-${it.name}`,
    type: 'umlInterface',
    position: { x: (i % 4) * 220, y: baseY + Math.floor(i / 4) * 160 },
    data: it,
  }))
})

const nodes = computed(() => [
  ...classNodes.value, ...enumNodes.value, ...interfaceNodes.value,
])

const edges = computed(() =>
  associations.value.map((a, i) => {
    let sourceLabel = ''
    let targetLabel = ''

    if (a.sourceNavigable) {
      const name = associationName(a.source, a.sourceMultiplicity, a.sourceRole)
      const mult = a.sourceMultiplicity ?? ''
      sourceLabel = (name + ' ' + mult).trim()
    }
    if (a.targetNavigable) {
      const name = associationName(a.target, a.targetMultiplicity, a.targetRole)
      const mult = a.targetMultiplicity ?? ''
      targetLabel = (name + ' ' + mult).trim()
    }

    return {
      id: `e${i}`,
      type: 'uml',
      source: a.source,
      target: a.target,
      data: { sourceLabel, targetLabel },
      markerEnd: a.targetNavigable
        ? { type: MarkerType.ArrowClosed, width: 20, height: 20, color: '#53fc18' }
        : undefined,
      markerStart: a.sourceNavigable
        ? { type: MarkerType.ArrowClosed, width: 20, height: 20, color: '#53fc18' }
        : undefined,
    }
  })
)
/*
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
)*/

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

const realizationEdges = computed(() =>
    classes.value.flatMap(c =>
      (c.realizations ?? []).map(iface => ({
        id: `r-${c.name}-${iface}`,
        type: 'smoothstep',
        source: c.name,
        target: `iface-${iface}`,
        markerEnd: { type: MarkerType.Arrow, width: 22, height: 22, color: '#888' },
        style: { stroke: '#888', strokeDasharray: '6 4' },  // dashed para realization
      }))
    )
  )

const allEdges = computed(() => [
  ...edges.value, ...inheritanceEdges.value, ...realizationEdges.value,
])

function onNodeClick(event: NodeMouseEvent) {
    if (event.node.type === 'umlClass') {
      store.selectClass(event.node.id)
    } else if (event.node.type === 'umlInterface') {
      store.selectClass(event.node.data.name)   // interface name
    } else {
      return
    }
  }
</script>

<template>
	<div class="flow-wrap">
		<VueFlow
			:nodes="nodes"
      :edges="allEdges"
      :node-types="nodeTypes"
      :edge-types="edgeTypes"
      fit-view-on-init
      @node-click="onNodeClick"
      @node-drag-start="onNodeClick"
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

:deep(.vue-flow__node.selected) .class-node,
:deep(.vue-flow__node.selected) .iface-node {
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
