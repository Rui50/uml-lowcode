import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useProjectSettingsStore = defineStore('projectSettings', () => {
  const basePackage = ref('com.example.app')
  const applicationName = ref('my-app')
  const databaseType = ref<'H2' | 'POSTGRESQL' | 'MYSQL'>('H2')
  const serverPort = ref<number>(8080)
  const openApi = ref<boolean>(true)
  const dockerfile = ref<boolean>(false)
  const timestamps = ref<boolean>(false)
  const demoFrontend = ref<boolean>(false)
  return { basePackage, applicationName, databaseType, serverPort, openApi, dockerfile, timestamps, demoFrontend }
})
