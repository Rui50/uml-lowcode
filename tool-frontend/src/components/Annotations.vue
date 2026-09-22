<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useModelStore } from '@/stores/model'
import { useAnnotationsStore } from '@/stores/annotations';
import { useSelectedClass } from '@/composables/useSelectedClass'
import type { RelationField } from '@/types'
import SpringTab from './SpringTab.vue'

import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import OclField from './OclField.vue'
import Select from 'primevue/select';
import SelectButton from 'primevue/selectbutton'
import Checkbox from 'primevue/checkbox'
import InputNumber from 'primevue/inputnumber'

type Tab = "ocl" | "persistence" | "spring"

const model = useModelStore()
const annotations = useAnnotationsStore()
const { selected, isInterface, className, classKey, attributes, operations, key } = useSelectedClass()

// coleções da classe selecionada + atributos da classe alvo (para o dropdown de order-by)
const collectionRelations = computed(() =>
  (model.selectedClass?.relations ?? []).filter(r => r.collection)
)
// so rels singulares (@ManyToOne/@OneToOne) — alvo do fetch strategy
const singularRelations = computed(() =>
  (model.selectedClass?.relations ?? []).filter(r => !r.collection)
)
function orderOptions(rel: RelationField) {
  const target = model.model?.classes.find(c => c.name === rel.targetClass)
  return (target?.attributes ?? []).map(a => ({ label: `${a.name}: ${a.type}`, value: a.name }))
}

const activeTab = ref<Tab>("ocl")

function hasPrecision(type: string) {
  return type === 'BigDecimal' || type === 'BigInteger'
}
function hasScale(type: string) {
  return type === 'BigDecimal'
}

// a tabela de column mapping liga o v-model direto a columnByAttribute[key], por isso
// cada atributo precisa de um objeto default
watch([attributes, className], () => {
  for (const a of attributes.value) {
    const k = key(a.name)
    if (!(k in annotations.columnByAttribute)) annotations.addColumnConfig(k)
  }
}, { immediate: true })

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

// a estratégia de herança JPA é anotada na raiz da hierarquia
// só mostramos o seletor se outra classe declarar esta como pai
const isSuperclass = computed(() =>
  !!className.value &&
  (model.model?.classes ?? []).some(c => c.parent === className.value)
)

const inheritanceOptions = [
  { label: 'Single table',  value: 'SINGLE_TABLE' },
  { label: 'Joined tables', value: 'JOINED' },
]

// @Id: anotado na raiz da hierarquia.
// integrais -> auto-gerados (@GeneratedValue); String -> chave natural atribuída pelo cliente
const ELIGIBLE_ID_TYPES = ['Long', 'Integer', 'long', 'int', 'String']

const isIdRoot = computed(() =>
  !isInterface.value && !!model.selectedClass && !model.selectedClass.parent
)

// uma classe tem @Table própria — exceto se for filha numa hierarquia SINGLE_TABLE
// (partilha a tabela do pai).
const ownsTable = computed(() => {
  const cls = model.selectedClass
  if (isInterface.value || !cls) return false
  if (!cls.parent) return true                       // raiz / standalone -> tabela própria
  const classes = model.model?.classes ?? []
  let root = cls
  while (root.parent) {                              // subir até à raiz da hierarquia
    const p = classes.find(c => c.name === root.parent)
    if (!p) break
    root = p
  }
  const strategy = annotations.inheritanceStrategyByClass[root.id] ?? 'SINGLE_TABLE'
  return strategy !== 'SINGLE_TABLE'                 // filho: só JOINED tem tabela própria
})

// config atual do id (undefined = id por defeito)
const idConfig = computed(() =>
  classKey.value ? annotations.idByClass[classKey.value] : undefined
)

// atributos elegíveis: integrais (wrapper) e não derivados
const idEligibleAttributes = computed(() =>
  (model.selectedClass?.attributes ?? []).filter(a =>
    ELIGIBLE_ID_TYPES.includes(a.type) &&
    !(key(a.name) in annotations.deriveByAttribute)
  )
)

const UUID_OPTION = '__uuid__'

// opções: UUID (sintético) + atributos elegíveis. Clear/placeholder = Long default.
const idOptions = computed(() => [
  { label: 'UUID (auto-generated)', value: UUID_OPTION },
  ...idEligibleAttributes.value.map(a => ({ label: `${a.name}: ${a.type}`, value: a.name })),
])

// valor no dropdown: null (Long) | UUID_OPTION | nome do atributo.
// UUID é a única config sem attributeName, daí o fallback.
const idSelection = computed(() =>
  idConfig.value ? (idConfig.value.attributeName || UUID_OPTION) : null
)

// chave natural String -> atribuída pelo cliente (mostra aviso)
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

