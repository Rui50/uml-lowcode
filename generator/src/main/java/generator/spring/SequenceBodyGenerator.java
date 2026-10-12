package generator.spring;

import generator.GeneratorUtils;
import generator.OperationGenerator;
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


/*
 * SequenceBodyGenerato
 *
 * REGRAS DO DIAGRAMA
 *   - lifeline "<Classe>Service" (o dono) obrigatória, sem ela -> erro
 *   - lifelines "<Classe>Repository" -> os campos de repo injetados
 *   - o self é carregado antes do corpo (por um param "<var>Id"), registado em preDeclaredVars
 *   - findById(id) : Tipo -> Tipo var = repo.findById(id).orElseThrow(NotFoundException);
 *   - tipos explícitos nos args/return.
 *
 * generateMessageCode — só as mensagens enviadas pelo service
 *   - create
 *   - declaração (x : Tipo)
 *   - atribuição simples (x = expr)
 *   - <campo>.add/remove/get numa entidade -> getter (put/remove por chave se Map)
 *   - self-message
 *   - reply -> return (+ checkInvariants antes)
 *   - break / continue
 *   - chamada com retorno (findById -> orElseThrow);
 *   - chamada sem retorno
 *
 * generateFragmentCode — loop (for/while transcrito tal e qual, sem adaptar Map->.values(),
 *   ao contrário do OperationGenerator), opt, alt.
 */
public class SequenceBodyGenerator {

    private final List<Association> associations;
    // variáveis já declaradas fora do corpo ( pelo carregamento da entidade self), não redeclarar
    private final Set<String> preDeclaredVars = new HashSet<>();
    // classe do metodo em geração (owner do diagrama)
    private Class currentClass;

    // return
    private Operation currentOp;
    private String returnType;
    private String selfVar;

    public SequenceBodyGenerator(List<Association> associations) {
        this.associations = associations;
    }

    // regista uma variável como já declarada ( self carregado antes do corpo)
    public void addPreDeclaredVar(String varName) {
        preDeclaredVars.add(varName);
    }

    public void generateMethodBodyFromInteraction(PrintWriter writer, Class cls, Operation op, Interaction interaction,
                                                  String returnType, String selfVar) {
        // identificar a lifeline do Service (owner deste metodo)
        this.currentClass = cls;
        this.currentOp = op;
        this.returnType = returnType;
        this.selfVar = selfVar;
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
        // TurmaRepository -> turmaRepository, AlunoRepository -> alunoRepository
        Map<String, String> lifelineVarMap = new HashMap<>();
        for (Lifeline ll : interaction.getLifelines()) {
            if (ll.getType() != null && ll.getType().endsWith("Repository")) {
                String repoVar = GeneratorUtils.lowercaseFirst(ll.getType());
                lifelineVarMap.put(ll.getId(), repoVar);
            }
            // if (ll.getType() != null && ll.getType().endsWith("Service")) {
            //     lifelineVarMap.put(ll.getId(), "self");
            // }
            if (ll.getType() != null && ll.getType().endsWith("Service") && !ll.getId().equals(serviceLifeline.getId())) {
                lifelineVarMap.put(ll.getId(), GeneratorUtils.lowercaseFirst(ll.getType()));   // outro service -> campo injetado
            }
        }

        // percorrer a árvore de elements (Messages e Fragments)
        generateElements(writer, interaction.getElements(), serviceLifeline, lifelineVarMap, 2);
    }

    // percorre a lista de elementos (mensagens ou fragmentos) e gera o código correspondente
    private void generateElements(PrintWriter writer, List<Object> elements,
                                  Lifeline serviceLifeline, Map<String, String> lifelineVarMap, int indent) {
        for (Object elem : elements) {
            if (elem instanceof Message) {
                generateMessageCode(writer, (Message) elem, serviceLifeline, lifelineVarMap, indent);
            } else if (elem instanceof Fragment) {
                generateFragmentCode(writer, (Fragment) elem, serviceLifeline, lifelineVarMap, indent);
            }
        }
    }

