<script setup lang="ts">
import { ref, computed } from 'vue'
import { useModelStore } from '@/stores/model'  

const store = useModelStore()
const collapsed = ref(false)

const isClassSelected = computed(() => store.selectedDiagram?.type === 'class')
const selectedSequenceId = computed(() =>
  store.selectedDiagram?.type === 'sequence' ? store.selectedDiagram.interactionId : null
)

</script>

 <template>
    <aside class="diagram-list" :class="{ collapsed }">
      <div class="header">
        <h3 v-if="!collapsed">Diagrams</h3>
        <button class="toggle" @click="collapsed = !collapsed" :title="collapsed ? 'Expand' : 'Collapse'">
          <i :class="collapsed ? 'pi pi-angle-right' : 'pi pi-angle-left'"></i>
        </button>
      </div>

      <ul v-if="!collapsed">
        <li
          :class="{ selected: isClassSelected }"
          @click="store.selectDiagram({ type: 'class' })"
        >
          <i class="pi pi-sitemap icon"></i> Class diagram
        </li>

        <li class="section-label">Sequence diagrams</li>
        <li
          v-for="i in store.model?.interactions ?? []"
          :key="i.id"
          :class="{ selected: i.id === selectedSequenceId }"
          @click="store.selectDiagram({ type: 'sequence', interactionId: i.id })"
        >
          <i class="pi pi-arrow-right-arrow-left icon"></i> {{ i.name }}
          <span v-if="i.contextClass" class="ctx">({{ i.contextClass }})</span>
        </li>
        <li v-if="!store.model?.interactions?.length" class="empty">
          No sequence diagrams
        </li>
      </ul>
    </aside>
  </template>

<style scoped>
  .diagram-list {
    border-right: 1px solid var(--color-border);
    background: var(--color-secondary);
    color: var(--color-text);
    width: 240px;
    flex-shrink: 0;
    height: 100%;
    overflow-y: auto;
    transition: width 0.15s;
  }
  .diagram-list.collapsed {
    width: 36px;
  }
  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.5rem 0.75rem;
    border-bottom: 1px solid var(--color-border);
    min-height: 40px;
  }
  .diagram-list.collapsed .header {
    justify-content: center;
    padding: 0.5rem 0;
  }
  h3 {
    margin: 0;
    font-size: 0.85rem;
    text-transform: uppercase;
    color: var(--color-text);
    opacity: 0.6;
  }
  .toggle {
    background: var(--color-secondary-hover);
    color: var(--color-text);
    border: 1px solid var(--color-border);
    border-radius: 3px;
    width: 24px;
    height: 24px;
    padding: 0;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
  .toggle .pi {
    font-size: 0.85rem;
    line-height: 1;
  }
  ul {
    list-style: none;
    padding: 0.5rem 0;
    margin: 0;
  }
  li {
    padding: 0.5rem 0.75rem;
    cursor: pointer;
    font-size: 0.9rem;
  }
  li:hover {
    background: var(--color-secondary-hover);
  }
  li.selected {
    background: var(--color-primary);
    color: #000;
    font-weight: 600;
  }
  li.selected .icon,
  li.selected .ctx {
    color: #000;
    opacity: 0.7;
  }
  .icon {
    display: inline-block;
    width: 1.2em;
    opacity: 0.6;
  }
  .section-label {
    cursor: default;
    opacity: 0.5;
    text-transform: uppercase;
    font-size: 0.7rem;
    letter-spacing: 0.05em;
    padding: 0.75rem 0.75rem 0.25rem;
  }
  .section-label:hover {
    background: transparent;
  }
  .ctx {
    opacity: 0.6;
    font-size: 0.85em;
    margin-left: 0.25rem;
  }
  .empty {
    cursor: default;
    opacity: 0.4;
    font-style: italic;
    font-size: 0.85em;
  }
  .empty:hover {
    background: transparent;
  }
</style>