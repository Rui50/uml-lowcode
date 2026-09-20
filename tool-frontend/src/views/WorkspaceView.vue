<script setup lang="ts">
import { ref, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useModelStore } from '@/stores/model'
import { useProjectSettingsStore } from '@/stores/projectSettings'
import { useAnnotationsStore } from '@/stores/annotations'
import { SQL_RESERVED } from '@/constants/sqlReserved'

import Select from 'primevue/select'
import Button from 'primevue/button'
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
const annotations = useAnnotationsStore();
const toast = useToast();

// mostra um toast sempre que o store reporta um erro
watch(() => modelStore.error, (msg) => {
  if (msg) {
    toast.add({ severity: 'error', summary: 'Erro ao gerar', detail: msg, life: 6000 });
    modelStore.error = null;
  }
});

// avisa para classes cujo nome é palavra reservada SQL e que AINDA não têm override de tabela.
// definir um Table name (painel Persistence) ou renomear a classe faz o aviso desaparecer.
function warnReservedNames() {
  if (target.value !== 'spring' && target.value !== 'cli-jpa') return  // só targets com BD
  const offending = (modelStore.model?.classes ?? [])
    .filter(c => SQL_RESERVED.has(c.name.toLowerCase()))
    .filter(c => !annotations.tableNameByClass[c.id]?.trim())         // resolvido = tem Table name (por id, nota #32)
    .map(c => c.name)
  if (offending.length) {
    toast.add({
      severity: 'warn',
      summary: 'Nome reservado em SQL',
      detail: `As classes [${offending.join(', ')}] usam palavras reservadas em SQL — `
            + `define um Table name no painel Persistence (ou renomeia a classe).`,
      life: 8000,
    })
  }
}

function startGenerate() {
  warnReservedNames()
  // todos os targets passam pelo modal para definir o basePackage
  showSettings.value = true
}

function onConfirm() {
  modelStore.generate(target.value, {
    basePackage: projectSettings.basePackage.trim() || undefined,   // vazio -> omitido, backend usa o default
    applicationName: projectSettings.applicationName,
    databaseType: projectSettings.databaseType,
    serverPort: projectSettings.serverPort,
    openApi: projectSettings.openApi,
    dockerfile: projectSettings.dockerfile,
    timestamps: projectSettings.timestamps,
    demoFrontend: projectSettings.demoFrontend,
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
          class="target-select"
        />
        <Button label="Generate" size="small" @click="startGenerate" />
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
.target-select {
  width: 150px;
}

.layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}
</style>