// placeholder do init: num enum mostra a sintaxe OCL (Tipo::LITERAL) com um exemplo real
function initPlaceholder(attr: { type: string }) {
  const lits = enumsText.value[attr.type]
  return lits?.length ? `ex: ${attr.type}::${lits[0]}` : 'init expression'
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
        <Tab value="persistence">Persistence</Tab>
        <Tab value="spring">Spring</Tab>
      </TabList>

      <TabPanels>
        <TabPanel value="ocl">

          <!-- Invariants   -->
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

            <div v-for="inv in invariants" :key="inv.id" class="entry">
              <InputText
                placeholder="name (optional)"
                v-model="inv.name"
              />
              <OclField
                v-model="inv.expression"
                placeholder="self.alunos->size() <= 30"
                header="Invariant"
              />
              <InputText
                placeholder="error message (optional)"
                v-model="inv.message"
              />
              <Button
                label="Remove" icon="pi pi-trash" size="small" severity="danger" text
                class="remove-btn"
                @click="annotations.deleteInvariant(classKey!, inv.id)"
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
                <OclField
                  :placeholder="initPlaceholder(attr)"
                  v-model="annotations.initByAttribute[key(attr.name)]"
                  header="Init"
                />
                <Button
                  icon="pi pi-trash" size="small" severity="danger" text
                  @click="annotations.deleteInit(key(attr.name))"
                />
              </div>
              <!-- init e derive são exclusivos: bloqueia add init se já tem derive -->
              <span v-else :title="key(attr.name) in annotations.deriveByAttribute ? 'Already has a derive — an attribute can\'t be both' : ''">
                <Button
                  label="Add init" icon="pi pi-plus" size="small" text
                  :disabled="key(attr.name) in annotations.deriveByAttribute"
                  @click="annotations.addInit(key(attr.name))"
                />
              </span>

              <div v-if="key(attr.name) in annotations.deriveByAttribute" class="sub-entry">
                <OclField
                  placeholder="derive expression"
                  v-model="annotations.deriveByAttribute[key(attr.name)]"
                  header="Derive"
                />
                <Button
                  icon="pi pi-trash" size="small" severity="danger" text
                  @click="annotations.deleteDerive(key(attr.name))"
                />
              </div>
              <!-- bloqueia add derive se já tem init -->
              <span v-else :title="key(attr.name) in annotations.initByAttribute ? 'Already has an init — an attribute can\'t be both' : ''">
                <Button
                  label="Add derive" icon="pi pi-plus" size="small" text
                  :disabled="key(attr.name) in annotations.initByAttribute"
                  @click="annotations.addDerive(key(attr.name))"
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

                <div
                  v-for="pre in annotations.preconditionsByOperation[op.id] ?? []"
                  :key="pre.id"
                  class="entry"
                >
                  <InputText placeholder="name (optional)" v-model="pre.name" />
                  <OclField
                    v-model="pre.expression"
                    placeholder="self.alunos->size() > 0"
                    header="Precondition"
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
                  <OclField
                    v-model="post.expression"
                    placeholder="self.alunos->size() = self.alunos@pre->size() + 1"
                    header="Postcondition"
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

        <TabPanel value="persistence">
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
                    <Checkbox v-model="annotations.columnByAttribute[key(attr.name)]!.unique" binary />
                  </td>
                  <td class="c" :class="{ 'is-disabled': !hasPrecision(attr.type) }">
                    <InputNumber
                      v-model="annotations.columnByAttribute[key(attr.name)]!.precision"
                      :disabled="!hasPrecision(attr.type)" :min="1" :useGrouping="false"
                    />
                  </td>
                  <td class="c" :class="{ 'is-disabled': !hasScale(attr.type) }">
                    <InputNumber
                      v-model="annotations.columnByAttribute[key(attr.name)]!.scale"
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
                v-model="annotations.columnByAttribute[key(attr.name)]!.name"
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
                :modelValue="annotations.orderAttribute(key(rel.name))"
                @update:modelValue="annotations.setOrderAttribute(key(rel.name), $event ?? '')"
                :options="orderOptions(rel)" optionLabel="label" optionValue="value"
                placeholder="— none —" showClear
              />
              <Button
                v-if="annotations.orderAttribute(key(rel.name))"
                :label="annotations.orderDirection(key(rel.name))"
                icon="pi pi-sort-alt" size="small" text
                @click="annotations.toggleOrderDirection(key(rel.name))"
              />
            </div>
          </section>

          <!-- Fetch strategy  (@ManyToOne/@OneToOne) -->
          <section v-if="singularRelations.length" class="block">
            <header class="block-head">
              <h4>Fetch strategy
                <span class="tip" v-tooltip.top="'LAZY (recomendado) carrega a relação só quando é usada | EAGER carrega sempre com a entidade.'"><i class="pi pi-info-circle"></i></span>
              </h4>
            </header>
            <div v-for="rel in singularRelations" :key="rel.name" class="order-row">
              <span class="order-rel">{{ rel.name }} <span class="type">-> {{ rel.targetClass }}</span></span>
              <SelectButton
                :modelValue="annotations.fetchStrategy(key(rel.name))"
                @update:modelValue="val => val && annotations.setFetchStrategy(key(rel.name), val)"
                :options="['LAZY', 'EAGER']"
                :allowEmpty="false"
                size="small"
              />
            </div>
          </section>

          <p v-if="!ownsTable && !isIdRoot && !isSuperclass && !attributes.length" class="empty">
            Sem opções de persistência para esta classe.
          </p>
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
    /*background: var(--color-secondary);*/
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

  .job-toggle {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .empty {
    padding: 1.5rem 1rem;
    opacity: 0.5;
    font-style: italic;
    font-size: 12px;
    text-align: center;
  }
  .hint {
    font-size: 11px;
    opacity: 0.65;
    margin: 0;
    line-height: 1.4;
  }
  .tip { font-size: 13px; opacity: 0.5; cursor: help; margin-left: 4px; font-weight: normal; }
  .hint.warn {
    opacity: 1;
    color: var(--p-orange-600, #c2410c);
  }

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

</style>
