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
  data?: {
    sourceMultiplicity?: string
    targetMultiplicity?: string
    sourceRole?: string
    targetRole?: string
  }
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
  markerEnd?: string
  markerStart?: string
}>()

const path = computed(() => getSmoothStepPath(props)[0])

const sourceLabel = computed(() => {
  const m = props.data?.sourceMultiplicity ?? ''
  const r = props.data?.sourceRole ?? ''
  return [m, r].filter(Boolean).join(' ')
})

const targetLabel = computed(() => {
  const m = props.data?.targetMultiplicity ?? ''
  const r = props.data?.targetRole ?? ''
  return [m, r].filter(Boolean).join(' ')
})
</script>

<template>
  <BaseEdge :id="id" :path="path" />
  <EdgeLabelRenderer>
    <div
      v-if="sourceLabel"
      class="end-label nodrag nopan"
      :style="{ transform: `translate(-50%, -50%) translate(${sourceX}px, ${sourceY}px)` }"
    >
      {{ sourceLabel }}
    </div>
    <div
      v-if="targetLabel"
      class="end-label nodrag nopan"
      :style="{ transform: `translate(-50%, -50%) translate(${targetX}px, ${targetY}px)` }"
    >
      {{ targetLabel }}
    </div>
  </EdgeLabelRenderer>
</template>

<style scoped>
.end-label {
  position: absolute;
  background: var(--color-secondary);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: 3px;
  padding: 1px 5px;
  font-size: 11px;
  font-family: monospace;
  pointer-events: all;
  white-space: nowrap;
}
</style>
