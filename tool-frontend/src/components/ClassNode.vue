<script setup lang="ts">
// https://vueflow.dev/examples/nodes/
// https://www.mintlify.com/bcakmakoglu/vue-flow/guides/custom-nodes
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core';
import type { ClassDTO } from '@/types'
import { useAnnotationsStore } from '@/stores/annotations';

const props = defineProps<{
  data: ClassDTO
}>()

/*
function splitOp(op: string) {
  const i = op.lastIndexOf('): ')
  if (i === -1) return { sig: op, ret: '' }
  return { sig: op.slice(0, i + 1), ret: op.slice(i + 3) }
}
*/
const annotations = useAnnotationsStore()

const classAnnotated = computed(() => {
  const cn = props.data.name
  const hasInvs = (annotations.invariantsByClass[cn]?.length ?? 0) > 0
  //const hasAttrs = props.data.attributes.some(a => attrAnnotated(a.name))
  return hasInvs
})

function attrAnnotated(attrName: string): boolean {
  const k = `${props.data.name}.${attrName}`
  return k in annotations.initByAttribute || k in annotations.deriveByAttribute
}
</script>

<template>
    <div class="class-node">
      <Handle type="target" :position="Position.Top" />

      <div class="header">
        <span v-if="classAnnotated" class="dot" />
        <div class="class-name">{{ data.name }}</div>
        <div v-if="data.parent" class="parent">: {{ data.parent }}</div>
      </div>

      <ul class="section attributes">
        <li v-for="a in data.attributes" :key="a.name">
          <span v-if="attrAnnotated(a.name)" class="dot" />
          {{ a.name }}: <span class="type">{{ a.type }}</span>
        </li>
        <li v-if="!data.attributes.length" class="empty">—</li>
      </ul>

      <ul class="section operations">
        <li v-for="op in data.operations" :key="op.id">
          {{ op.name }}({{ op.parameters.map(p => `${p.name}: ${p.type}`).join(', ') }}){{ op.returnType ? ':' : '' }}
          <span v-if="op.returnType" class="type"> {{ op.returnType }}</span>
        </li>
        <li v-if="!data.operations?.length" class="empty">—</li>
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
    min-width: 180px;
    font-size: 12px;
    overflow: hidden;          
  }

  .header {
    background: var(--color-secondary-hover);
    padding: 6px 8px;
    text-align: center;
    border-bottom: 2px solid var(--color-border);
  }
  .class-name {
    font-weight: 700;
    font-size: 13px;
    letter-spacing: 0.3px;
  }
  .parent {
    margin-top: 2px;
    font-size: 11px;
    font-style: italic;
    opacity: 0.7;
  }

  .section {
    list-style: none;
    margin: 0;
    padding: 4px 8px;
    min-height: 18px;         
  }
  .section li { 
    padding: 2px 0; 
  }
  .section + .section {
    border-top: 1px solid var(--color-border);
  }

  .operations li { 
    font-family: monospace; font-size: 11px;
  }

  .type { 
    color: var(--color-primary); font-family: monospace; 
  }
  .empty { 
    opacity: 0.4; 
  }

  .dot {
    display: inline-block;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--color-primary);
    margin-right: 4px;
    vertical-align: middle;
  }
</style>