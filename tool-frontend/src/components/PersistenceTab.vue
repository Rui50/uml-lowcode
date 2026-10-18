<script setup lang="ts">
import { computed, watch } from 'vue'
import { useModelStore } from '@/stores/model'
import { useAnnotationsStore } from '@/stores/annotations'
import { useSelectedClass } from '@/composables/useSelectedClass'
import type { RelationField } from '@/types'

import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import SelectButton from 'primevue/selectbutton'
import Checkbox from 'primevue/checkbox'
import InputNumber from 'primevue/inputnumber'
import Button from 'primevue/button'

const model = useModelStore()
const annotations = useAnnotationsStore()
const { isInterface, className, classKey, attributes } = useSelectedClass()

// coleções da classe (para o order-by) e singulares (fetch strategy)
const collectionRelations = computed(() =>
  (model.selectedClass?.relations ?? []).filter(r => r.collection)
)
const singularRelations = computed(() =>
  (model.selectedClass?.relations ?? []).filter(r => !r.collection)
)
function orderOptions(rel: RelationField) {
  const target = model.model?.classes.find(c => c.name === rel.targetClass)
  return (target?.attributes ?? []).map(a => ({ label: `${a.name}: ${a.type}`, value: a.name }))
}

function hasPrecision(type: string) {
  return type === 'BigDecimal' || type === 'BigInteger'
}
function hasScale(type: string) {
  return type === 'BigDecimal'
}
const hasDerive = (name: string) =>
  classKey.value != null && annotations.deriveByAttribute[classKey.value]?.[name] !== undefined

// ColumnConfig do atributo na classe atual (o seed watch garante que existe)
const col = (name: string) => annotations.columnByAttribute[classKey.value!]![name]!

// cada atributo precisa de um ColumnConfig default para o v-model do column mapping ligar direto
watch([attributes, className], () => {
  const classId = classKey.value
  if (!classId) return
  for (const a of attributes.value) {
    if (annotations.columnByAttribute[classId]?.[a.name] === undefined) annotations.addColumnConfig(classId, a.name)
  }
}, { immediate: true })

// estratégia de herança JPA anotada na raiz; só se outra classe declarar esta como pai
const isSuperclass = computed(() =>
  !!className.value &&
  (model.model?.classes ?? []).some(c => c.parent === className.value)
)
const inheritanceOptions = [
  { label: 'Single table',  value: 'SINGLE_TABLE' },
  { label: 'Joined tables', value: 'JOINED' },
]

// @Id anotado na raiz. integrais -> auto-gerados; String -> chave natural atribuída pelo cliente
const ELIGIBLE_ID_TYPES = ['Long', 'Integer', 'long', 'int', 'String']

const isIdRoot = computed(() =>
  !isInterface.value && !!model.selectedClass && !model.selectedClass.parent
)

// uma classe tem @Table própria — exceto filha numa hierarquia SINGLE_TABLE (partilha a do pai)
const ownsTable = computed(() => {
  const cls = model.selectedClass
  if (isInterface.value || !cls) return false
  if (!cls.parent) return true
  const classes = model.model?.classes ?? []
  let root = cls
  while (root.parent) {
    const p = classes.find(c => c.name === root.parent)
    if (!p) break
    root = p
  }
  const strategy = annotations.inheritanceStrategyByClass[root.id] ?? 'SINGLE_TABLE'
  return strategy !== 'SINGLE_TABLE'
})

const idConfig = computed(() =>
  classKey.value ? annotations.idByClass[classKey.value] : undefined
)
const idEligibleAttributes = computed(() =>
  (model.selectedClass?.attributes ?? []).filter(a =>
    ELIGIBLE_ID_TYPES.includes(a.type) && !hasDerive(a.name)
  )
)
const UUID_OPTION = '__uuid__'
const idOptions = computed(() => [
  { label: 'UUID (auto-generated)', value: UUID_OPTION },
  ...idEligibleAttributes.value.map(a => ({ label: `${a.name}: ${a.type}`, value: a.name })),
])
const idSelection = computed(() =>
  idConfig.value ? (idConfig.value.attributeName || UUID_OPTION) : null
)
const idIsAssigned = computed(() => idConfig.value?.attributeType === 'String')

