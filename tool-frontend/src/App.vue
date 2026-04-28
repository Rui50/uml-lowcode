<script setup lang="ts">
import { useModelStore } from '@/stores/model'
import UploadView from '@/views/UploadView.vue'
import WorkspaceView from '@/views/WorkspaceView.vue'
const modelStore = useModelStore()
</script>
<template>
  <UploadView v-if="!modelStore.model" />
  <WorkspaceView v-else />
</template>

<style scoped></style>
