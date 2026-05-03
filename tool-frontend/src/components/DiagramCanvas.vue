<script setup lang="ts">
import { ref, computed } from 'vue';
import { useModelStore } from '@/stores/model'
import ClassList from '@/components/ClassList.vue'
import ClassDetail from './ClassDetail.vue';
import ClassDiagram from './ClassDiagram.vue';
import SequenceDiagram from './SequenceDiagram.vue';

const store = useModelStore()
const view = ref<'list' | 'diagram'>('diagram')

const currentInteraction = computed(() => {
  const sel = store.selectedDiagram
  if (sel?.type !== 'sequence') return null
  return store.model?.interactions.find(i => i.id === sel.interactionId) ?? null
})
</script>

<template>
  <section class="diagram-canvas">
    <div v-if="!store.selectedDiagram" class="empty">
      Select a diagram on the left.
    </div>

     <template v-else-if="store.selectedDiagram.type === 'class'">
        <div class="view-toggle">
          <button :class="{ active: view === 'list' }" @click="view = 'list'">List</button>
          <button :class="{ active: view === 'diagram' }" @click="view ='diagram'">Diagram</button>
        </div>

        <div v-if="view === 'list'" class="canvas-body">
          <ClassList />
          <ClassDetail />
        </div>
        <div v-else class="canvas-body">
          <ClassDiagram />
        </div>
      </template>

        <template v-else-if="store.selectedDiagram.type === 'sequence'">
          <div class="canvas-body">
            <SequenceDiagram
              v-if="currentInteraction"
              :key="currentInteraction.id"
              :interaction="currentInteraction"
            />
            <div v-else class="empty">Interaction not found.</div>
          </div>
        </template>
    </section>
  </template>

<style scoped>
  .diagram-canvas {
    flex: 1;
    height: 100%;
    overflow: auto;
    background: var(--background);
    color: var(--color-text);
    display: flex;
    flex-direction: column;
  }
  .empty,
  .placeholder {
    padding: 2rem;
    opacity: 0.7;
  }

  .canvas-body {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }
  .placeholder h2 {
    margin-top: 0;
  }

  .view-toggle {
    display: flex;
    gap: 0.25rem;
    padding: 0.5rem;
    border-bottom: 1px solid var(--color-border);
  }
  .view-toggle button {
    background: var(--color-secondary);
    color: var(--color-text);
    border: 1px solid var(--color-border);
    border-radius: 4px;
    padding: 0.3rem 0.7rem;
    cursor: pointer;
    font-size: 0.85rem;
  }
  .view-toggle button.active {
    background: var(--color-primary);
    color: #000;
    border-color: var(--color-primary);
  }
</style>