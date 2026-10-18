<script setup lang="ts">
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import OclField from './OclField.vue'
import type { OclAnnotation } from '@/stores/annotations'

// lista de entradas OCL (invariante / pré / pós-condição): name + expressão + mensagem + remover.
// as entradas são objetos do store (por referência), por isso o v-model edita-os diretamente.
defineProps<{ entries: OclAnnotation[]; placeholder: string; header: string }>()
defineEmits<{ (e: 'delete', id: string): void }>()
</script>

<template>
  <div v-for="entry in entries" :key="entry.id" class="entry">
    <InputText placeholder="name (optional)" v-model="entry.name" />
    <OclField v-model="entry.expression" :placeholder="placeholder" :header="header" />
    <InputText placeholder="error message (optional)" v-model="entry.message" />
    <Button
      label="Remove" icon="pi pi-trash" size="small" severity="danger" text
      class="remove-btn"
      @click="$emit('delete', entry.id)"
    />
  </div>
</template>

<style scoped>
.entry {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  margin-bottom: 8px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--background);
}
.entry .remove-btn {
  align-self: flex-end;
}
</style>
