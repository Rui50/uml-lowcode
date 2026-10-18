<script setup lang="ts">
import { ref } from 'vue'
import { useSelectedClass } from '@/composables/useSelectedClass'
import OclTab from './OclTab.vue'
import PersistenceTab from './PersistenceTab.vue'
import SpringTab from './SpringTab.vue'

import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'

type Tab = "ocl" | "persistence" | "spring"

const { className } = useSelectedClass()
const activeTab = ref<Tab>("ocl")
</script>

<template>
  <aside v-if="className" :key="className" class="right-panel">
    <header class="panel-head">
      <h3> {{ className }}</h3>
    </header>

    <Tabs v-model:value="activeTab">
      <TabList>
        <Tab value="ocl">OCL</Tab>
        <Tab value="persistence">Persistence</Tab>
        <Tab value="spring">Spring</Tab>
      </TabList>

      <TabPanels>
        <TabPanel value="ocl">
          <OclTab />
        </TabPanel>

        <TabPanel value="persistence">
          <PersistenceTab />
        </TabPanel>

        <TabPanel value="spring">
          <SpringTab />
        </TabPanel>
      </TabPanels>
    </Tabs>
  </aside>
</template>

<style scoped>
  .right-panel {
    border-left: 1px solid var(--color-border);
    background: var(--background);
    color: var(--color-text);
    width: 400px;
    flex-shrink: 0;
    height: 100%;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
  }

  .right-panel :deep(.p-textarea) {
    font-family: ui-monospace, 'SF Mono', Menlo, Consolas, monospace;
    font-size: 14px;
    line-height: 1.4;
  }

  .panel-head {
    padding: 10px 12px;
    border-bottom: 1px solid var(--color-border);
  }
  .panel-head h3 {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    letter-spacing: 0.3px;
  }

  :deep(.p-tabs) {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }
  :deep(.p-tabpanels) {
    flex: 1;
    min-height: 0;
    background: var(--background);
  }
  :deep(.p-tabpanel) {
    height: 100%;
  }
</style>
