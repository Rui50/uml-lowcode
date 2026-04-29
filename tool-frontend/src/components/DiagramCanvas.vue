<script setup lang="ts">
import { useModelStore } from '@/stores/model'
import ClassList from '@/components/ClassList.vue'
import ClassDetail from './ClassDetail.vue';

const store = useModelStore()
</script>

<template>
  <section class="diagram-canvas">
    <div v-if="!store.selectedDiagram" class="empty">
      Select a diagram on the left.
    </div>

    <template v-else-if="store.selectedDiagram.type === 'class'">
      <ClassList />
      <ClassDetail />
    </template>

    <div v-else-if="store.selectedDiagram.type === 'sequence'" class="placeholder">
      <h2>Sequence diagram</h2>
      <p>Interaction id: <code>{{ store.selectedDiagram.interactionId }}</code></p>
    </div>
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
  .placeholder h2 {
    margin-top: 0;
  }
</style>