    // ignora mensagens que nao fazem parte do service
    private void generateMessageCode(PrintWriter writer, Message msg, Lifeline serviceLifeline,
                                      Map<String, String> lifelineVarMap, int indent) {
        String ind = "    ".repeat(indent);
        if (msg.getSource() == null || msg.getTarget() == null) return;

        // só processar mensagens enviadas pelo service
        if (serviceLifeline == null || !msg.getSource().getId().equals(serviceLifeline.getId())) return;

        // break/continue sao mensagens reservadas (self sempre e se nao tiverem argumentos ou seja n sao metodos)
        String n = msg.getName();
        if (("break".equals(n) || "continue".equals(n)) && msg.getArguments().isEmpty()) {
            writer.println(ind + n + ";");
            return;
        }

        // reply = return,
        if ("reply".equals(msg.getMessageSort())) {
            String expr = msg.getRawName() != null ? msg.getRawName() : msg.getName();
            if (expr != null && !expr.isBlank()) {
                // o self só está declarado se foi carregado  sem ele não há variável onde chamar o checkInvariants
                if (currentClass.hasAnyInvariants() && preDeclaredVars.contains(selfVar)) {
                    writer.println(ind + selfVar + ".checkInvariants();");
                }
                OperationGenerator.writeReturn(writer, currentOp, returnType, expr.trim(), selfVar, indent);
            }
            return;
        }

        // mensagem «create»: gera "Type var = new Type(args)".
        if ("createMessage".equals(msg.getMessageSort())) {
            String type = msg.getTarget().getType();
            // nome da variável: o "x =" do diagrama, senão o nome da lifeline, senão o tipo em minuscula
            String var;
            String represents = msg.getTarget().getRepresents();
            if (msg.getAssignVariable() != null) {
                var = msg.getAssignVariable();
            } else if (represents != null && !represents.isBlank()) {
                var = represents;
            } else {
                var = GeneratorUtils.lowercaseFirst(type);
            }
            StringBuilder a = new StringBuilder();
            for (int i = 0; i < msg.getArguments().size(); i++) {
                if (i > 0) a.append(", ");
                a.append(msg.getArguments().get(i).getName());
            }
            writer.println(ind + type + " " + var + " = new " + type + "(" + a + ");");
            return;
        }

        // declaração: "text : String" -> "String text;" (assignVariable + returnType, sem valor).
        // name==null distingue de "x = foo() : Type", que é chamada
        if (msg.getAssignVariable() != null && msg.getAssignExpression() == null
                && msg.getName() == null
                && msg.getReturnType() != null && !msg.getReturnType().isEmpty()) {
            writer.println(ind + msg.getReturnType() + " " + msg.getAssignVariable() + ";");
            return;
        }

        // atribuição simples (sem chamada): "x = expr" ou "x = expr : Type"
        // ": Type" presente -> declara; ausente -> reatribui
        if (msg.isPlainAssignment()) {
            // com ": Tipo" declara a variável, sem ele é só reatribuir
            String declType = "";
            if (msg.getReturnType() != null && !msg.getReturnType().isEmpty()) {
                declType = msg.getReturnType() + " ";
            }
            writer.println(ind + declType + msg.getAssignVariable() + " = " + msg.getAssignExpression() + ";");
            return;
        }

        // var da lifeline alvo: repos/service vêm do map e entidades carregadas usam o nome
        // (represents) para distinguir duas da mesma classe (turma vs destino), senão o tipo
        String targetVar = lifelineVarMap.get(msg.getTarget().getId());
        if (targetVar == null) {
            String represents = msg.getTarget().getRepresents();
            if (represents != null && !represents.isBlank()) {
                targetVar = represents;
            } else {
                targetVar = GeneratorUtils.lowercaseFirst(msg.getTarget().getType());
            }
        }

        // construir lista dos argumentos
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < msg.getArguments().size(); i++) {
            if (i > 0) args.append(", ");
            args.append(msg.getArguments().get(i).getName());
        }

        String methodName = msg.getName();
        boolean isSelfMessage = msg.getSource().getId().equals(msg.getTarget().getId());

