<script setup lang="ts">
import { Handle, Position } from '@vue-flow/core'
import type { EnumerationDTO } from '@/types'

defineProps<{ data: EnumerationDTO }>()
</script>

<template>
  <div class="enum-node">
    <Handle type="target" :position="Position.Top" />

    <div class="header">
      <div class="stereotype">«enumeration»</div>
      <div class="enum-name">{{ data.name }}</div>
    </div>

    <ul class="compartment literals">
      <li v-for="lit in data.literals" :key="lit">{{ lit }}</li>
      <li v-if="!data.literals.length" class="empty">—</li>
    </ul>

    <Handle type="source" :position="Position.Bottom" />
  </div>
</template>

<style scoped>
.enum-node {
  background: var(--color-secondary);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  min-width: 160px;
  font-size: 12px;
  overflow: hidden;
}
.header {
  background: var(--color-secondary-hover);
  padding: 6px 8px;
  text-align: center;
  border-bottom: 2px solid var(--color-border);
}
.stereotype {
  font-size: 10px;
  opacity: 0.6;
  font-style: italic;
}
.enum-name {
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.3px;
}
.compartment {
  list-style: none;
  margin: 0;
  padding: 4px 8px;
  min-height: 18px;
}
.compartment li {
  padding: 2px 0;
  font-family: monospace;
  font-size: 11px;
}
.empty {
  opacity: 0.4;
}
</style>
