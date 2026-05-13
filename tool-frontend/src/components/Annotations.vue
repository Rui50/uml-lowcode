<script setup lang="ts">
import { ref, computed } from 'vue'
import { useModelStore } from '@/stores/model'
import { useAnnotationsStore } from '@/stores/annotations';

import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select';

/**
 * POSSIVEIS MUDANÇAS:
 *  - Nos inits e derives, clicar em add Derive and selecionar um atributo da classe. Assim, a anotação fica diretamente associada ao atributo, e não precisamos de uma sintaxe para referenciar o atributo (ex: key(attr.name)).
 * 
 */

type Tab = "ocl" | "spring"

const model = useModelStore()
const annotations = useAnnotationsStore()

const activeTab = ref<Tab>("ocl")

const selected = computed(() => model.selectedClass ?? model.selectedInterface)
const isInterface = computed(() => model.selectedKind === 'interface')

const className = computed(() => selected.value?.name ?? null)

const invariants = computed(() =>
  className.value ? annotations.invariantsByClass[className.value] ?? [] : []
)

const attributes = computed(() => isInterface.value ? [] : model.selectedClass?.attributes ?? [])

const operations = computed(() => isInterface.value ? [] : model.selectedClass?.operations ?? [])
const interactions = computed(() => model.model?.interactions ?? [])

const enumsText = computed<Record<string, string[]>>(() => {
  const enums = model.model?.enumerations ?? []
  const map: Record<string, string[]> = {}

  for (const e of enums) {
    map[e.name] = e.literals
  }

  return map
})

const interactionOptions = computed(() =>
  interactions.value.map(i => ({
    id: i.id,
    label: i.contextClass ? `${i.name} [${i.contextClass}]` : i.name,
  }))
  )

function key(name: string) {
  return `${className.value}.${name}`
}

function close() {
  model.selectClass(null)
}
</script>

