import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { ParseResponse } from '../types'
import { parseModel } from '@/api/model'

export const useModelStore = defineStore('model', () => {
  const model = ref<ParseResponse | null>(null)
  const fileName = ref<string | null>(null)
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function parseFile(file: File) {
    loading.value = true;
    error.value = null;
    try {
      model.value = await parseModel(file);
      fileName.value = file.name;
    } catch (err) {
      error.value = (err as Error).message;
    } finally {
      loading.value = false;
    }
  }

  function reset(){
    fileName.value = null
    model.value = null
    error.value = null
  }


  return { 
    model,
    fileName,
    reset,  
    parseFile,
    loading,
    error
  }
})
