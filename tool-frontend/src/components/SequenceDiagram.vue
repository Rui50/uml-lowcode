<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import mermaid from 'mermaid'
import type { FragmentDTO, InteractionDTO, LifelineDTO, SequenceElement } from '@/types';

/** exemplo https://mermaid.js.org/syntax/examples.html
 * sequenceDiagram
    participant Alice
    participant Bob
    Alice->>John: Hello John, how are you?
    loop HealthCheck
        John->>John: Fight against hypochondria
    end
    Note right of John: Rational thoughts<br/>prevail...
    John-->>Alice: Great!
    John->>Bob: How about you?
    Bob-->>John: Jolly good!
 */
const props = defineProps<{
  interaction: InteractionDTO
}>()

const container = ref<HTMLDivElement>()

mermaid.initialize({ startOnLoad: false, theme: "dark", securityLevel: "loose" })

function buildMermaidDiagram(i: InteractionDTO): string {
  const lines: string[] = ['sequenceDiagram']

  const orderedLifelines = orderLifelinesBasedOnMessages(i)

  for (const lifeline of orderedLifelines) {
    const label = lifeline.type ? `${lifeline.represents || ""} : ${lifeline.type}`.trim() : (lifeline.name || lifeline.id)
    lines.push(`  participant ${lifeline.id} as ${label}`)
  }

  buildElements(i.elements, lines, '  ')
  return lines.join('\n')
}

function buildElements(elements: SequenceElement[], lines: string[], indent: string) {
  for (const elem of elements){
    if (elem.type === "message") {
      if (!elem.sourceLifelineId || !elem.targetLifelineId) continue
      const source = elem.sourceLifelineId
      const target = elem.targetLifelineId
      const arrow = elem.messageSort === 'reply' ? '-->>' : '->>'
      lines.push(`${indent}${source} ${arrow} ${target}: ${elem.label || ''}`)
    } else {
      buildFragment(elem, lines, indent)
    }
  }
}

function buildFragment(fragment: FragmentDTO, lines: string[], indent: string) {
  const operator = fragment.operator
  const inner = indent + '  '

  const keyword = operator
  // mermaid uses "else" for alt and "and" for parallel fragments (par not sure if will be implemented)
  const sep =
    operator === 'alt' ? 'else' :
    operator === 'par' ? 'and' :
    null

  for (let i = 0; i < fragment.operands.length; i++) {
    const operand = fragment.operands[i]
    // to silence typ
    if (!operand) continue
    const guard = operand?.guard ?? ""
    if (i === 0) {
      lines.push(`${indent}${keyword} ${guard}`.trimEnd())
    } else if (sep) {
      lines.push(`${indent}${sep} ${guard}`.trimEnd())
    }
    buildElements(operand.elements, lines, inner)
  }
  lines.push(`${indent}end`)
}

// para poder dar display da forma certa, senao o ator tava a ficar em ultimo lugar e diagrama ficava confuso
function orderLifelinesBasedOnMessages(i: InteractionDTO) : LifelineDTO[] {
  const orderedIds: string[] = []

  // mark as visited
  const visit = (id: string | null) => {
    if (id && !orderedIds.includes(id)) orderedIds.push(id)
  }

  function walkMessages(elements: SequenceElement[]) {
    for (const elem of elements) {
      if (elem.type === "message") {
        visit(elem.sourceLifelineId)
        visit(elem.targetLifelineId)
      } else {
        for (const operand of elem.operands) {
          walkMessages(operand.elements)
        }
      }
    }
  }
  walkMessages(i.elements)
  // no caso de ter lifelines perdidas nao usadas nao ficar a procura.
  for (const ll of i.lifelines) visit(ll.id)

  const result: LifelineDTO[] = []
  for (const id of orderedIds) {
    const ll = i.lifelines.find(l => l.id === id)
    if (ll) result.push(ll)
  }
  return result
}

async function render(){
  if(!container.value) return
  const code = buildMermaidDiagram(props.interaction)
  const { svg } = await mermaid.render('mermaidDiagram', code)
  container.value.innerHTML = svg
}

onMounted(render)
watch(() => props.interaction, render)

</script> 

<template>
  <div ref="container" class="seq-diagram"></div>
</template>

<style scoped>
.seq-diagram {
  flex: 1;
  overflow: auto;
  padding: 1rem;
  display: flex;
  justify-content: center;
  background: var(--background);
  /* dot grid para parecer como do diag de classes */
  background-image: radial-gradient(circle, #444 1px, transparent 1px);
  background-size: 16px 16px;
}

.seq-diagram :deep(svg) {
  max-width: 100%;
  height: auto;
}
</style>