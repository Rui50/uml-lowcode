package generator.spring;

import generator.GeneratorUtils;
import model.Association;
import model.Class;
import model.Operation;
import model.sequence.*;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * diagrama de sequência (com service, com lifelines
 * de repositórios) para o corpo de um metodo Java. Usado pelos services (operações custom)
 * e pelos jobs (@Scheduled).
 */
public class SequenceBodyGenerator {

    private final List<Association> associations;
    // variáveis já declaradas fora do corpo ( pelo carregamento da entidade self) — não redeclarar
    private final Set<String> preDeclaredVars = new HashSet<>();
    // classe do metodo em geração (owner do diagrama) usada para resolver associações
    // herdadas — a lifeline pode ser uma subclasse e a associação estar declarada no pai
    private Class currentClass;

    public SequenceBodyGenerator(List<Association> associations) {
        this.associations = associations;
    }

    // regista uma variável como já declarada ( self carregado antes do corpo)
    public void addPreDeclaredVar(String varName) {
        preDeclaredVars.add(varName);
    }

    public void generateMethodBodyFromInteraction(PrintWriter writer, Class cls, Operation op, Interaction
            interaction) {
        // identificar a lifeline do Service (owner deste metodo)
        this.currentClass = cls;
        String className = cls.getName();
        String serviceName = className + "Service";

        // encontrar a lifeline do service owner
        Lifeline serviceLifeline = null;
        for (Lifeline ll : interaction.getLifelines()) {
            if (ll.getType() != null && ll.getType().equals(serviceName)) {
                serviceLifeline = ll;
                break;
            }
        }

        // todos os diagramas precisam de service,
        // Os jobs tecnicamente nao mas falta ver isso
        if (serviceLifeline == null) {
            throw new IllegalArgumentException(
                    "Operação '" + op.getName() + "': o diagrama de sequência não tem uma lifeline do tipo '"
                    + serviceName + "' (o owner do método). Sem ela não é possível gerar o corpo.");
        }

        // mapear lifeline ID -> nome da variável java para os repositories
        // ex: TurmaRepository -> turmaRepository, AlunoRepository -> alunoRepository
        Map<String, String> lifelineVarMap = new HashMap<>();
        for (Lifeline ll : interaction.getLifelines()) {
            if (ll.getType() != null && ll.getType().endsWith("Repository")) {
                String repoVar = GeneratorUtils.lowercaseFirst(ll.getType());
                lifelineVarMap.put(ll.getId(), repoVar);
            }
            if (ll.getType() != null && ll.getType().endsWith("Service")) {
                lifelineVarMap.put(ll.getId(), "self");
            }
        }

        // percorrer a árvore de elements (Messages e Fragments)
        generateElements(writer, interaction.getElements(), serviceLifeline, lifelineVarMap, 2);
    }

    // percorre a lista de elementos (mensagens ou fragmentos) e gera o código correspondente
    // indent: nível de indentação (0, 1, 2...) — cada nível = 4 espaços
    private void generateElements(PrintWriter writer, List<Object> elements,
                                  Lifeline serviceLifeline, Map<String, String> lifelineVarMap, int indent) {
        String indentStr = "    ".repeat(indent);
        for (Object elem : elements) {
            if (elem instanceof Message) {
                generateMessageCode(writer, (Message) elem, serviceLifeline, lifelineVarMap, indentStr);
            } else if (elem instanceof Fragment) {
                generateFragmentCode(writer, (Fragment) elem, serviceLifeline, lifelineVarMap, indent);
            }
        }
    }