function pickId(value: string | null) {
  if (!classKey.value) return
  if (!value) { annotations.setId(classKey.value, null); return }              // Long default
  if (value === UUID_OPTION) {
    annotations.setId(classKey.value, { attributeName: '', attributeType: 'UUID' })
    return
  }
  const attr = idEligibleAttributes.value.find(a => a.name === value)
  if (!attr) return
  annotations.setId(classKey.value, { attributeName: attr.name, attributeType: attr.type })
}
</script>

<template>
  <p class="tab-note">
    Applies only to the <strong>CLI + JPA</strong> and <strong>Spring</strong> targets.
  </p>
  <!-- Nome da tabela (override) — só onde a classe tem @Table própria -->
  <section v-if="ownsTable" class="block">
    <header class="block-head">
      <h4>Table name
        <span class="tip" v-tooltip.top="'Override do nome da tabela. Vazio = nome da classe em minúsculas. Útil para evitar palavras reservadas em SQL (ex: Order, User).'"><i class="pi pi-info-circle"></i></span>
      </h4>
    </header>
    <div class="entry">
      <InputText
        v-model="annotations.tableNameByClass[classKey!]"
        :placeholder="className?.toLowerCase()"
      />
    </div>
  </section>

  <!-- Identificador @Id (só na raiz da hierarquia) -->
  <section v-if="isIdRoot" class="block">
    <header class="block-head">
      <h4>Identifier (@Id)</h4>
    </header>
    <div class="entry">
      <Select
        :modelValue="idSelection"
        @update:modelValue="pickId"
        :options="idOptions"
        optionLabel="label"
        optionValue="value"
        placeholder="Default (Long, auto-generated)"
        showClear
      />
      <p v-if="idIsAssigned" class="hint warn">
        Chave natural — tem de ser fornecida pelo cliente (não é auto-gerada).
      </p>
      <p v-else class="hint">
        O id é gerado automaticamente pelo servidor.
      </p>
    </div>
  </section>

  <!-- Estratégia de herança JPA (só na superclasse) -->
  <section v-if="isSuperclass" class="block">
    <header class="block-head">
      <h4>Inheritance strategy
        <span class="tip" v-tooltip.top="'Mapeamento JPA da hierarquia (alvos Spring e CLI-JPA).'"><i class="pi pi-info-circle"></i></span>
      </h4>
    </header>
    <div class="entry">
      <Select
        v-model="annotations.inheritanceStrategyByClass[classKey!]"
        :options="inheritanceOptions"
        optionLabel="label"
        optionValue="value"
        placeholder="Single table"
      />
    </div>
  </section>

  <!-- Column mapping (@Column JPA) por atributo -->
  <section v-if="attributes.length" class="block">
    <header class="block-head">
      <h4>Column mapping
        <span class="tip" v-tooltip.top="'Config de coluna JPA: nome da coluna (vazio = nome do atributo), unique e precisão/escala (só em decimais).'"><i class="pi pi-info-circle"></i></span>
      </h4>
    </header>
    <table class="col-map">
      <thead>
        <tr>
          <th>Attribute</th>
          <th class="c">Unique</th>
          <th class="c">Precision <span class="tip" v-tooltip.top="'Só em BigDecimal/BigInteger (total de dígitos).'"><i class="pi pi-info-circle"></i></span></th>
          <th class="c">Scale <span class="tip" v-tooltip.top="'Só em BigDecimal (casas decimais).'"><i class="pi pi-info-circle"></i></span></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="attr in attributes" :key="attr.name">
          <td class="attr-cell">{{ attr.name }} <span class="type">{{ attr.type }}</span></td>
          <td class="c">
            <Checkbox v-model="col(attr.name).unique" binary />
          </td>
          <td class="c" :class="{ 'is-disabled': !hasPrecision(attr.type) }">
            <InputNumber
              v-model="col(attr.name).precision"
              :disabled="!hasPrecision(attr.type)" :min="1" :useGrouping="false"
            />
          </td>
          <td class="c" :class="{ 'is-disabled': !hasScale(attr.type) }">
            <InputNumber
              v-model="col(attr.name).scale"
              :disabled="!hasScale(attr.type)" :min="0" :useGrouping="false"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </section>

  <!-- Column name override por atributo -->
  <section v-if="attributes.length" class="block">
    <header class="block-head">
      <h4>Column names
        <span class="tip" v-tooltip.top="'Nome da coluna na base de dados por atributo. Vazio = nome do atributo.'"><i class="pi pi-info-circle"></i></span>
      </h4>
    </header>
    <div v-for="attr in attributes" :key="attr.name" class="order-row">
      <span class="order-rel">{{ attr.name }} <span class="type">{{ attr.type }}</span></span>
      <InputText
        v-model="col(attr.name).name"
        :placeholder="attr.name" size="small"
      />
    </div>
  </section>

  <!-- Collection ordering (@OrderBy) por coleção -->
  <section v-if="collectionRelations.length" class="block">
    <header class="block-head">
      <h4>Collection ordering
        <span class="tip" v-tooltip.top="'@OrderBy — ordena os itens de uma coleção. Vazio = ordem indefinida.'"><i class="pi pi-info-circle"></i></span>
      </h4>
    </header>
    <div v-for="rel in collectionRelations" :key="rel.name" class="order-row">
      <span class="order-rel">{{ rel.name }} <span class="type">-> {{ rel.targetClass }}</span></span>
      <Select
        :modelValue="annotations.orderAttribute(classKey!, rel.name)"
        @update:modelValue="annotations.setOrderAttribute(classKey!, rel.name, $event ?? '')"
        :options="orderOptions(rel)" optionLabel="label" optionValue="value"
        placeholder="— none —" showClear
      />
      <Button
        v-if="annotations.orderAttribute(classKey!, rel.name)"
        :label="annotations.orderDirection(classKey!, rel.name)"
        icon="pi pi-sort-alt" size="small" text
        @click="annotations.toggleOrderDirection(classKey!, rel.name)"
      />
    </div>
  </section>

  <!-- Fetch strategy (@ManyToOne/@OneToOne) -->
  <section v-if="singularRelations.length" class="block">
    <header class="block-head">
      <h4>Fetch strategy
        <span class="tip" v-tooltip.top="'LAZY (recomendado) carrega a relação só quando é usada | EAGER carrega sempre com a entidade.'"><i class="pi pi-info-circle"></i></span>
      </h4>
    </header>
    <div v-for="rel in singularRelations" :key="rel.name" class="order-row">
      <span class="order-rel">{{ rel.name }} <span class="type">-> {{ rel.targetClass }}</span></span>
      <SelectButton
        :modelValue="annotations.fetchStrategy(classKey!, rel.name)"
        @update:modelValue="val => val && annotations.setFetchStrategy(classKey!, rel.name, val)"
        :options="['LAZY', 'EAGER']"
        :allowEmpty="false"
        size="small"
      />
    </div>
  </section>

  <p v-if="!ownsTable && !isIdRoot && !isSuperclass && !attributes.length" class="empty">
    Sem opções de persistência para esta classe.
  </p>
