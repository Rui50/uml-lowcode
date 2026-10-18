<script setup lang="ts">
import { computed } from 'vue'
import { useModelStore } from '@/stores/model'
import { useAnnotationsStore } from '@/stores/annotations'
import { useSelectedClass } from '@/composables/useSelectedClass'

import Button from 'primevue/button'
import Select from 'primevue/select'
import OclField from './OclField.vue'
import OCLText from './OCLText.vue'

const model = useModelStore()
const annotations = useAnnotationsStore()
const { isInterface, classKey, attributes, operations } = useSelectedClass()

const hasInit = (name: string) => classKey.value != null && annotations.initByAttribute[classKey.value]?.[name] !== undefined
const hasDerive = (name: string) => classKey.value != null && annotations.deriveByAttribute[classKey.value]?.[name] !== undefined

const invariants = computed(() =>
  classKey.value ? annotations.invariantsByClass[classKey.value] ?? [] : []
)

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

// placeholder do init: num enum mostra a sintaxe OCL (Tipo::LITERAL) com um exemplo real
function initPlaceholder(attr: { type: string }) {
  const lits = enumsText.value[attr.type]
  return lits?.length ? `ex: ${attr.type}::${lits[0]}` : 'init expression'
}
</script>

<template>
  <!-- Invariants -->
  <section class="block">
    <header class="block-head">
      <h4>Invariants</h4>
      <div style="display: flex; gap: 6px;">
        <Button label="Fill required" icon="pi pi-check-square" size="small" severity="secondary"
                v-tooltip.top="'Cria invariantes de não-nulo para as relações single obrigatórias (mult. 1) desta classe'"
                @click="annotations.prefillMandatoryRelations(classKey!)" />
        <Button label="Add" icon="pi pi-plus" size="small" @click="annotations.addInvariant(classKey!)" />
      </div>
    </header>

    <OCLText
      :entries="invariants"
      placeholder="self.alunos->size() <= 30"
      header="Invariant"
      @delete="id => annotations.deleteInvariant(classKey!, id)"
    />
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

      <div v-if="hasInit(attr.name)" class="sub-entry">
        <OclField
          :placeholder="initPlaceholder(attr)"
          :modelValue="annotations.initExpr(classKey!, attr.name)"
          @update:modelValue="v => annotations.setInit(classKey!, attr.name, v ?? '')"
          header="Init"
        />
        <Button
          icon="pi pi-trash" size="small" severity="danger" text
          @click="annotations.deleteInit(classKey!, attr.name)"
        />
      </div>
      <!-- init e derive são exclusivos: bloqueia add init se já tem derive -->
      <span v-else :title="hasDerive(attr.name) ? 'Already has a derive — an attribute can\'t be both' : ''">
        <Button
          label="Add init" icon="pi pi-plus" size="small" text
          :disabled="hasDerive(attr.name)"
          @click="annotations.addInit(classKey!, attr.name)"
        />
      </span>

      <div v-if="hasDerive(attr.name)" class="sub-entry">
        <OclField
          placeholder="derive expression"
          :modelValue="annotations.deriveExpr(classKey!, attr.name)"
          @update:modelValue="v => annotations.setDerive(classKey!, attr.name, v ?? '')"
          header="Derive"
        />
        <Button
          icon="pi pi-trash" size="small" severity="danger" text
          @click="annotations.deleteDerive(classKey!, attr.name)"
        />
      </div>
      <!-- bloqueia add derive se já tem init -->
      <span v-else :title="hasInit(attr.name) ? 'Already has an init — an attribute can\'t be both' : ''">
        <Button
          label="Add derive" icon="pi pi-plus" size="small" text
          :disabled="hasInit(attr.name)"
          @click="annotations.addDerive(classKey!, attr.name)"
        />
      </span>
    </div>
    <p v-if="!attributes.length" class="empty">No attributes.</p>
  </section>

  <!-- Operations -->
  <section v-if="!isInterface" class="block">
    <header class="block-head">
      <h4>Operations</h4>
    </header>
    <div v-for="op in operations" :key="op.id" class="entry">
      <div class="op-head">
        {{ op.name }}
      </div>
      <div class="sub-entry seq-link">
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

        <OCLText
          :entries="annotations.preconditionsByOperation[op.id] ?? []"
          placeholder="self.alunos->size() > 0"
          header="Precondition"
          @delete="id => annotations.deletePrecondition(op.id, id)"
        />
      </div>

      <div class="sub-block">
        <header class="sub-head">
          <span>Postconditions</span>
          <Button label="Add" icon="pi pi-plus" size="small" text
                  @click="annotations.addPostcondition(op.id)" />
        </header>

        <OCLText
          :entries="annotations.postconditionsByOperation[op.id] ?? []"
          placeholder="self.alunos->size() = self.alunos@pre->size() + 1"
          header="Postcondition"
          @delete="id => annotations.deletePostcondition(op.id, id)"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.block {
  padding: 24px 0;
}
.block + .block {
  border-top: 1px solid var(--color-border);
}
.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
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
  gap: 8px;
  padding: 8px;
  margin-bottom: 8px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--background);
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
.seq-link {
  margin-top: 6px;
}
.sub-entry :deep(.p-inputtext),
.sub-entry :deep(.p-textarea) {
  flex: 1;
}
.sub-entry :deep(.ocl-field) {
  flex: 1;
  min-width: 0;
}
.sub-entry :deep(.p-select) {
  flex: 1;
  min-width: 0;
}
.sub-entry :deep(.p-select-label) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.op-head {
  font-family: monospace;
  font-size: 13px;
  font-weight: 600;
  margin: -8px;
  padding: 6px 8px;
  border-radius: 6px 6px 0 0;
  border-bottom: 1px solid var(--color-border);
  background: color-mix(in srgb, var(--color-text) 7%, transparent);
}

.sub-block {
  margin-top: 6px;
}
.sub-block + .sub-block {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--color-border);
}
.sub-block .entry {
  border: none;
  border-radius: 0;
  background: transparent;
  padding: 0;
}
.sub-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  opacity: 0.7;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  margin-bottom: 8px;
}

.empty {
  padding: 1.5rem 1rem;
  opacity: 0.5;
  font-style: italic;
  font-size: 12px;
  text-align: center;
}
</style>
