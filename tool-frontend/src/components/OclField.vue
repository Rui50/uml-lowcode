<script setup lang="ts">
import { ref } from 'vue'
import Textarea from 'primevue/textarea'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

const value = defineModel<string>()
defineProps<{
  placeholder?: string
  header?: string
}>()

const open = ref(false)
</script>

<template>
  <div class="ocl-field">
    <Textarea v-model="value" :placeholder="placeholder" rows="3" auto-resize />
    <Button
      icon="pi pi-window-maximize" text rounded size="small"
      class="expand-btn" v-tooltip.left="'Expand'"
      @click="open = true"
    />

    <Dialog v-model:visible="open" modal :header="header ?? 'OCL expression'"
            :style="{ width: '680px' }" dismissableMask>
      <Textarea v-model="value" :placeholder="placeholder" rows="16" autofocus class="ocl-big" />
    </Dialog>
  </div>
</template>

<style scoped>
  .ocl-field { position: relative; }
  .ocl-field :deep(.p-textarea) { width: 100%; padding-right: 30px; }

  .expand-btn {
    position: absolute;
    top: 3px;
    right: 3px;
    width: 24px;
    height: 24px;
    opacity: 0.45;
  }
  .expand-btn:hover { opacity: 1; }

  .ocl-big {
    width: 100%;
    font-family: ui-monospace, 'SF Mono', Menlo, Consolas, monospace;
    font-size: 14px;
    line-height: 1.5;
  }
</style>
