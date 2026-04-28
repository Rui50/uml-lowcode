<script setup lang="ts">
import { computed } from 'vue'
import { useModelStore } from '@/stores/model'
import type { ClassDTO } from '@/types'
// INDICAR AS QUE SAO NAVEGAVEIS FALTA
const store = useModelStore()

const selectedClass = computed<ClassDTO | null>(() => store.selectedClass ?? null)

const relatedAssociations = computed(() => {
  const name = selectedClass.value?.name
  if (!name) return []
  return store.model?.associations.filter(
    a => a.source === name || a.target === name
  ) ?? []
})
</script>

<template>
  <section class="class-detail">
    <div v-if="!selectedClass" class="empty">
      Select a class on the left to view its details.
    </div>

    <div v-else>
      <h2>{{ selectedClass.name }}</h2>
      <p v-if="selectedClass.parent" class="parent">
        extends <strong>{{ selectedClass.parent }}</strong>
      </p>

      <h3>Attributes</h3>
      <table v-if="selectedClass.attributes.length">
        <thead>
          <tr><th>Name</th><th>Type</th></tr>
        </thead>
        <tbody>
          <tr v-for="attr in selectedClass.attributes" :key="attr.name">
            <td>{{ attr.name }}</td>
            <td><code>{{ attr.type }}</code></td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">No attributes.</p>

      <h3>Associations</h3>
      <ul v-if="relatedAssociations.length">
        <li v-for="(a, i) in relatedAssociations" :key="i">
          <strong>{{ a.source }}</strong>
          <span class="mult">{{ a.sourceMultiplicity || '' }}</span>
          —
          <span class="mult">{{ a.targetMultiplicity || '' }}</span>
          <strong>{{ a.target }}</strong>
          <span v-if="a.sourceRole || a.targetRole" class="roles">
            ({{ a.sourceRole || '?' }} / {{ a.targetRole || '?' }})
          </span>
        </li>
      </ul>
      <p v-else class="empty">No associations.</p>
    </div>
  </section>
</template>

<style scoped>
  .class-detail {
    padding: 1.5rem 1.25rem;
    color: var(--color-text);
  }
  .empty {
    opacity: 0.5;
    font-style: italic;
  }
  h2 {
    margin-top: 0;
  }
  h3 {
    margin-top: 1.5rem;
    font-size: 0.9rem;
    text-transform: uppercase;
    opacity: 0.6;
  }
  .parent {
    opacity: 0.7;
  }
  table {
    border-collapse: collapse;
    width: 100%;
    max-width: 500px;
  }
  th, td {
    text-align: left;
    padding: 0.4rem 0.75rem;
    border-bottom: 1px solid var(--color-border);
  }
  th {
    font-size: 0.85em;
    opacity: 0.6;
  }
  ul {
    list-style: none;
    padding: 0;
  }
  li {
    padding: 0.4rem 0;
    border-bottom: 1px solid var(--color-border);
  }
  .mult {
    color: var(--color-primary);
    margin: 0 0.3rem;
    font-family: monospace;
  }
  .roles {
    opacity: 0.6;
    margin-left: 0.5rem;
    font-size: 0.85em;
  }
</style>