    // gera codigo java para mensagens
    // ignora mensagens que nao fazem parte do service
    private void generateMessageCode(PrintWriter writer, Message msg, Lifeline serviceLifeline,
                                      Map<String, String> lifelineVarMap, String indent) {
        if (msg.getSource() == null || msg.getTarget() == null) return;
        if ("reply".equals(msg.getMessageSort())) return;

        // só processar mensagens enviadas pelo service
        if (serviceLifeline == null || !msg.getSource().getId().equals(serviceLifeline.getId())) return;

        // mensagem «create»: gera "Type var = new Type(args)".
        // Antes de isPlainAssignment porque "loan =" parece atribuiçã
        // Só compila se Type tiver construtor público com esses args (em Spring só no-arg) — nota #9 no Main.
        if ("createMessage".equals(msg.getMessageSort())) {
            String type = msg.getTarget().getType();
            String var = msg.getAssignVariable() != null ? msg.getAssignVariable()
                    : (msg.getTarget().getRepresents() != null && !msg.getTarget().getRepresents().isBlank()
                        ? msg.getTarget().getRepresents()
                        : GeneratorUtils.lowercaseFirst(type));
            StringBuilder a = new StringBuilder();
            for (int i = 0; i < msg.getArguments().size(); i++) {
                if (i > 0) a.append(", ");
                a.append(msg.getArguments().get(i).getName());
            }
            writer.println(indent + type + " " + var + " = new " + type + "(" + a + ");");
            return;
        }

        // atribuição simples (sem chamada): "x = expr" ou "x = expr : Type"
        // ": Type" presente -> declara; ausente -> reatribui
        if (msg.isPlainAssignment()) {
            String declType = (msg.getReturnType() != null && !msg.getReturnType().isEmpty())
                    ? msg.getReturnType() + " " : "";
            writer.println(indent + declType + msg.getAssignVariable() + " = " + msg.getAssignExpression() + ";");
            return;
        }

        // var da lifeline alvo: repos/service vêm do map; entidades carregadas usam o nome
        // (represents) para distinguir duas da mesma classe (turma vs destino), senão o tipo
        String targetVar = lifelineVarMap.get(msg.getTarget().getId());
        if (targetVar == null) {
            String represents = msg.getTarget().getRepresents();
            targetVar = (represents != null && !represents.isBlank())
                    ? represents
                    : GeneratorUtils.lowercaseFirst(msg.getTarget().getType());
        }

        // construir lista dos argumentos
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < msg.getArguments().size(); i++) {
            if (i > 0) args.append(", ");
            args.append(msg.getArguments().get(i).getName());
        }

        String methodName = msg.getName();
        boolean isSelfMessage = msg.getSource().getId().equals(msg.getTarget().getId());

