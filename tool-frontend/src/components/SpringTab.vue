<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useModelStore } from '@/stores/model'
import { useAnnotationsStore, type EndpointType, type DTORole } from '@/stores/annotations'
import { useSelectedClass } from '@/composables/useSelectedClass'
import type { Endpoint } from '@/types'

import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Checkbox from 'primevue/checkbox'

const model = useModelStore()
const annotations = useAnnotationsStore()
const { className, classKey, isInterface, operations } = useSelectedClass()

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

// endpointType de override do response de um endpoint CRUD (null = endpoint sem response próprio)
function endpointTypeOf(e: Endpoint): EndpointType | null {
  if (e.type !== 'CRUD') return null
  if (e.httpMethod === 'GET') {
    if (e.path.endsWith('{id}')) return 'Get'   // GET /x/{id}
    return 'List'                               // GET /x
  }
  if (e.httpMethod === 'POST') return 'Create'
  if (e.httpMethod === 'PATCH') return 'Update'
  return null   // DELETE não devolve corpo
}

// endpointType expandido na lista de endpoints (config do override de response)
const expandedEndpointType = ref<EndpointType | null>(null)
watch(className, () => { expandedEndpointType.value = null })

// nome auto do DTO de override; o responseType já é o fragmento (List/Get/Create/Update).
// tem de bater com SpringDTOGenerator.responseDTOName
function autoDtoName(endpointType: EndpointType) {
  return `${className.value}${endpointType}ResponseDTO`
}

// um clique liga o override (se ainda não existe) e abre/fecha os campos
function openOverride(endpointType: EndpointType) {
  annotations.ensureResponseOverride(classKey.value!, endpointType)
  expandedEndpointType.value = expandedEndpointType.value === endpointType ? null : endpointType
}
function removeOverride(endpointType: EndpointType) {
  annotations.removeResponseOverride(classKey.value!, endpointType)
  expandedEndpointType.value = null
}

// Linhas da tabela "Base DTO fields": uma por atributo e relação (da classe E dos pais — os DTOs
// gerados também incluem os campos herdados). Cada linha tem 3 toggles, Create/Update/Response:
// marcado = o campo entra nesse DTO. O id custom (vive na raiz) não é campo de DTO -> não aparece.
// init/derive procuram-se pela chave da classe que DECLARA o atributo.
//
// 'locked' = toggle que o user não pode desmarcar porque a semântica já o decide:
//   - init   -> o campo nunca entra no Create (o valor é obrigado na criação);
//   - derive -> o campo só existe no Response;
//   - escondido por um pai -> o filho não o pode voltar a mostrar.
const DTO_ROLES = ['create', 'update', 'response'] as const
const fieldRows = computed(() => {
  const classes = model.model?.classes ?? []
  const byName = new Map(classes.map(c => [c.name, c]))

  // classe selecionada + antepassados, de baixo para cima (topo = raiz)
  const hierarchyList: NonNullable<typeof model.selectedClass>[] = []
  for (let c = model.selectedClass; c; c = c.parent ? byName.get(c.parent) ?? null : null) {
    hierarchyList.push(c)
  }

  // id custom vive na raiz -> excluído da matriz em toda a hierarquia (
  const idAttr = annotations.idByClass[hierarchyList.at(-1)?.id ?? '']?.attributeName

  // escondido por um antepassado (herda-se; o filho não pode des-esconder -> trancado)
  const ancestorHides = (role: DTORole, attr: string) =>
    hierarchyList.slice(1).some(c => annotations.dtoHiddenByClass[c.id]?.[role]?.includes(attr))

  return hierarchyList.flatMap((c, depth) => {
    const attrRows = c.attributes
      .filter(a => a.name !== idAttr)
      .map(a => {
        const k = `${c.id}.${a.name}`
        const hasInit = k in annotations.initByAttribute
        const hasDerive = k in annotations.deriveByAttribute
        return {
          name: a.name, inherited: depth > 0, isRelation: false, collection: false, hasInit, hasDerive,
          locked: {
            create: hasInit || hasDerive || ancestorHides('create', a.name),
            update: hasDerive || ancestorHides('update', a.name),
            response: ancestorHides('response', a.name),
          },
        }
      })
    const relRows = c.relations.map(rel => ({
      name: rel.name, inherited: depth > 0, isRelation: true, collection: rel.collection,
      hasInit: false, hasDerive: false,
      locked: {
        create: ancestorHides('create', rel.name),
        update: rel.collection || ancestorHides('update', rel.name),  // coleção não está no UpdateDTO
        response: ancestorHides('response', rel.name),
      },
    }))
    return [...attrRows, ...relRows]
  })
})

