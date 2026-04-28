<script setup lang="ts">
import { ref } from 'vue'
import { useModelStore } from '@/stores/model'

import DiagramList from '@/components/DiagramList.vue'
import DiagramCanvas from '@/components/DiagramCanvas.vue'
import RightPanel from '@/components/RightPanel.vue'

const modelStore = useModelStore()
const target = ref('plain')
</script>

<template>
  <div class="workspace">
    <header class="topbar">
      <h1>UML-LowCode</h1>
      <span class="filename">{{ modelStore.fileName }}</span>

      <select v-model="target">
        <option value="plain">Plain Java</option>
        <option value="cli">CLI</option>
        <option value="spring">Spring Boot</option>
      </select>
      <button @click="modelStore.generate(target)">Generate</button>

      <button @click="modelStore.reset()">Upload another</button>
    </header>
    <div class="layout">
      <DiagramList />
      <DiagramCanvas />
      <RightPanel />
    </div>
  </div>
</template>

<style scoped>
.workspace {
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.topbar {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.5rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-secondary);
}
.topbar h1 {
  font-size: 1.1rem;
  margin: 0;
  color: var(--color-text);
}
.filename {
  color: var(--color-text);
  opacity: 0.7;
  font-size: 0.9rem;
  flex: 1;
}
.topbar button {
  background: var(--color-secondary-hover);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 0.4rem 0.8rem;
  cursor: pointer;
}
.topbar button:hover {
  background: var(--color-primary);
  color: #000;
  border-color: var(--color-primary);
}
.layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}
</style>