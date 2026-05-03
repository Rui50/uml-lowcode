<script setup lang="ts">
import { computed } from 'vue'
import { BaseEdge, EdgeLabelRenderer, getSmoothStepPath, Position } from '@vue-flow/core'

const props = defineProps<{
  id: string
  sourceX: number
  sourceY: number
  targetX: number
  targetY: number
  sourcePosition: Position
  targetPosition: Position
  markerEnd?: string
  markerStart?: string
  data?: { 
    sourceLabel?: string; 
    targetLabel?: string 
  }

  // typescript tava a teimar por nao tarem definidos, os abaixo nao sao usados para naada
  sourceNode?: any
  targetNode?: any
  source?: string
  target?: string
  type?: string
  updatable?: boolean
  selected?: boolean
  animated?: boolean
  label?: string
  labelStyle?: any
  labelShowBg?: boolean
  labelBgStyle?: any
  labelBgPadding?: any
  labelBgBorderRadius?: number
  events?: any
  style?: any
  sourceHandleId?: string
  targetHandleId?: string
  interactionWidth?: number
}>()

const path = computed(() => getSmoothStepPath({
  sourceX: props.sourceX,
  sourceY: props.sourceY,
  targetX: props.targetX,
  targetY: props.targetY,
  sourcePosition: props.sourcePosition,
  targetPosition: props.targetPosition,
}))

const pathString = computed(() => path.value[0])
</script>

<template>
  <BaseEdge :id="id" :path="pathString" :marker-end="markerEnd" :marker-start="markerStart" />

  <EdgeLabelRenderer>
    <div
      v-if="data?.sourceLabel"
      class="edge-label"
      :style="{ transform: `translate(-50%, -50%) translate(${sourceX}px, ${sourceY + 14}px)`}"
    >
      {{ data.sourceLabel }}
    </div>
    <div
      v-if="data?.targetLabel"
      class="edge-label"
      :style="{ transform: `translate(-50%, -50%) translate(${targetX}px, ${targetY - 14}px)`}"
    >
      {{ data.targetLabel }}
    </div>
  </EdgeLabelRenderer>
</template>

<style scoped>
.edge-label {
  position: absolute;
  pointer-events: all;
  background: var(--color-secondary);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: 3px;
  padding: 1px 5px;
  font-size: 11px;
  font-family: monospace;
  white-space: nowrap;
  opacity: 0.7;
  transition: opacity 0.15s;
}
.edge-label:hover {
  opacity: 1;
  z-index: 10;
}
</style>