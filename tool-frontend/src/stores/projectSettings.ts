import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useProjectSettingsStore = defineStore('projectSettings', () => {
  const basePackage = ref('com.example.app')
  const applicationName = ref('my-app')
  const databaseType = ref<'H2' | 'POSTGRESQL' | 'MYSQL'>('H2')
  const serverPort = ref<number>(8080)
  return { basePackage, applicationName, databaseType, serverPort }
})
