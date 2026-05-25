<script setup lang="ts">
import { useModelStore } from '@/stores/model'
import UploadView from '@/views/UploadView.vue'
import WorkspaceView from '@/views/WorkspaceView.vue'
import Toast from 'primevue/toast'
const modelStore = useModelStore()
</script>
<template>
  <Toast />
  <UploadView v-if="!modelStore.model" />
  <WorkspaceView v-else />
</template>

<style>
/* these are necessary styles for vue flow */
@import '@vue-flow/core/dist/style.css';

/* this contains the default theme, these are optional styles */
@import '@vue-flow/core/dist/theme-default.css';
</style>
