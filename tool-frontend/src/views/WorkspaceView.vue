<script setup lang="ts">
import ClassList from '@/components/ClassList.vue'
import ClassDetail from '@/components/ClassDetail.vue'

import { useModelStore } from '@/stores/model'

const modelStore = useModelStore()
</script>

<template>
  <div class="workspace">
    <header class="topbar">
      <h1>UML-LowCode</h1>
      <span class="filename">{{ modelStore.fileName }}</span>
      <button @click="modelStore.reset">Upload another</button>
    </header>

    <div class="layout">
      <ClassList />
      <ClassDetail />
    </div>
  </div>
  <!--
  <main style="padding: 2rem">
    <h1>Workspace</h1>
    <p>Loaded: <strong>{{ modelStore.fileName }}</strong></p>
    <p>Classes: {{ modelStore.model?.classes.length }}</p>
    <p>Associations: {{ modelStore.model?.associations.length }}</p>
    <button @click="modelStore.reset">Upload another</button>

  </main>
  -->
</template>