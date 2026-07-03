<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useModelStore } from '@/stores/model'
import { useAnnotationsStore, type ResponseSlot, type DTORole } from '@/stores/annotations'
import { useSelectedClass } from '@/composables/useSelectedClass'
import type { Endpoint } from '@/types'

import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Checkbox from 'primevue/checkbox'

const model = useModelStore()
const annotations = useAnnotationsStore()
const { className, isInterface, operations } = useSelectedClass()

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

// slot de override do response de um endpoint CRUD (null = endpoint sem response próprio)
function responseSlot(e: Endpoint): ResponseSlot | null {
  if (e.type !== 'CRUD') return null
  if (e.httpMethod === 'GET') {
    if (e.path.endsWith('{id}')) return 'Get'   // GET /x/{id}
    return 'List'                               // GET /x
  }
  if (e.httpMethod === 'POST') return 'Create'
  if (e.httpMethod === 'PATCH') return 'Update'
  return null   // DELETE não devolve corpo
}

// slot expandido na lista de endpoints (config do override de response)
const expandedSlot = ref<ResponseSlot | null>(null)
watch(className, () => { expandedSlot.value = null })

// nome auto do DTO de override; o responseType já é o fragmento (List/Get/Create/Update).
// tem de bater com SpringDTOGenerator.responseDTOName
function autoDtoName(slot: ResponseSlot) {
  return `${className.value}${slot}ResponseDTO`
}

// um clique liga o override (se ainda não existe) e abre/fecha os campos
function openOverride(slot: ResponseSlot) {
  annotations.ensureResponseOverride(className.value!, slot)
  expandedSlot.value = expandedSlot.value === slot ? null : slot
}
function removeOverride(slot: ResponseSlot) {
  annotations.removeResponseOverride(className.value!, slot)
  expandedSlot.value = null
}

