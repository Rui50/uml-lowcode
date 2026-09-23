<script setup lang="ts">
  import FileUploader from '@/components/FileUploader.vue';
  import { useModelStore } from '@/stores/model';
  import ProgressSpinner from 'primevue/progressspinner';
  import Message from 'primevue/message';
  import { useToast } from 'primevue/usetoast';

  const modelStore = useModelStore();
  const toast = useToast();

  async function selectFile(file: File) {
    await modelStore.parseFile(file);
    // so resumo, a lista está no "Diagram Issues"
    const warnings = modelStore.model?.warnings ?? [];
    if (warnings.length) {
      toast.add({
        severity: 'warn',
        summary: 'Model Warnings',
        detail: `${warnings.length} warning(s) — see "Diagram Issues"`,
        life: 6000,
      });
    }
  }
</script>

<template>
  <main class="page">
    <header class="masthead">
      <!--<span class="kicker">UML&nbsp;->&nbsp;Code</span>-->
      <!--<h1 class="title">UML-LowCode</h1>-->
      <p class="lede">Upload a UML model to begin.</p>
      <p class="accepts">
        accepts <strong>Visual&nbsp;Paradigm</strong> <code>XMI</code> exports
      </p>
    </header>

    <details class="export">
      <summary class="export__summary">
        <span class="export__label">How to export from Visual Paradigm</span>
        <svg class="chev" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="1.7"
                stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </summary>

      <ol class="steps">
        <li><code>Project -> Export -> XMI…</code></li>
        <li>Select <code>Export for UML2</code></li>
        <li>Include both the <strong>class</strong> and <strong>sequence</strong> diagrams.</li>
      </ol>
    </details>

    <FileUploader @file-selected="selectFile" />

    <ProgressSpinner v-if="modelStore.loading" />

    <Message v-if="modelStore.error" severity="error" :closable="false">
      {{ modelStore.error }}
    </Message>
  </main>
</template>

<style scoped>
.page {
  --accent: var(--p-primary-color, #2f6f87);
  --surface: var(--p-content-background, #ffffff);
  --line: var(--p-content-border-color, #e4e8ee);
  --ink: var(--p-text-color, #1f2933);
  --muted: var(--p-text-muted-color, #6b7787);
  --mono: ui-monospace, "JetBrains Mono", "SF Mono", "Fira Code", Menlo, Consolas, monospace;

  width: 600px;
  max-width: 100%;
  margin: auto;
  padding: 2rem 0;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 1.5rem;
  color: var(--ink);
}

.masthead { text-align: center; display: flex; flex-direction: column; gap: 0.35rem; }
.kicker {
  align-self: center;
  font-family: var(--mono);
  font-size: 0.7rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--accent);
  opacity: 0.9;
}
.title { margin: 0; font-weight: 700; font-size: 2rem; letter-spacing: -0.02em; }
.lede { margin: 0; color: var(--muted); }

.accepts {
  margin: 0.2rem 0 0;
  align-self: center;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--muted);
}
.accepts strong { color: var(--accent); font-weight: 600; }
.accepts code {
  font-family: var(--mono);
  font-size: 0.78em;
  letter-spacing: 0.06em;
  color: var(--accent);
  padding: 0.05rem 0.32rem;
  border: 1px solid color-mix(in srgb, var(--accent) 28%, transparent);
  border-radius: 4px;
}

.export {
  border: 1px solid var(--line);
  border-radius: 10px;
  background:
    radial-gradient(color-mix(in srgb, var(--line) 70%, transparent) 1px, transparent 1px) 0 0 / 14px 14px,
    var(--surface);
  overflow: hidden;
}
.export__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.8rem 1rem;
  cursor: pointer;
  user-select: none;
  list-style: none;
  font-weight: 600;
  font-size: 0.9rem;
}
.export__summary::-webkit-details-marker { display: none; }
.export__summary:hover { color: var(--accent); }
.chev {
  flex: none;
  width: 18px;
  height: 18px;
  color: var(--muted);
  transition: transform 0.25s ease, color 0.25s ease;
}
.export[open] .chev { transform: rotate(90deg); color: var(--accent); }

.steps {
  margin: 0;
  padding: 1.1rem 1.15rem 1.1rem 1.15rem;
  list-style: none;
  counter-reset: step;
  border-top: 1px solid var(--line);
  text-align: left;
}
.steps li {
  position: relative;
  counter-increment: step;
  padding: 0 0 0.95rem 2.1rem;
  font-size: 0.88rem;
  line-height: 1.45;
  color: var(--ink);
}
.steps li:last-child { padding-bottom: 0.15rem; }
.steps li::before {
  content: counter(step);
  position: absolute;
  left: 0;
  top: -0.1rem;
  width: 1.45rem;
  height: 1.45rem;
  display: grid;
  place-items: center;
  font-family: var(--mono);
  font-size: 0.72rem;
  color: var(--accent);
  background: var(--surface);
  border: 1px solid color-mix(in srgb, var(--accent) 45%, var(--line));
  border-radius: 50%;
}
.steps li:not(:last-child)::after {
  content: "";
  position: absolute;
  left: 0.72rem;
  top: 1.45rem;
  bottom: 0.2rem;
  width: 1px;
  background: var(--line);
}
.steps code {
  font-family: var(--mono);
  font-size: 0.82em;
  padding: 0.08rem 0.35rem;
  border-radius: 4px;
  background: color-mix(in srgb, var(--accent) 7%, transparent);
  color: color-mix(in srgb, var(--accent) 88%, var(--ink));
  white-space: nowrap;
}

.export[open] .steps li {
  animation: step-rise 0.34s cubic-bezier(0.2, 0.7, 0.3, 1) both;
}
.export[open] .steps li:nth-child(1) { animation-delay: 0.02s; }
.export[open] .steps li:nth-child(2) { animation-delay: 0.07s; }
.export[open] .steps li:nth-child(3) { animation-delay: 0.12s; }
.export[open] .steps li:nth-child(4) { animation-delay: 0.17s; }

@keyframes step-rise {
  from { opacity: 0; transform: translateY(5px); }
  to   { opacity: 1; transform: translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .chev, .export[open] .steps li { transition: none; animation: none; }
}
</style>
