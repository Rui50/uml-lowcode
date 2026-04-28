<script setup lang="ts">
import { useModelStore } from '@/stores/model'
const store = useModelStore()
</script>

<template>
  <aside class="class-list">
    <h3>Classes</h3>
    <ul>
      <li
        v-for="cls in store.model?.classes ?? []"
        :key="cls.name"
        :class="{ selected: cls.name === store.selectedClassName }"
        @click="store.selectClass(cls.name)"
      >
        {{ cls.name }}
        <span v-if="cls.parent" class="parent"> : {{ cls.parent }}</span>
      </li>
    </ul>
  </aside>
</template>

<style scoped>
  .class-list {
    padding: 1rem;
    height: 100%;
    overflow-y: auto;
    color: var(--color-text);
  }
  .class-list h3 {
    margin-top: 0;
    font-size: 0.9rem;
    text-transform: uppercase;
    opacity: 0.6;
  }
  ul {
    list-style: none;
    padding: 0;
    margin: 0;
  }
  li {
    padding: 0.5rem 0.75rem;
    cursor: pointer;
    border-radius: 4px;
  }
  li:hover {
    background: var(--color-secondary-hover);
  }
  li.selected {
    background: var(--color-primary);
    color: #000;
    font-weight: 600;
  }
  .parent {
    opacity: 0.6;
    font-size: 0.85em;
  }
</style>