</template>

<style scoped>
.tab-note {
  margin: 0 0 12px;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: 6px;
}
.tab-note strong {
  font-weight: 600;
}

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

.tip { font-size: 13px; opacity: 0.5; cursor: help; margin-left: 4px; font-weight: normal; }
.hint {
  font-size: 11px;
  opacity: 0.65;
  margin: 0;
  line-height: 1.4;
}
.hint.warn {
  opacity: 1;
  color: var(--p-orange-600, #c2410c);
}

.col-map {
  width: 100%; border-collapse: collapse; font-size: 12px;
}
.col-map th, .col-map td {
  padding: 5px 6px; vertical-align: middle;
}
.col-map thead th {
  border-bottom: 1px solid var(--color-border);
}
.col-map tbody td {
  border-bottom: 1px solid var(--color-border);
}
.col-map tbody tr:last-child td {
  border-bottom: none;
}
.col-map th {
  text-align: left;
  font-size: 11px;
  font-weight: 600;
  opacity: 0.7;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}
.col-map th.c, .col-map td.c {
  text-align: center;
}
.col-map .attr-cell {
  font-family: monospace;
  white-space: nowrap;
}
.col-map .attr-cell .type { color: var(--color-primary); }
.col-map td.is-disabled { opacity: 0.4; }
.col-map :deep(.p-inputnumber-input) {
  width: 3.2rem;
  padding: 3px 5px;
  text-align: center;
}

.order-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0;
}
.order-rel {
  flex: 1;
  font-family: monospace;
  font-size: 12px;
}
.order-rel .type {
  color: var(--color-primary);
}
.order-row :deep(.p-select) {
  min-width: 8rem;
}

.empty {
  padding: 1.5rem 1rem;
  opacity: 0.5;
  font-style: italic;
  font-size: 12px;
  text-align: center;
}
</style>