// matriz "DTO fields": uma linha por atributo, células trancadas quando a semântica já
// (init sai sempre do Create; derive só existe no Response).
// Inclui atributos HERDADOS (os DTOs gerados também os têm); init/derive procuram-se
// pela chave da classe que DECLARA o atributo. O atributo-id custom (da raiz) não aparece.
const DTO_ROLES = ['create', 'update', 'response'] as const
const fieldRows = computed(() => {
  const classes = model.model?.classes ?? []
  const byName = new Map(classes.map(c => [c.name, c]))

  // classe selecionada + antepassados, de baixo para cima (topo = raiz)
  const hierarchyList: NonNullable<typeof model.selectedClass>[] = []
  for (let c = model.selectedClass; c; c = c.parent ? byName.get(c.parent) ?? null : null) {
    hierarchyList.push(c)
  }

  // id custom vive na raiz -> excluído da matriz em toda a hierarquia
  const idAttr = annotations.idByClass[hierarchyList.at(-1)?.name ?? '']?.attributeName

  // escondido por um antepassado (herda-se; o filho não pode des-esconder -> trancado)
  const ancestorHides = (role: DTORole, attr: string) =>
    hierarchyList.slice(1).some(c => annotations.dtoHiddenByClass[c.name]?.[role].includes(attr))

  return hierarchyList.flatMap((c, depth) =>
    c.attributes
      .filter(a => a.name !== idAttr)
      .map(a => {
        const k = `${c.name}.${a.name}`
        const hasInit = k in annotations.initByAttribute
        const hasDerive = k in annotations.deriveByAttribute
        return {
          name: a.name,
          inherited: depth > 0,
          hasInit,
          hasDerive,
          locked: {
            create: hasInit || hasDerive || ancestorHides('create', a.name),
            update: hasDerive || ancestorHides('update', a.name),
            response: ancestorHides('response', a.name),
          },
        }
      })
  )
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
</script>

<template>
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
        <div class="ep-row">
          <span class="verb" :class="e.httpMethod.toLowerCase()">{{ e.httpMethod }}</span>
          <code class="ep-path" :title="e.path">{{ e.path.replace('/api', '') }}</code>
          <!-- só nos 4 endpoints CRUD com corpo: um clique liga o override e abre os campos -->
          <a v-if="responseSlot(e)" class="ep-override-btn"
            :class="{ active: annotations.hasResponseOverride(className!, responseSlot(e)!) }"
            @click="openOverride(responseSlot(e)!)">
            {{ annotations.hasResponseOverride(className!, responseSlot(e)!) ? '● custom' : 'customize' }}
          </a>
        </div>
        <!-- painel do override: response próprio deste endpoint (independente do default) -->
        <div v-if="responseSlot(e) && expandedSlot === responseSlot(e)" class="override-panel">
          <label class="override-name">
            <span>name</span>
            <InputText :modelValue="annotations.overrideName(className!, responseSlot(e)!)"
              @update:modelValue="annotations.setOverrideName(className!, responseSlot(e)!, $event ?? '')"
              :placeholder="autoDtoName(responseSlot(e)!)" size="small" />
          </label>
          <div class="override-fields">
            <label v-for="row in fieldRows" :key="row.name" class="override-field">
              <Checkbox :binary="true"
                :modelValue="!annotations.isOverrideHidden(className!, responseSlot(e)!, row.name)"
                @update:modelValue="annotations.toggleOverrideHidden(className!, responseSlot(e)!, row.name)" />
              <span>{{ row.name }}<span v-if="row.inherited" class="enum-hint"> (herdado)</span></span>
            </label>
          </div>
          <a class="override-remove" @click="removeOverride(responseSlot(e)!)">remove</a>
        </div>
      </li>
    </ul>
  </section>

  <!-- Campos por DTO: desmarcar esconde o atributo desse contrato (default = tudo visível) -->
  <section v-if="!isInterface && fieldRows.length" class="block">
    <header class="block-head">
      <h4>Base DTO fields</h4>
    </header>
    <table class="dto-matrix">
      <thead>
        <tr><th></th><th>Create</th><th>Update</th><th>Resp</th></tr>
      </thead>
      <tbody>
        <tr v-for="row in fieldRows" :key="row.name">
          <td class="attr-label">{{ row.name }}<span v-if="row.inherited" class="enum-hint">(herdado)</span><span v-if="row.hasInit" class="enum-hint">(init)</span><span v-else-if="row.hasDerive" class="enum-hint">(derive)</span></td>
          <td v-for="role in DTO_ROLES" :key="role">
            <span v-if="row.locked[role]" class="lock" title="fixed by init/derive or hidden by parent">🔒</span>
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
</template>

<style scoped>
/* --- estrutura partilhada com o painel de anotações (duplicada por causa do scoped;
   consolidar num stylesheet comum se os outros tabs também forem extraídos) --- */
.block { padding: 4px 0; }
.block-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; padding: 0 4px; }
.block-head h4 { margin: 0; font-size: 12px; opacity: 0.8; text-transform: uppercase; letter-spacing: 0.5px; }
.entry {
  display: flex; flex-direction: column; gap: 6px; padding: 8px; margin-bottom: 8px;
  border: 1px solid var(--color-border); border-radius: 6px; background: var(--background);
}
.attr-label { font-family: monospace; font-size: 12px; }
.attr-label .enum-hint { opacity: 0.6; font-size: 11px; margin-left: 4px; }
.job-toggle { display: flex; align-items: center; gap: 6px; }
.empty { padding: 1.5rem 1rem; opacity: 0.5; font-style: italic; font-size: 12px; text-align: center; }
.hint { font-size: 11px; opacity: 0.65; margin: 0; line-height: 1.4; }
.hint.warn { opacity: 1; color: var(--p-orange-600, #c2410c); }

/* --- específico do Spring --- */
.endpoint-cfg { display: flex; flex-direction: column; gap: 6px; }
.endpoint-row { display: flex; gap: 4px; }
.endpoint-row :deep(.p-select),
.endpoint-row :deep(.p-inputtext) { flex: 1; min-width: 0; }
.endpoint-cfg > :deep(.p-inputtext) { width: 100%; }

/* lista de endpoints previstos (preview, read-only) */
.endpoint-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 5px; }
.endpoint-item { display: flex; flex-direction: column; font-size: 12px; line-height: 1.4; }
.ep-row { display: flex; align-items: flex-start; gap: 6px; }
.ep-override-btn { margin-left: auto; font-size: 11px; color: var(--color-primary); cursor: pointer; white-space: nowrap; }
.ep-override-btn.active { color: #f59e0b; font-weight: 600; }
.override-panel { margin: 4px 0 6px 54px; display: flex; flex-direction: column; gap: 6px; }
.override-name { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.override-name span { opacity: .7; }
.override-remove { font-size: 11px; color: #ef4444; cursor: pointer; align-self: flex-start; }
.override-fields { display: flex; flex-direction: column; gap: 4px; }
.override-field { display: flex; align-items: center; gap: 6px; font-family: monospace; font-size: 12px; }
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