<template>
  <aside v-if="className" :key="className" class="right-panel">
    <header class="panel-head">
      <h3> {{ className }}</h3>
    </header>

    <Tabs v-model:value="activeTab">
      <TabList>
        <Tab value="ocl">OCL</Tab>
        <Tab value="spring">Spring</Tab>
      </TabList>

      <TabPanels>
        <TabPanel value="ocl">

          <!-- Invariants   -->
          <section class="block">
            <header class="block-head">
              <h4>Invariants</h4>
              <Button label="Add" icon="pi pi-plus" size="small" @click="annotations.addInvariant(className!)" />            
            </header>

            <div v-for="inv in invariants" :key="inv.id" class="entry">
              <InputText
                placeholder="name (optional)"
                v-model="inv.name"
              />
              <Textarea 
                v-model="inv.expression" 
                placeholder="self.alunos->size() <= 30"
                rows="3" auto-resize 
              />
              <InputText
                placeholder="error message (optional)"
                v-model="inv.message"
              />
              <Button
                label="Remove" icon="pi pi-trash" size="small" severity="danger" text
                class="remove-btn"
                @click="annotations.deleteInvariant(className!, inv.id)"
              />
            </div>
            <p v-if="!invariants.length" class="empty">No invariants yet.</p>
          </section>

          <!-- Attributes -->
          <section v-if="!isInterface" class="block">
            <header class="block-head">
              <h4>Attributes</h4>
            </header>
            <div v-for="attr in attributes" :key="attr.name" class="entry">
              <div class="attr-label">
                {{ attr.name }}: <span class="type">{{ attr.type }}</span>
                <span v-if="enumsText[attr.type]" class="enum-hint">
                  ({{ enumsText[attr.type]?.join(', ') }})
                </span>
              </div>

              <div v-if="key(attr.name) in annotations.initByAttribute" class="sub-entry">
                <Textarea
                  placeholder="init expression"
                  v-model="annotations.initByAttribute[key(attr.name)]"
                  rows="2" auto-resize
                />
                <Button
                  icon="pi pi-trash" size="small" severity="danger" text
                  @click="annotations.deleteInit(key(attr.name))"
                />
              </div>
              <Button
                v-else label="Add init" icon="pi pi-plus" size="small" text
                @click="annotations.addInit(key(attr.name))"
              />

              <div v-if="key(attr.name) in annotations.deriveByAttribute" class="sub-entry">
                <Textarea
                  placeholder="derive expression"
                  v-model="annotations.deriveByAttribute[key(attr.name)]"
                  rows="2" auto-resize
                />
                <Button
                  icon="pi pi-trash" size="small" severity="danger" text
                  @click="annotations.deleteDerive(key(attr.name))"
                />
              </div>
              <Button
                v-else label="Add derive" icon="pi pi-plus" size="small" text
                @click="annotations.addDerive(key(attr.name))"
              />
            </div>
            <p v-if="!attributes.length" class="empty">No attributes.</p>
          </section>


          <!-- Operations -->
          <section v-if="!isInterface" class="block">
            <header class="block-head">
              <h4>Operations</h4>
            </header>
            <div v-for="op in operations" :key="op.id" class="entry">
              <div class="attr-label">
                {{ op.name }}
              </div>
              <div class="sub-entry">
                <Select
                  v-model="annotations.operationToSeqDiagram[op.id]"
                  :options="interactionOptions"
                  optionLabel="label"
                  optionValue="id"
                  placeholder="— no sequence linked —"
                  showClear
                />
              </div>

              <div class="sub-block">
                <header class="sub-head">
                  <span>Preconditions</span>
                  <Button label="Add" icon="pi pi-plus" size="small" text
                          @click="annotations.addPrecondition(op.id)" />
                </header>

                <div
                  v-for="pre in annotations.preconditionsByOperation[op.id] ?? []"
                  :key="pre.id"
                  class="entry"
                >
                  <InputText placeholder="name (optional)" v-model="pre.name" />
                  <Textarea
                    v-model="pre.expression"
                    placeholder="self.alunos->size() > 0"
                    rows="2" auto-resize
                  />
                  <InputText placeholder="error message (optional)" v-model="pre.message" />
                  <Button
                    label="Remove" icon="pi pi-trash" size="small" severity="danger" text
                    class="remove-btn"
                    @click="annotations.deletePrecondition(op.id, pre.id)"
                  />
                </div>
              </div>

              <div class="sub-block">
                <header class="sub-head">
                  <span>Postconditions</span>
                  <Button label="Add" icon="pi pi-plus" size="small" text
                          @click="annotations.addPostcondition(op.id)" />
                </header>

                <div
                  v-for="post in annotations.postconditionsByOperation[op.id] ?? []"
                  :key="post.id"
                  class="entry"
                >
                  <InputText placeholder="name (optional)" v-model="post.name" />
                  <Textarea
                    v-model="post.expression"
                    placeholder="self.alunos->size() = self.alunos@pre->size() + 1"
                    rows="2" auto-resize
                  />
                  <InputText placeholder="error message (optional)" v-model="post.message" />
                  <Button
                    label="Remove" icon="pi pi-trash" size="small" severity="danger" text
                    class="remove-btn"
                    @click="annotations.deletePostcondition(op.id, post.id)"
                  />
                </div>
              </div>
            </div>
          </section>

        </TabPanel>

        <TabPanel value="spring">
          <p class="empty">Spring annotations</p>
        </TabPanel>
      </TabPanels>
    </Tabs>
  </aside>
  
</template>

<style scoped>
.right-panel {
  border-left: 1px solid var(--color-border);
  /*background: var(--color-secondary);*/
  background: var(--background);
  color: var(--color-text);
  width: 360px;
  flex-shrink: 0;
  height: 100%;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
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

.block {
  padding: 4px 0;
}
.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding: 0 4px;
}
.block-head h4 {
  margin: 0;
  font-size: 12px;
  opacity: 0.8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.entry {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  margin-bottom: 8px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--background);
}
.entry .remove-btn {
  align-self: flex-end;
}
.attr-label {
  font-family: monospace;
  font-size: 12px;
}
.attr-label .type {
  color: var(--color-primary);
}
.attr-label .enum-hint {
  opacity: 0.6;
  font-size: 11px;
  margin-left: 4px;
}
.sub-entry {
  display: flex;
  gap: 4px;
  align-items: center;
}

.sub-entry :deep(.p-inputtext),
.sub-entry :deep(.p-textarea) {
  flex: 1;
}

.empty {
  padding: 1.5rem 1rem;
  opacity: 0.5;
  font-style: italic;
  font-size: 12px;
  text-align: center;
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
  /*background: var(--color-secondary);*/
  background: var(--background);
}
:deep(.p-tabpanel) {
  height: 100%;
}

.sub-block {
    margin-top: 6px;
    padding-left: 8px;
    border-left: 2px solid var(--color-border);
  }
  .sub-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 11px;
    opacity: 0.7;
    text-transform: uppercase;
    letter-spacing: 0.4px;
    margin-bottom: 4px;
  }
</style>