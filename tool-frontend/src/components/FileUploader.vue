<script setup lang="ts">
  import FileUpload, { type FileUploadSelectEvent } from 'primevue/fileupload'
  
  const emit = defineEmits(['file-selected'])

  function select(event: FileUploadSelectEvent) {
    const files = event.files as File[]
    if (files.length > 0) emit('file-selected', files[0])
  }
</script>

<template>
  <FileUpload
    mode="basic"
    accept=".uml"
    :auto="false"
    :customUpload="true"
    chooseLabel="Choose UML Model"
    @select="select"
  />
</template>