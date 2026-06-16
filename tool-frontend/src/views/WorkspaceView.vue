<script setup lang="ts">
import { ref, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useModelStore } from '@/stores/model'
import { useProjectSettingsStore } from '@/stores/projectSettings'

import Select from 'primevue/select'
import DiagramList from '@/components/DiagramList.vue'
import DiagramCanvas from '@/components/DiagramCanvas.vue'
import Annotations from '@/components/Annotations.vue'
import SettingsModal from '@/components/SettingsModal.vue'

const modelStore = useModelStore()
const target = ref('plain')
const targetOptions = [
  { label: 'Plain Java', value: 'plain' },
  { label: 'CLI', value: 'cli' },
  { label: 'CLI + JPA', value: 'cli-jpa' },
  { label: 'Spring Boot', value: 'spring' },
]

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
    dockerfile: projectSettings.dockerfile,
    timestamps: projectSettings.timestamps,
  })
}

</script>

<template>
  <div class="workspace">
    <header class="topbar">
      <div class="brand">
        <span class="brand__name">UML-LowCode</span>
        <span v-if="modelStore.fileName" class="brand__sep" aria-hidden="true"></span>
        <code v-if="modelStore.fileName" class="brand__file">{{ modelStore.fileName }}</code>
      </div>

      <div class="actions">
        <span class="target-label">Generation target</span>
        <Select
          v-model="target"
          :options="targetOptions"
          optionLabel="label"
          optionValue="value"
          size="small"
          aria-label="Generation target"
        />
        <button class="btn" @click="startGenerate">Generate</button>
        <button class="btn" @click="modelStore.reset()">Upload another</button>
        <SettingsModal v-model:open="showSettings" :target="target" @confirm="onConfirm" />
      </div>
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
  --accent: var(--p-primary-color, #2f6f87);
  --contrast: var(--p-primary-contrast-color, #ffffff);
  --line: var(--p-content-border-color, var(--color-border, #e4e8ee));
  --ink: var(--p-text-color, var(--color-text, #1f2933));
  --muted: var(--p-text-muted-color, #6b7787);
  --surface: var(--p-content-background, var(--color-secondary, #ffffff));
  --mono: ui-monospace, "JetBrains Mono", "SF Mono", "Fira Code", Menlo, Consolas, monospace;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.6rem 1.25rem;
  border-bottom: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink);
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  min-width: 0;
}
.brand__name {
  font-weight: 700;
  font-size: 1rem;
  letter-spacing: -0.01em;
  white-space: nowrap;
}
.brand__sep {
  flex: none;
  width: 1px;
  height: 16px;
  background: var(--line);
}
.brand__file {
  font-family: var(--mono);
  font-size: 0.78rem;
  color: var(--muted);
  max-width: 26ch;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.target-label {
  font-size: 0.85rem;
  color: var(--muted);
}

.btn {
  font: inherit;
  font-size: 0.85rem;
  color: var(--ink);
  background: color-mix(in srgb, var(--ink) 4%, transparent);
  border: 1px solid var(--line);
  border-radius: 5px;
  cursor: pointer;
  padding: 0.38rem 0.8rem;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}
.btn:hover {
  background: var(--accent);
  border-color: var(--accent);
  color: var(--contrast);
}

.layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}
</style>