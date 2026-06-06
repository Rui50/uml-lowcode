<script setup lang="ts">
import { ref, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useModelStore } from '@/stores/model'
import { useProjectSettingsStore } from '@/stores/projectSettings'

import DiagramList from '@/components/DiagramList.vue'
import DiagramCanvas from '@/components/DiagramCanvas.vue'
import Annotations from '@/components/Annotations.vue'
import SettingsModal from '@/components/SettingsModal.vue'

const modelStore = useModelStore()
const target = ref('plain')

const showSettings = ref(false);
const projectSettings = useProjectSettingsStore();
const toast = useToast();

// mostra um toast sempre que o store reporta um erro
watch(() => modelStore.error, (msg) => {
  if (msg) {
    toast.add({ severity: 'error', summary: 'Erro ao gerar', detail: msg, life: 6000 });
    modelStore.error = null;
  }
});

function startGenerate() {
  // cli já usa um package fixo; plain/cli-jpa/spring passam pelo modal para definir o basePackage
  if (target.value === 'cli') {
    modelStore.generate(target.value)
  } else {
    showSettings.value = true
  }
}

function onConfirm() {
  modelStore.generate(target.value, {
    basePackage: projectSettings.basePackage,
    applicationName: projectSettings.applicationName,
    databaseType: projectSettings.databaseType,
    serverPort: projectSettings.serverPort,
    openApi: projectSettings.openApi,
  })
}

</script>

<template>
  <div class="workspace">
    <header class="topbar">
      <h1>UML-LowCode</h1>
      <span class="filename">{{ modelStore.fileName }}</span>

      <select v-model="target">
        <option value="plain">Plain Java</option>
        <option value="cli">CLI</option>
        <option value="cli-jpa">CLI + JPA</option>
        <option value="spring">Spring Boot</option>
      </select>
      <button @click="startGenerate">Generate</button>
      <SettingsModal v-model:open="showSettings" :target="target" @confirm="onConfirm" />

      <button @click="modelStore.reset()">Upload another</button>
    </header>
    <div class="layout">
      <DiagramList />
      <DiagramCanvas />
      <Annotations />
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