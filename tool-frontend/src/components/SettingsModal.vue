<script setup lang="ts">
  import { computed } from 'vue';
  import Dialog from 'primevue/dialog'
  import InputText from 'primevue/inputtext'
  import InputNumber from 'primevue/inputnumber'
  import Select from 'primevue/select'
  import Checkbox from 'primevue/checkbox'
  import Button from 'primevue/button'
  import { useProjectSettingsStore } from '@/stores/projectSettings';

  const props = defineProps<{
    open: boolean;
    target: string
  }>();

  const emit = defineEmits<{
    (event: "update:open", value: boolean): void
    (event: "confirm"): void
  }>();

  const settings = useProjectSettingsStore();

  const showDB = computed(() => props.target === 'spring' || props.target === 'cli-jpa')
  const showPort = computed(() => props.target === 'spring')
  // plain só gera classes; applicationName não se aplica
  const showAppName = computed(() => props.target === 'spring' || props.target === 'cli-jpa')

  const dbOptions = [
    { label: 'H2 (in-memory)', value: 'H2' },
    { label: 'PostgreSQL',     value: 'POSTGRESQL' },
    { label: 'MySQL',          value: 'MYSQL' },
  ]

  function close() { emit('update:open', false) }
  function confirm() { emit('confirm'); close() }
</script>

<template>
  <Dialog :visible="open" @update:visible="emit('update:open', $event)"
          modal header="Project settings" :style="{ width: '420px' }">
    <div class="field">
      <label>Base package</label>
      <InputText v-model="settings.basePackage" />
    </div>
    <div v-if="showAppName" class="field">
      <label>Application name</label>
      <InputText v-model="settings.applicationName" />
    </div>
    <div v-if="showDB" class="field">
      <label>Database</label>
      <Select v-model="settings.databaseType" :options="dbOptions"
              optionLabel="label" optionValue="value" />
    </div>
    <div v-if="showPort" class="field">
      <label>Server port</label>
      <InputNumber v-model="settings.serverPort" :useGrouping="false" />
    </div>
    <div v-if="showPort" class="field-check">
      <Checkbox v-model="settings.openApi" :binary="true" inputId="openApi" />
      <label for="openApi">Include OpenAPI / Swagger docs</label>
    </div>

    <p v-if="showDB" class="db-note">
      Nota: os dados da base de dados têm de ser definidos no
      ficheiro de configuração do projeto gerado antes de o executar.
    </p>

    <template #footer>
      <Button label="Cancel" text @click="close" />
      <Button label="Generate" @click="confirm" />
    </template>
  </Dialog>
</template>

<style scoped>
.field { 
  display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; 
}
.field label {
  font-size: 12px; opacity: 0.8;
}
.field-check {
  display: flex; align-items: center; gap: 8px; margin-bottom: 10px;
}
.field-check label {
  font-size: 12px; opacity: 0.8;
}
.db-note {
  font-size: 11px;
  opacity: 0.7;
  margin: 4px 0 0;
  line-height: 1.4;
}
</style>