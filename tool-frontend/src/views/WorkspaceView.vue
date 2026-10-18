<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useModelStore } from '@/stores/model'
import { useProjectSettingsStore } from '@/stores/projectSettings'
import { useAnnotationsStore } from '@/stores/annotations'
import { SQL_RESERVED } from '@/constants/sqlReserved'

import Select from 'primevue/select'
import Button from 'primevue/button'
import Menu from 'primevue/menu'
import Dialog from 'primevue/dialog'
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

// avisos do parse e com botão "Diagram Issues" abre um modal.
// dispensar esconde o botão
const showIssues = ref(false);
const issuesDismissed = ref(false);
const issues = computed(() => modelStore.model?.warnings ?? []);
function dismissIssues() {
  issuesDismissed.value = true;
  showIssues.value = false;
}
// novo modelo -> volta a mostrar o botão
watch(() => modelStore.model, () => { issuesDismissed.value = false; });

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

// avisa para classes e atributos cujo nome é palavra reservada SQL e que AINDA não têm override.
// definir um Table/Column name (painel Persistence) ou renomear faz o aviso desaparecer.
function warnReservedNames() {
  if (target.value !== 'spring' && target.value !== 'cli-jpa') return  // só targets com BD
  const offending = (modelStore.model?.classes ?? [])
    .filter(c => SQL_RESERVED.has(c.name.toLowerCase()))
    .filter(c => !annotations.tableNameByClass[c.id]?.trim())         // resolvido = tem Table name
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

  // para atributos em que o nome bate com nome reservado de tabela
  const offendingAttrs = (modelStore.model?.classes ?? [])
    .flatMap(c => c.attributes.map(a => ({ cls: c, attr: a })))
    .filter(({ attr }) => SQL_RESERVED.has(attr.name.toLowerCase()))
    .filter(({ cls, attr }) => !annotations.columnByAttribute[cls.id]?.[attr.name]?.name?.trim())
    .map(({ cls, attr }) => `${cls.name}.${attr.name}`)
  if (offendingAttrs.length) {
    toast.add({
      severity: 'warn',
      summary: 'Nome reservado em SQL',
      detail: `Os atributos [${offendingAttrs.join(', ')}] usam palavras reservadas em SQL — `
            + `define um Column name no painel Persistence (ou renomeia o atributo).`,
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

const importInput = ref<HTMLInputElement | null>(null)

const annMenu = ref()
const annItems = computed(() => [
  { label: 'Export annotations (JSON)', icon: 'pi pi-download',
    command: () => annotations.exportToFile(modelStore.fileName) },
  { label: 'Import annotations (JSON)', icon: 'pi pi-upload',
    command: () => importInput.value?.click() },
])

async function onImport(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const importedModel = await annotations.importFromFile(file, modelStore.fileName)
    if (importedModel && modelStore.fileName && importedModel !== modelStore.fileName) {
      toast.add({ severity: 'warn', summary: 'Different model',
        detail: `These annotations were from model "${importedModel}", not "${modelStore.fileName}". Imported anyway — check they fit.`,
        life: 8000 })
    } else {
      toast.add({ severity: 'success', summary: 'Annotations imported', life: 3000 })
    }
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Import failed', detail: String(err), life: 6000 })
  } finally {
    input.value = ''   // allow re-importing the same file
  }
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

      <Button
        v-if="issues.length && !issuesDismissed"
        :label="`Diagram Issues (${issues.length})`"
        severity="warn" size="small" text
        class="issues-btn"
        @click="showIssues = true"
      />
      <Dialog
        v-model:visible="showIssues" modal header="Diagram Issues"
        :style="{ width: '34rem' }" :dismissableMask="true"
      >
        <ul class="issues-list">
          <li v-for="(w, i) in issues" :key="i">
            <span>{{ w }}</span>
          </li>
        </ul>
        <template #footer>
          <Button label="Dismiss" size="small" text severity="secondary" @click="dismissIssues" />
          <Button label="Close" size="small" text @click="showIssues = false" />
        </template>
      </Dialog>

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
        <Button
          label="Annotations" icon="pi pi-file-export"
          size="small" text severity="secondary"
          :disabled="!modelStore.fileName"
          aria-haspopup="true" aria-controls="ann_menu"
          @click="annMenu.toggle($event)"
        />
        <Menu ref="annMenu" id="ann_menu" :model="annItems" :popup="true" />
        <input ref="importInput" type="file" accept="application/json" hidden @change="onImport" />
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

  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.6rem 1.25rem;
  border-bottom: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink);
}


.issues-btn {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.issues-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  font-size: 0.88rem;
  line-height: 1.45;
}
.issues-list li {
  display: flex;
  gap: 0.55rem;
  align-items: flex-start;
  padding: 0.6rem 0.75rem;
  border-radius: 6px;
  background: var(--color-secondary-hover);
  color: var(--color-text, #1f2933);
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