        // para as coleções: <campo>.add/remove/get numa entidade carregada acesso com getter
        if (methodName != null && msg.getArguments().size() == 1) {
            int sep = methodName.indexOf('.');
            if (sep > 0) {
                String attrName = methodName.substring(0, sep);
                String op = methodName.substring(sep + 1);
                Association assoc = findAssocByAttribute(msg.getTarget().getType(), attrName);
                if (assoc != null) {
                    String argName = msg.getArguments().get(0).getName();
                    String base = targetVar + ".get" + GeneratorUtils.capitalizeFirst(attrName) + "()";

                    // get(chave)  leitura, é uma atribuição (Tipo var = ...)
                    if ("get".equals(op) && msg.getAssignVariable() != null) {
                        String type = (msg.getReturnType() != null && !msg.getReturnType().isEmpty())
                                ? msg.getReturnType() + " " : "";
                        writer.println(ind + type + msg.getAssignVariable() + " = " + base + ".get(" + argName + ");");
                        return;
                    }

                    // add/remove(x) ->  (List add/remove ou Map put/remove com a chave)
                    if ("add".equals(op) || "remove".equals(op)) {
                        String code;
                        if (assoc.hasQualifier()) {
                            // Map: a chave vem do qualifier, add -> put(chave, x) e remove -> remove(chave)
                            String keyGetter = "get" + GeneratorUtils.capitalizeFirst(assoc.getQualifierName()) + "()";
                            if ("add".equals(op)) {
                                code = base + ".put(" + argName + "." + keyGetter + ", " + argName + ");";
                            } else {
                                code = base + ".remove(" + argName + "." + keyGetter + ");";
                            }
                        } else {
                            code = base + "." + op + "(" + argName + ");";
                        }
                        writer.println(ind + code);
                        return;
                    }
                }
            }
        }

        // receiver: a self-message chama direto (contar()), as outras vão pela var da lifeline alvo
        String receiver = "";
        if (!isSelfMessage) {
            receiver = targetVar + ".";
        }
        // a chamada já escrita, com receiver e argumentos: "turmaRepository.findById(id)"
        String call = receiver + methodName + "(" + args + ")";

        // tem ": Tipo" no diagrama ent o resultado é para guardar numa variável
        if (msg.getReturnType() != null && !msg.getReturnType().isEmpty()) {
            String returnType = msg.getReturnType();
            // nome da variável: o "x =" do diagrama, se não escreveu a var usa o tipo em minuscula
            String varName;
            if (msg.getAssignVariable() != null) {
                varName = msg.getAssignVariable();
            } else {
                varName = GeneratorUtils.lowercaseFirst(returnType);
            }

            // já declarada antes (o self carregado para as condições) -> não declarar outra vez
             if (preDeclaredVars.contains(varName)) return;

            // findById do repositório devolve Optional -> precisa do orElseThrow (self-message nunca é repo)
            if (!isSelfMessage && methodName.equals("findById")) {
                writer.println(ind + returnType + " " + varName + " = " + call);
                writer.println(ind + "    .orElseThrow(() -> new NotFoundException(\"" + returnType + " not found\"));");
            } else {
                writer.println(ind + returnType + " " + varName + " = " + call + ";");
            }
        } else {
            // sem tipo de retorno -> só a chamada: save(turma)
            writer.println(ind + call + ";");
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

            // o guard  do loop: "for (...)" ou "while (...)"
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
            String guard = operand.getGuard();
            if (guard == null || guard.isBlank()) {
                throw new IllegalArgumentException("Fragmento 'opt' tem de ter uma condição (guarda).");
            }
            writer.println(indentStr + "if (" + guard + ") {");
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
        else {
            throw new IllegalArgumentException("Fragmento '" + operator + "' não é suportado.");
        }
    }

    // ownerType está numa ponta desta associação diretamente, ou por herança de um pai
    private boolean participates(String ownerType, String assocEndClass) {
        if (ownerType != null && ownerType.equals(assocEndClass)) return true;
        return currentClass != null && currentClass.getName().equals(ownerType)
                && currentClass.isOrInheritsFrom(assocEndClass);
    }

    // procura a associação cujo campo (no lado do ownerType) se chama attrName
    private Association findAssocByAttribute(String ownerType, String attrName) {
        for (Association a : associations) {
            if (!participates(ownerType, a.getSourceClass()) && !participates(ownerType, a.getTargetClass())) continue;
            if (attrName.equals(attributeNameFor(a, ownerType))) return a;
        }
        return null;
    }

    // nome do campo, no lado do owner, que refere o outro lado da associação
    private String attributeNameFor(Association a, String ownerType) {
        if (participates(ownerType, a.getSourceClass())) {
            return GeneratorUtils.makeAttributeName(a.getTargetClass(), a.getTargetRole(), a.getTargetMultiplicity());
        }
        return GeneratorUtils.makeAttributeName(a.getSourceClass(), a.getSourceRole(), a.getSourceMultiplicity());
    }
}