        // heurística para as coleções: <campo>.add/remove/get numa entidade carregada faz acesso com getter
        if (methodName != null && msg.getArguments().size() == 1) {
            int sep = methodName.indexOf('.');
            if (sep > 0) {
                String attrName = methodName.substring(0, sep);
                String op = methodName.substring(sep + 1);
                Association assoc = findAssocByField(msg.getTarget().getType(), attrName);
                if (assoc != null) {
                    String argName = msg.getArguments().get(0).getName();
                    String base = targetVar + ".get" + GeneratorUtils.capitalizeFirst(attrName) + "()";

                    // get(chave)  leitura, é uma atribuição (Tipo var = ...)
                    if ("get".equals(op) && msg.getAssignVariable() != null) {
                        String type = (msg.getReturnType() != null && !msg.getReturnType().isEmpty())
                                ? msg.getReturnType() + " " : "";
                        writer.println(indent + type + msg.getAssignVariable() + " = " + base + ".get(" + argName + ");");
                        return;
                    }

                    // add/remove(x) ->  (List add/remove ou Map put/remove com a chave)
                    if ("add".equals(op) || "remove".equals(op)) {
                        String code;
                        if (assoc.hasQualifier()) {
                            String keyGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName()) + "()";
                            code = "add".equals(op)
                                    ? base + ".put(" + argName + "." + keyGetter + ", " + argName + ");"
                                    : base + ".remove(" + argName + "." + keyGetter + ");";
                        } else {
                            code = base + "." + op + "(" + argName + ");";
                        }
                        writer.println(indent + code);
                        return;
                    }
                }
            }
        }

        if (isSelfMessage) {
            // self-message: turma.removeAluno(aluno) -> código direto
            writer.println(indent + methodName + "(" + args + ");");
        } else if (msg.getReturnType() != null && !msg.getReturnType().isEmpty()) {
            // chamada com retorno: findById(id) : Turma
            String returnType = msg.getReturnType();
            String varName = msg.getAssignVariable() != null
                    ? msg.getAssignVariable()
                    : GeneratorUtils.lowercaseFirst(returnType);

            // skip cuz this message would duplicate a variable already declared by a precondition check at the start of the method
            if (preDeclaredVars.contains(varName)) return;

            if (methodName.equals("findById")) {
                writer.println(indent + returnType + " " + varName + " = " + targetVar + "." +
                        methodName + "(" + args + ")");
                writer.println(indent + "    .orElseThrow(() -> new RuntimeException(\"" + returnType + " not found\"));");
            } else {
                writer.println(indent + returnType + " " + varName + " = " + targetVar + "." +
                        methodName + "(" + args + ");");
            }
        } else {
            // chamada sem retorno: save(turma)
            writer.println(indent + targetVar + "." + methodName + "(" + args + ");");
        }
    }

    /*
    * gera código para fragmentos (alt, opt, loop)
     */
    private void generateFragmentCode(PrintWriter writer, Fragment frag, Lifeline serviceLifeline,
                                      Map<String, String> lifelineVarMap, int indent) {
        String indentStr = "    ".repeat(indent);
        String operator = frag.getOperator();

        if ("loop".equals(operator)){
            Operand operand = frag.getOperands().get(0);
            String guard = operand.getGuard() != null ? operand.getGuard().trim() : "";

            // o guard escreve o cabeçalho do loop: "for (...)" ou "while (...)" — transcrito tal e qual
            if (guard.startsWith("for") || guard.startsWith("while")) {
                writer.println(indentStr + guard.substring(0, guard.indexOf('(')).trim()
                        + " (" + GeneratorUtils.extractParen(guard) + ") {");
            } else {
                throw new IllegalArgumentException(
                        "Guard de loop tem de ser 'for (...)' ou 'while (...)': '" + guard + "'");
            }
            generateElements(writer, operand.getElements(), serviceLifeline, lifelineVarMap, indent + 1);
            writer.println(indentStr + "}");
        }

        else if ("opt".equals(operator)) {
            Operand operand = frag.getOperands().get(0);
            writer.println(indentStr + "if (" + operand.getGuard() + ") {");
            generateElements(writer, operand.getElements(), serviceLifeline, lifelineVarMap, indent + 1);
            writer.println(indentStr + "}");
        }

        else if ("alt".equals(operator)) {
            for(int i = 0; i < frag.getOperands().size(); i++) {
                Operand operand = frag.getOperands().get(i);
                String guard = operand.getGuard();
                if (i == 0) {
                    if (guard == null || guard.isBlank()) {
                        throw new IllegalArgumentException(
                                "A primeira ramificação de um 'alt' tem de ter uma condição.");
                    }
                    writer.println(indentStr + "if (" + guard + ") {");
                } else if (guard != null && !guard.isEmpty() && !guard.equalsIgnoreCase("else")) {
                    writer.println(indentStr + "} else if (" + guard + ") {");
                } else {
                    writer.println(indentStr + "} else {");
                }
                generateElements(writer, operand.getElements(), serviceLifeline, lifelineVarMap, indent + 1);
            }
            writer.println(indentStr + "}");
        }
    }

    // ownerType é (ou herda de) a classe desta ponta da associação?
    // serve para uma subclasse reconhecer associações declaradas no pai.
    // tenta match direto pelo nomem, senão, se ownerType for a classe em geração,
    // usa isOrInheritsFrom para apanhar o caso herdado.
    private boolean participates(String ownerType, String assocEndClass) {
        if (ownerType != null && ownerType.equals(assocEndClass)) return true;
        return currentClass != null && currentClass.getName().equals(ownerType)
                && currentClass.isOrInheritsFrom(assocEndClass);
    }

    // procura a associação cujo campo (no lado do ownerType) se chama attrName
    private Association findAssocByField(String ownerType, String attrName) {
        for (Association a : associations) {
            if (!participates(ownerType, a.getSourceClass()) && !participates(ownerType, a.getTargetClass())) continue;
            if (attrName.equals(fieldNameFor(a, ownerType))) return a;
        }
        return null;
    }

    // nome do campo, no lado do owner, que refere o outro lado da associação
    private String fieldNameFor(Association a, String ownerType) {
        if (participates(ownerType, a.getSourceClass())) {
            return GeneratorUtils.makeAttributeName(a.getTargetClass(), a.getTargetRole(), a.getTargetMultiplicity());
        }
        return GeneratorUtils.makeAttributeName(a.getSourceClass(), a.getSourceRole(), a.getSourceMultiplicity());
    }
}
