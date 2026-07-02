<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useModelStore } from '@/stores/model'
import { useAnnotationsStore } from '@/stores/annotations';
import type { Endpoint } from '@/types'

import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select';
import Checkbox from 'primevue/checkbox'

/**
 * POSSIVEIS MUDANÇAS:
 *  - Nos inits e derives, clicar em add Derive and selecionar um atributo da classe. Assim, a anotação fica diretamente associada ao atributo, e não precisamos de uma sintaxe para referenciar o atributo (ex: key(attr.name)).
 *
 */

type Tab = "ocl" | "persistence" | "spring"

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

function pluralPath(name: string) {
  const p = name.toLowerCase()
  return p.endsWith('s') ? p : p + 's'
}

// lista unificada de endpoints da classe: CRUD + relações (backend, estruturais) // add custom despois
const allEndpoints = computed<Endpoint[]>(() => {
  const cls = model.selectedClass
  if (!cls) return []
  const base = `/api/${pluralPath(cls.name)}`
  const list: Endpoint[] = [...(cls.endpoints ?? [])]

  for (const op of cls.operations) {
    const cfg = annotations.endpointByOperation[op.id]
    const path = cfg?.endpointName?.trim() || op.name
    if (cfg?.job) {
      list.push({ httpMethod: 'POST', path: `${base}/${path}`, type: 'CUSTOM', label: op.name, dtoType: null })
    } else if (annotations.operationToSeqDiagram[op.id]) {
      list.push({ httpMethod: cfg?.httpMethod || 'POST', path: `${base}/${path}`, type: 'CUSTOM', label: op.name, dtoType: null })
    }
  }
  return list
})

// matriz "DTO fields": uma linha por atributo, células trancadas quando a semântica já
// (init sai sempre do Create; derive só existe no Response).
const DTO_ROLES = ['create', 'update', 'response'] as const
const dtoFieldRows = computed(() => {
  return attributes.value
    .filter(a => a.name !== idConfig.value?.attributeName)
    .map(a => {
      const hasInit = key(a.name) in annotations.initByAttribute
      const hasDerive = key(a.name) in annotations.deriveByAttribute
      return {
        name: a.name,
        hasInit,
        hasDerive,
        locked: { create: hasInit || hasDerive, update: hasDerive, response: false },
      }
    })
})

const httpMethods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']
// garante uma config de endpoint para cada operação da classe selecionada
watch(operations, (ops) => {
  for (const op of ops) {
    if (!annotations.endpointByOperation[op.id]) {
      annotations.endpointByOperation[op.id] = { job: false, endpointName: '', httpMethod: 'POST', cron: '' }
    }
  }
}, { immediate: true })

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
  const strategy = annotations.inheritanceStrategyByClass[root.name] ?? 'SINGLE_TABLE'
  return strategy !== 'SINGLE_TABLE'                 // filho: só JOINED tem tabela própria
})

// config atual do id (undefined = id por defeito)
const idConfig = computed(() =>
  className.value ? annotations.idByClass[className.value] : undefined
)