const httpMethods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']

// valida o cron, o Spring usa 6 campos (seg min hora dia mês dia da semana),
// Vazio = o gerador usa o default, '-' = desativado; macros @daily etc. válidas.
function cronError(cron: string): string | null {
  const c = cron.trim()
  if (!c || c === '-') return null
  if (/^@(yearly|annually|monthly|weekly|daily|hourly)$/i.test(c)) return null
  const f = c.split(/\s+/)
  if (f.length !== 6) return `Cron do Spring tem 6 campos (seg min hora dia mês dia-da-semana) — este tem ${f.length}. Ex: 0 0 * * * *`
  if (!f.every(x => /^[\d*,\-/?LW#a-zA-Z]+$/.test(x))) return 'Cron com caracteres inválidos'
  return null
}
// garante uma config de endpoint para cada operação da classe selecionada
watch(operations, (ops) => {
  for (const op of ops) {
    if (!annotations.endpointByOperation[op.id]) {
      annotations.endpointByOperation[op.id] = { job: false, endpointName: '', httpMethod: 'POST', cron: '' }
    }
  }
}, { immediate: true })

// controller REST por classe (Spring); abstratas/interfaces não têm controller
const controllerEligible = computed(() =>
  !isInterface.value && !!model.selectedClass && !model.selectedClass.isAbstract
)

// garante a entrada do controller (default true) para a classe selecionada
watch(classKey, (id) => {
  if (id && controllerEligible.value && annotations.generateControllerByClass[id] === undefined) {
    annotations.generateControllerByClass[id] = true
  }
}, { immediate: true })

// Bean Validation por classe (Spring); aplica-se a qualquer classe (inclui abstratas)
const beanValidationEligible = computed(() => !isInterface.value && !!model.selectedClass)

// garante a entrada do bean validation (default true) para a classe selecionada
watch(classKey, (id) => {
  if (id && beanValidationEligible.value && annotations.beanValidationByClass[id] === undefined) {
    annotations.beanValidationByClass[id] = true
  }
}, { immediate: true })
</script>

<template>
  <!-- Geração do controller REST por classe -->
  <section v-if="controllerEligible" class="block">
    <header class="block-head">
      <h4>REST controller</h4>
    </header>
    <div class="entry">
      <div class="job-toggle">
        <Checkbox v-model="annotations.generateControllerByClass[classKey!]" :binary="true" inputId="gen-controller" />
        <label for="gen-controller">Generate REST controller</label>
        <span class="tip" v-tooltip.top="'Se desativado, esta classe não expõe endpoints REST. A entidade, repositório e serviço continuam a ser gerados.'">ⓘ</span>
      </div>
    </div>
  </section>

  <!-- Endpoints REST previstos (CRUD + relações), derivados do modelo -->
  <section v-if="!isInterface && allEndpoints.length && annotations.generateControllerByClass[classKey!] !== false" class="block">
    <header class="block-head">
      <h4>Available endpoints
        <span class="tip" v-tooltip.top="`'customize' molda só a RESPOSTA de cada endpoint CRUD (ResponseDTO próprio). O corpo de Create/Update define-se em 'Base DTO fields'.`">ⓘ</span>
      </h4>
    </header>
    <ul class="endpoint-list">
      <li v-for="(e, i) in allEndpoints" :key="i" class="endpoint-item">
        <div class="ep-row">
          <span class="verb" :class="e.httpMethod.toLowerCase()">{{ e.httpMethod }}</span>
          <code class="ep-path" v-tooltip.top="e.path">{{ e.path.replace('/api', '') }}</code>
          <!-- só nos 4 endpoints CRUD com corpo: um clique liga o override e abre os campos -->
          <a v-if="endpointTypeOf(e)" class="ep-override-btn"
            :class="{ active: annotations.hasResponseOverride(classKey!, endpointTypeOf(e)!) }"
            @click="openOverride(endpointTypeOf(e)!)">
            {{ annotations.hasResponseOverride(classKey!, endpointTypeOf(e)!) ? '● custom' : 'customize' }}
          </a>
        </div>
        <!-- painel do override: response próprio deste endpoint (independente do default) -->
        <div v-if="endpointTypeOf(e) && expandedEndpointType === endpointTypeOf(e)" class="override-panel">
          <label class="override-name">
            <span>name</span>
            <InputText :modelValue="annotations.overrideName(classKey!, endpointTypeOf(e)!)"
              @update:modelValue="annotations.setOverrideName(classKey!, endpointTypeOf(e)!, $event ?? '')"
              :placeholder="autoDtoName(endpointTypeOf(e)!)" size="small" />
          </label>
          <div class="override-fields">
            <label v-for="row in fieldRows" :key="row.name" class="override-field">
              <Checkbox :binary="true"
                :modelValue="!annotations.isOverrideHidden(classKey!, endpointTypeOf(e)!, row.name)"
                @update:modelValue="annotations.toggleOverrideHidden(classKey!, endpointTypeOf(e)!, row.name)" />
              <span>{{ row.name }}<span v-if="row.inherited" class="enum-hint"> (herdado)</span></span>
            </label>
          </div>
          <a class="override-remove" @click="removeOverride(endpointTypeOf(e)!)">remove</a>
        </div>
      </li>
    </ul>
  </section>

  <!-- Campos por DTO: desmarcar esconde o atributo desse contrato (default = tudo visível) -->
  <section v-if="!isInterface && fieldRows.length" class="block">
    <header class="block-head">
      <h4>Base DTO fields
        <span class="tip" v-tooltip.top="'Unchecked = hidden from that DTO. Init fields are already forced out of Create; derived are response-only.'">ⓘ</span>
      </h4>
    </header>
    <table class="dto-matrix">
      <thead>
        <tr><th></th><th>Create</th><th>Update</th><th>Resp</th></tr>
      </thead>
      <tbody>
        <tr v-for="row in fieldRows" :key="row.name">
          <td class="attr-label">{{ row.name }}<span v-if="row.isRelation" class="enum-hint">({{ row.collection ? 'coleção' : 'ref' }})</span><span v-if="row.inherited" class="enum-hint">(herdado)</span><span v-if="row.hasInit" class="enum-hint">(init)</span><span v-else-if="row.hasDerive" class="enum-hint">(derive)</span></td>
          <td v-for="role in DTO_ROLES" :key="role">
            <span v-if="row.locked[role]" class="lock" v-tooltip.top="'fixed by init/derive or hidden by parent'">🔒</span>
            <Checkbox v-else
              :binary="true"
              :modelValue="!annotations.isDTOHidden(classKey!, role, row.name)"
              @update:modelValue="annotations.toggleDTOHidden(classKey!, role, row.name)"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </section>

  <!-- Invariantes OCL como anotações Bean Validation por classe -->
  <section v-if="beanValidationEligible" class="block">
    <header class="block-head">
      <h4>Bean Validation</h4>
    </header>
    <div class="entry">
      <div class="job-toggle">
        <Checkbox v-model="annotations.beanValidationByClass[classKey!]" :binary="true" inputId="bean-validation" />
        <label for="bean-validation">Invariantes OCL como anotações</label>
        <span class="tip" v-tooltip.top="'Invariantes simples (ex: points >= 0) viram anotações Bean Validation no campo; as restantes continuam no método validate().'">ⓘ</span>
      </div>
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
        <div v-else class="cron-field">
          <InputText v-model="annotations.endpointByOperation[op.id]!.cron" placeholder="cron — ex: 0 0 * * * *"
                     :invalid="!!cronError(annotations.endpointByOperation[op.id]!.cron)" />
          <small v-if="cronError(annotations.endpointByOperation[op.id]!.cron)" class="cron-err">
            {{ cronError(annotations.endpointByOperation[op.id]!.cron) }}
          </small>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* --- estrutura partilhada com o painel de anotações (duplicada por causa do scoped;
   consolidar num stylesheet comum se os outros tabs também forem extraídos) --- */
.block {
  padding: 16px 0;
}
.block + .block {
  border-top: 1px solid var(--color-border);
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
.attr-label {
  font-family: monospace;
  font-size: 12px;
}
.attr-label .enum-hint {
  opacity: 0.6;
  font-size: 11px;
  margin-left: 4px;
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
.tip {
  font-size: 11px;
  opacity: 0.5;
  cursor: help;
  margin-left: 4px;
  font-weight: normal;
}
.hint.warn {
  opacity: 1;
  color: var(--p-orange-600, #c2410c);
}

/* --- específico do Spring --- */
.endpoint-cfg {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.endpoint-row {
  display: flex;
  gap: 4px;
}
.cron-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.cron-field :deep(.p-inputtext) {
  width: 100%;
}
.cron-err {
  color: var(--p-red-500, #e24c4c);
  font-size: 0.72rem;
  line-height: 1.3;
}
.endpoint-row :deep(.p-select),
.endpoint-row :deep(.p-inputtext) {
  flex: 1;
  min-width: 0;
}
.endpoint-cfg > :deep(.p-inputtext) {
  width: 100%;
}

/* lista de endpoints previstos (preview, read-only) */
.endpoint-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.endpoint-item {
  display: flex;
  flex-direction: column;
  font-size: 12px;
  line-height: 1.4;
}
.ep-row {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}
.ep-override-btn {
  margin-left: auto;
  font-size: 11px;
  color: var(--color-primary);
  cursor: pointer;
  white-space: nowrap;
}
.ep-override-btn.active {
  color: #f59e0b;
  font-weight: 600;
}
.override-panel {
  margin: 4px 0 6px 54px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.override-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
.override-name span {
  opacity: 0.7;
}
.override-remove {
  font-size: 11px;
  color: #ef4444;
  cursor: pointer;
  align-self: flex-start;
}
.override-fields {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.override-field {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: monospace;
  font-size: 12px;
}
.endpoint-item .verb {
  flex: none;
  min-width: 48px;
  text-align: center;
  padding: 1px 5px;
  border-radius: 3px;
  font-weight: 700;
  font-size: 11px;
  color: #fff;
}
.verb.get {
  background: #3b82f6;
}
.verb.post {
  background: #22c55e;
}
.verb.put, .verb.patch {
  background: #f59e0b;
}
.verb.delete {
  background: #ef4444;
}
.ep-path {
  font-family: monospace;
  color: var(--color-text);
  overflow-wrap: anywhere;
  word-break: break-all;
}

/* matriz de campos por DTO */
.dto-matrix {
  width: 100%;
  border-collapse: collapse;
}
.dto-matrix th {
  font-size: 10px;
  font-weight: 600;
  opacity: 0.6;
  text-transform: uppercase;
  text-align: center;
  padding-bottom: 4px;
}
.dto-matrix td {
  padding: 2px 0;
  text-align: center;
}
.dto-matrix td.attr-label {
  text-align: left;
}
.dto-matrix .lock {
  opacity: 0.5;
  font-size: 11px;
  cursor: default;
}
</style>
