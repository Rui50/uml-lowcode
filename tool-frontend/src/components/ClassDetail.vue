<script setup lang="ts">
import { computed } from 'vue'
import { useModelStore } from '@/stores/model'
import type { ClassDTO } from '@/types'

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
    padding: 1.5rem 2rem;
    flex: 1;
    overflow-y: auto;
  }
  .empty {
    color: #888;
    font-style: italic;
  }
  h2 {
    margin-top: 0;
  }
  h3 {
    margin-top: 1.5rem;
    font-size: 0.9rem;
    text-transform: uppercase;
    color: #666;
  }
  .parent {
    color: #555;
  }
  table {
    border-collapse: collapse;
    width: 100%;
    max-width: 500px;
  }
  th, td {
    text-align: left;
    padding: 0.4rem 0.75rem;
    border-bottom: 1px solid #eee;
  }
  th {
    font-size: 0.85em;
    color: #666;
  }
  ul {
    list-style: none;
    padding: 0;
  }
  li {
    padding: 0.4rem 0;
    border-bottom: 1px solid #f0f0f0;
  }
  .mult {
    color: #c0392b;
    margin: 0 0.3rem;
    font-family: monospace;
  }
  .roles {
    color: #888;
    margin-left: 0.5rem;
    font-size: 0.85em;
  }
  </style>