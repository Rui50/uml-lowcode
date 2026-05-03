<script setup lang="ts">
  import FileUploader from '@/components/FileUploader.vue';
  import { useModelStore } from '@/stores/model';
  import ProgressSpinner from 'primevue/progressspinner';
  import Message from 'primevue/message';

  const modelStore = useModelStore();

  function selectFile(file: File) {
    modelStore.parseFile(file);
  }
</script>

<template>
  <main class="page">
    <!-- por as regras aqui depois -->
    <p>Upload a UML model (XMI from Visual Paradigm) to begin.</p>
    <FileUploader @file-selected="selectFile" />

    <ProgressSpinner v-if="modelStore.loading" />

    <Message v-if="modelStore.error" severity="error" :closable="false">
      {{ modelStore.error }}
    </Message>  
  </main>
</template>

<style scoped>
.page {
  max-width: 600px;
  margin: 4rem auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
  text-align: center;
}
</style>