// atributos elegíveis: integrais (wrapper) e não derivados
const idEligibleAttributes = computed(() =>
  (model.selectedClass?.attributes ?? []).filter(a =>
    ELIGIBLE_ID_TYPES.includes(a.type) &&
    !(`${className.value}.${a.name}` in annotations.deriveByAttribute)
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
  if (!className.value) return
  if (!value) { annotations.setId(className.value, null); return }              // Long default
  if (value === UUID_OPTION) {
    annotations.setId(className.value, { attributeName: '', attributeType: 'UUID' })
    return
  }
  const attr = idEligibleAttributes.value.find(a => a.name === value)
  if (!attr) return
  annotations.setId(className.value, { attributeName: attr.name, attributeType: attr.type })
}

// controller REST por classe (Spring); abstratas/interfaces não têm controller
const controllerEligible = computed(() =>
  !isInterface.value && !!model.selectedClass && !model.selectedClass.isAbstract
)

// garante a entrada do controller (default true) para a classe selecionada
watch(className, (name) => {
  if (name && controllerEligible.value && annotations.generateControllerByClass[name] === undefined) {
    annotations.generateControllerByClass[name] = true
  }
}, { immediate: true })

// Bean Validation por classe (Spring); aplica-se a qualquer classe (inclui abstratas)
const beanValidationEligible = computed(() => !isInterface.value && !!model.selectedClass)

// garante a entrada do bean validation (default true) para a classe selecionada
watch(className, (name) => {
  if (name && beanValidationEligible.value && annotations.beanValidationByClass[name] === undefined) {
    annotations.beanValidationByClass[name] = true
  }
}, { immediate: true })

function key(name: string) {
  return `${className.value}.${name}`
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
                  :placeholder="initPlaceholder(attr)"
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

        <TabPanel value="persistence">
          <p class="tab-note">
            Applies only to the <strong>CLI + JPA</strong> and <strong>Spring</strong> targets.
          </p>
          <!-- Nome da tabela (override) — só onde a classe tem @Table própria -->
          <section v-if="ownsTable" class="block">
            <header class="block-head">
              <h4>Table name</h4>
            </header>
            <div class="entry">
              <InputText
                v-model="annotations.tableNameByClass[className!]"
                :placeholder="className?.toLowerCase()"
              />
              <p class="hint">
                Override do nome da tabela. Vazio = nome da classe em minúsculas.
                Útil para evitar palavras reservadas em SQL (ex: Order, User).
              </p>
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
              <h4>Inheritance strategy</h4>
            </header>
            <div class="entry">
              <Select
                v-model="annotations.inheritanceStrategyByClass[className!]"
                :options="inheritanceOptions"
                optionLabel="label"
                optionValue="value"
                placeholder="Single table"
              />
              <p class="hint">
                Mapeamento JPA da hierarquia (alvos Spring e CLI-JPA).
              </p>
            </div>
          </section>

          <p v-if="!ownsTable && !isIdRoot && !isSuperclass" class="empty">
            Sem opções de persistência para esta classe.
          </p>
        </TabPanel>

        <TabPanel value="spring">
          <!-- Geração do controller REST por classe -->
          <section v-if="controllerEligible" class="block">
            <header class="block-head">
              <h4>REST controller</h4>
            </header>
            <div class="entry">
              <div class="job-toggle">
                <Checkbox v-model="annotations.generateControllerByClass[className!]" :binary="true" inputId="gen-controller" />
                <label for="gen-controller">Generate REST controller</label>
              </div>
              <p class="hint">
                Se desativado, esta classe não expõe endpoints REST.
                A entidade, repositório e serviço continuam a ser gerados.
              </p>
            </div>
          </section>

          <!-- Endpoints REST previstos (CRUD + relações), derivados do modelo -->
          <section v-if="!isInterface && allEndpoints.length" class="block">
            <header class="block-head">
              <h4>Available endpoints</h4>
            </header>
            <ul class="endpoint-list">
              <li v-for="(e, i) in allEndpoints" :key="i" class="endpoint-item">
                <span class="verb" :class="e.httpMethod.toLowerCase()">{{ e.httpMethod }}</span>
                <code class="ep-path" :title="e.path">{{ e.path.replace('/api', '') }}</code>
              </li>
            </ul>
          </section>

          <!-- Campos por DTO: desmarcar esconde o atributo desse contrato (default = tudo visível) -->
          <section v-if="!isInterface && dtoFieldRows.length" class="block">
            <header class="block-head">
              <h4>DTO fields</h4>
            </header>
            <table class="dto-matrix">
              <thead>
                <tr><th></th><th>Create</th><th>Update</th><th>Resp</th></tr>
              </thead>
              <tbody>
                <tr v-for="row in dtoFieldRows" :key="row.name">
                  <td class="attr-label">{{ row.name }}<span v-if="row.hasInit" class="enum-hint">(init)</span><span v-else-if="row.hasDerive" class="enum-hint">(derive)</span></td>
                  <td v-for="role in DTO_ROLES" :key="role">
                    <span v-if="row.locked[role]" class="lock" title="fixed by init/derive">🔒</span>
                    <Checkbox v-else
                      :binary="true"
                      :modelValue="!annotations.isDTOHidden(className!, role, row.name)"
                      @update:modelValue="annotations.toggleDTOHidden(className!, role, row.name)"
                    />
                  </td>
                </tr>
              </tbody>
            </table>
            <p class="hint">Unchecked = hidden from that DTO. Init fields are already forced out of Create; derived are response-only.</p>
          </section>

          <!-- Invariantes OCL como anotações Bean Validation por classe -->
          <section v-if="beanValidationEligible" class="block">
            <header class="block-head">
              <h4>Bean Validation</h4>
            </header>
            <div class="entry">
              <div class="job-toggle">
                <Checkbox v-model="annotations.beanValidationByClass[className!]" :binary="true" inputId="bean-validation" />
                <label for="bean-validation">Invariantes OCL como anotações</label>
              </div>
              <p class="hint">
                Invariantes simples (ex: points &gt;= 0) viram anotações Bean Validation no campo;
                as restantes continuam no método validate().
              </p>
            </div>
          </section>

          <!-- Endpoint / Job por operação (mapeamento PSM) -->
          <section v-if="!isInterface" class="block">
            <header class="block-head">
              <h4>Operation endpoints</h4>
            </header>
            <p v-if="operations.length === 0" class="empty">No operations</p>
            <div v-for="op in operations" :key="op.id" class="entry">
              <div class="attr-label">
                {{ op.name }}
              </div>
              <div class="endpoint-cfg">
                <div class="job-toggle">
                  <Checkbox v-model="annotations.endpointByOperation[op.id]!.job" :binary="true" :inputId="`job-${op.id}`" />
                  <label :for="`job-${op.id}`">Job (@Scheduled)</label>
                </div>
                <div v-if="!annotations.endpointByOperation[op.id]!.job" class="endpoint-row">
                  <Select v-model="annotations.endpointByOperation[op.id]!.httpMethod" :options="httpMethods" placeholder="POST" />
                  <InputText v-model="annotations.endpointByOperation[op.id]!.endpointName" :placeholder="op.name" />
                </div>
                <InputText v-else v-model="annotations.endpointByOperation[op.id]!.cron" placeholder="cron — ex: 0 0 * * * *" />
              </div>
            </div>
          </section>
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

.sub-entry :deep(.p-select) {
  flex: 1;
  min-width: 0;
}
.sub-entry :deep(.p-select-label) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.endpoint-cfg {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.endpoint-row {
  display: flex;
  gap: 4px;
}

.endpoint-row :deep(.p-select),
.endpoint-row :deep(.p-inputtext) {
  flex: 1;
  min-width: 0;
}

.endpoint-cfg > :deep(.p-inputtext) {
  width: 100%;
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

/* lista de endpoints previstos (preview, read-only) */
.endpoint-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 5px; }
.endpoint-item { display: flex; align-items: flex-start; gap: 6px; font-size: 12px; line-height: 1.4; }
.endpoint-item .verb {
  flex: none; min-width: 48px; text-align: center; padding: 1px 5px; border-radius: 3px;
  font-weight: 700; font-size: 11px; color: #fff;
}
.verb.get { background: #3b82f6; }
.verb.post { background: #22c55e; }
.verb.put, .verb.patch { background: #f59e0b; }
.verb.delete { background: #ef4444; }
.ep-path { font-family: monospace; color: var(--color-text); overflow-wrap: anywhere; word-break: break-all; }

/* matriz de campos por DTO */
.dto-matrix { width: 100%; border-collapse: collapse; }
.dto-matrix th { font-size: 10px; font-weight: 600; opacity: .6; text-transform: uppercase; text-align: center; padding-bottom: 4px; }
.dto-matrix td { padding: 2px 0; text-align: center; }
.dto-matrix td.attr-label { text-align: left; }
.dto-matrix .lock { opacity: .5; font-size: 11px; cursor: default; }
</style>
