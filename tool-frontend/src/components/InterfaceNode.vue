<script setup lang="ts">
import { Handle, Position } from '@vue-flow/core'
import type { InterfaceDTO } from '@/types'
const props = defineProps<{ data: InterfaceDTO }>()
</script>

<template>
  <div class="iface-node">
    <Handle type="target" :position="Position.Top" />
    <div class="header">
      <div class="stereotype">«interface»</div>
      <div class="iface-name">{{ data.name }}</div>
    </div>
    <ul class="section operations">
      <li v-for="op in data.operations" :key="op.id">
        {{ op.name }}({{ op.parameters.map(p => `${p.name}: ${p.type}`).join(', ') }}){{ op.returnType ?':' : '' }}
        <span v-if="op.returnType" class="type"> {{ op.returnType }}</span>
      </li>
      <li v-if="!data.operations?.length" class="empty">—</li>
    </ul>
    <Handle type="source" :position="Position.Bottom" />
  </div>
</template>

<style scoped>
.iface-node {
  background: var(--color-secondary);
  border: 1px dashed var(--color-border); 
  border-radius: 4px;
  min-width: 180px;
  font-size: 12px;
}
.header { 
  padding: 6px 8px; text-align: center; border-bottom: 2px solid var(--color-border); 
}
.stereotype { 
  font-size: 10px; opacity: 0.7; 
}
.iface-name { 
  font-weight: 700; font-size: 13px; font-style: italic; 
}
.section { 
  list-style: none; margin: 0; padding: 4px 8px; 
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
</style>