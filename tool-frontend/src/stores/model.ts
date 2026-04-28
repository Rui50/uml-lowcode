import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { ParseResponse } from '../types'
import { parseModel } from '@/api/model'

export type SelectedDiagram = 
  | { type: 'class' }
  | { type: 'sequence'; interactionId: string }
  | null

export const useModelStore = defineStore('model', () => {
  const model = ref<ParseResponse | null>(null)
  const fileName = ref<string | null>(null)
  const loading = ref(false);
  const error = ref<string | null>(null);

  const selectedClassName = ref<string | null>(null)
  const selectedDiagram = ref<SelectedDiagram>(null)
  
  const selectedClass = computed(() =>
    model.value?.classes.find(c => c.name === selectedClassName.value) ?? null
  );

  function selectDiagram(d: SelectedDiagram) {
    selectedDiagram.value = d
    // clear element selection when switching diagrams
    selectedClassName.value = null
  }


  async function parseFile(file: File) {
    loading.value = true;
    error.value = null;
    try {
      model.value = await parseModel(file);
      fileName.value = file.name;
      selectedDiagram.value = { type: 'class' }
      console.log('Parsed model:', model.value);
    } catch (err) {
      error.value = (err as Error).message;
    } finally {
      loading.value = false;
    }
  }

  function selectClass(className: string) {
    selectedClassName.value = className;
  }

  function reset(){
    fileName.value = null
    model.value = null
    error.value = null
    selectedClassName.value = null
    selectedDiagram.value = null
  }


  return { 
    model,
    fileName,
    reset,  
    parseFile,
    loading,
    error,
    selectedClassName,
    selectedClass,
    selectClass,
    selectedDiagram,
    selectDiagram
  }
})
