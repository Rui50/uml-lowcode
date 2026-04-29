<script setup lang="ts">
// https://vueflow.dev/examples/nodes/
// https://www.mintlify.com/bcakmakoglu/vue-flow/guides/custom-nodes
import { Handle, Position } from '@vue-flow/core';
import type { ClassDTO } from '@/types'

const props = defineProps<{
  data: ClassDTO
}>()
</script>

<template>
  <div class="class-node">
    <Handle type="target" :position="Position.Top" />

    <div class="class-name">{{ data.name }}</div>    

    <div v-if="data.parent" class="parent">: {{ data.parent }}</div>

    <ul class="attributes">
      <li v-for="a in data.attributes" :key="a.name">
          {{ a.name }}: <span class="type">{{ a.type }}</span>
        </li>
        <li v-if="!data.attributes.length" class="empty">—</li>
    </ul>

    <ul class="operations">
      <li v-for="op in data.operations" :key="op">{{ op }}</li>
    </ul>
    
    <Handle type="source" :position="Position.Bottom" />
  </div>
</template>

<style scoped>
  .class-node {
    background: var(--color-secondary);
    color: var(--color-text);
    border: 1px solid var(--color-border);
    border-radius: 4px;
    min-width: 160px;
    font-size: 12px;
  }
  .header {
    background: var(--color-secondary-hover);
    padding: 4px 8px;
    font-weight: 600;
    border-bottom: 1px solid var(--color-border);
  }
  .parent {
    padding: 2px 8px;
    opacity: 0.7;
    font-style: italic;
    border-bottom: 1px solid var(--color-border);
  }
  .attributes {
    list-style: none;
    margin: 0;
    padding: 4px 8px;
  }
  .attributes li {
    padding: 2px 0;
  }
  .type {
    color: var(--color-primary);
    font-family: monospace;
  }
  .empty { opacity: 0.4; }

  .operations {
    list-style: none;
    margin: 0;
    padding: 4px 8px;
    border-top: 1px solid var(--color-border);
  }
  .operations li {
    padding: 2px 0;
    font-family: monospace;
    font-size: 11px;
  }
  .operations .empty {
    opacity: 0.4;
  }
</style>