<script setup lang="ts">
  import { ref } from 'vue'

  const emit = defineEmits(['file-selected'])

  const inputRef = ref<HTMLInputElement | null>(null)
  const dragging = ref(false)

  function openPicker() {
    inputRef.value?.click()
  }

  function emitFirst(files: FileList | null | undefined) {
    if (files && files.length > 0) emit('file-selected', files[0])
  }

  function onInputChange(e: Event) {
    const target = e.target as HTMLInputElement
    emitFirst(target.files)
    target.value = ''          // allow re-selecting the same file
  }

  function onDrop(e: DragEvent) {
    dragging.value = false
    emitFirst(e.dataTransfer?.files)
  }
</script>

<template>
  <div
    class="dropzone"
    :class="{ 'is-dragging': dragging }"
    role="button"
    tabindex="0"
    aria-label="Choose or drop a UML model"
    @click="openPicker"
    @keydown.enter.prevent="openPicker"
    @keydown.space.prevent="openPicker"
    @dragover.prevent="dragging = true"
    @dragenter.prevent="dragging = true"
    @dragleave.prevent="dragging = false"
    @drop.prevent="onDrop"
  >
    <input
      ref="inputRef"
      type="file"
      class="sr-only"
      accept=".uml,.xmi"
      @change="onInputChange"
    />

    <svg class="dz-glyph" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 15V4m0 0L8.5 7.5M12 4l3.5 3.5" fill="none" stroke="currentColor"
            stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M5 15.5v2.2A1.8 1.8 0 0 0 6.8 19.5h10.4A1.8 1.8 0 0 0 19 17.7v-2.2"
            fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    </svg>

    <p class="dz-primary">Choose UML Model</p>
    <p class="dz-secondary">or drop it here</p>
  </div>
</template>

<style scoped>
.dropzone {
  --accent: var(--p-primary-color, #2f6f87);
  --line: var(--p-content-border-color, #e4e8ee);
  --muted: var(--p-text-muted-color, #6b7787);
  --surface: var(--p-content-background, #ffffff);
  --ink: var(--p-text-color, #1f2933);

  width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2.5rem 1.5rem;
  text-align: center;
  cursor: pointer;
  outline: none;

  border: 1.5px dashed color-mix(in srgb, var(--accent) 26%, var(--line));
  border-radius: 14px;
  background: var(--surface);
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.dropzone:hover,
.dropzone:focus-visible {
  border-color: var(--accent);
}

.dropzone.is-dragging {
  border-style: solid;
  border-color: var(--accent);
  background: color-mix(in srgb, var(--accent) 5%, var(--surface));
}

.dz-glyph {
  width: 30px;
  height: 30px;
  color: var(--accent);
}

.dz-primary {
  margin: 0;
  font-weight: 600;
  font-size: 0.95rem;
  color: var(--ink);
}

.dz-secondary {
  margin: 0;
  font-size: 0.82rem;
  color: var(--muted);
}

.sr-only {
  position: absolute;
  width: 1px; height: 1px;
  padding: 0; margin: -1px;
  overflow: hidden; clip: rect(0, 0, 0, 0);
  white-space: nowrap; border: 0;
}

@media (prefers-reduced-motion: reduce) {
  .dropzone { transition: none; }
}
</style>
