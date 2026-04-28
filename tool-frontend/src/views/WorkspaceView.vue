<script setup lang="ts">
import { useModelStore } from '@/stores/model'

const modelStore = useModelStore()
</script>

<template>
  <main style="padding: 2rem">
    <h1>Workspace</h1>
    <p>Loaded: <strong>{{ modelStore.fileName }}</strong></p>
    <p>Classes: {{ modelStore.model?.classes.length }}</p>
    <p>Associations: {{ modelStore.model?.associations.length }}</p>
    <button @click="modelStore.reset">Upload another</button>
  </main>
</template>