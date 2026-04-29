<script setup lang="ts">
import { useModelStore } from '@/stores/model'
import ClassDetail from '@/components/ClassDetail.vue'

const store = useModelStore()
</script>

<template>
  <aside class="right-panel">
    <div class="empty">
      Select an element in the diagram
    </div>
  </aside>
</template>

<style scoped>
.right-panel {
  border-left: 1px solid var(--color-border);
  background: var(--color-secondary);
  color: var(--color-text);
  width: 360px;
  flex-shrink: 0;
  height: 100%;
  overflow-y: auto;
}
.empty {
  padding: 2rem;
  opacity: 0.5;
  font-style: italic;
}
</style>