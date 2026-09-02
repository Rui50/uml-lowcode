package generator;

import model.Association;
import model.Class;
import model.Operation;
import model.Parameter;
import model.annotations.Postcondition;
import model.annotations.PreSnapshot;
import model.annotations.Precondition;
import model.sequence.*;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * ========
 * OperationGenerator
 * ==========
 *
 *
 * REGRAS PARA DESENHAR O DIAGRAMA
 *   - Um actor (lifeline sem tipo) chama a operação, essa 1ª mensagem é a "entry message".
 *   - O nome da entry message === nome da operação.
 *   - Todos os argumentos e o return tem os seus tipos explicitamente (a: Aluno; ... : boolean).
 *   - O nome de uma lifeline === o parâmetro/variável que representa (para chamadas cross-lifeline).
 *   - As lifelines do dono tem o tipo (turma: Turma).
 *   - Uma reply para o actor dá o valor de retorno; sem reply => void.
 *   - Mensagens «create» sem argumentos, pôr os valores com setters a seguir.
 *
 * passo a passo
 *   1. encontra a entry message (primeira mensagem, actor->dono)
 *   2. valida que o nome da entry === nome da operação
 *   3. ve o tipo do return (entry -> class diagram -> void)
 *   4. gera a assinatura a partir dos argumentos da entry (não do class diagram), cada arg tem de ter tipo
 *   5. preconds OCL -> if (!(expr)) throw IllegalStateException(msg).
 *   6. snaptchots @pre (para pós condições que referem x@pre) declarados antes do corpo.
 *   7. Corpo -> generateElements.
 *   8. expressao de retorno -> findReplyToActor (última reply para o actor), nonvoid sem reply -> erro.
 *   9. "result" (se uma pós-condição o referir) declarado depois do corpo.
 *   10. pos condições OCL -> mesma forma if (!(...)) throw.
 *   11. return result / return <expr>.
 *
 * generateElements
 *   Só emite uma Message se a sua source for o DONO (entry.getTarget()) e não for reply
 *
 * generateMessage — os cinco casos
 *   1. «create» -> Type var = new Type(args);
 *   2. declaração x : Tipo (sem valor) -> Tipo x;
 *   3. atribuição simples (isPlainAssignment, points = points - amount) -> [Type] x = expr;
 *   4. campo Map (associação qualificada): add/remove viram put/remove
 *      <campo>.add(x)    -> <campo>.put(x.getChave(), x)
 *      <campo>.remove(x) -> <campo>.remove(x.getChave())
 *   5. numa mensagem normal, ou seja cahmar um metodo, se for self cal mete o this, senao o nome da lifeline
 *
 * generateFragment
 *    Coleções Map levam .values() (adaptCollectionAccess).
 *
 * parte do ocl
 *   pre e pos condições -> if (!(javaExpr)) throw IllegalStateException(msg), @pre precisa de um snapshot
 *   antes do corpo, o "result" precisa de um alias depois do corpo (por isso um vai acima e o outro abaixo).
 *
 *  uma pós-condição que falha faz throw. no Spring/CLI-JPA (com BD) o throw dá rollback
 *   por causa da anot @transaction
 *
 *
 *   - findEntryMessage/firstMessage — primeira mensagem, descendo em fragments.
 *   - participates — ownerType é (ou herda de) a classe desta ponta da associação (subclasse reconhece
 *     associação do pai).
 *   - findAssoc/findAssocByAttribute/attributeNameFor — a que associação um add/remove se refere e o campo.
 *   - adaptCollectionAccess — acrescenta .values() ao iterar uma associação Map (qualificada).
 */
public class OperationGenerator {

    public static void generateOperations(PrintWriter writer, Class cls, List<Association> associations) {
        if (cls.getOperations().isEmpty()) {
            return;
        }

        writer.println("    // Operations");

        for (Operation op : cls.getOperations()) {
            // op abstrata só a assinatura abstract
            if (op.isAbstract()) {
                String absReturn = GeneratorUtils.removePackageNames(op.getReturnType());
                writer.print("    " + op.getVisibility() + " abstract " + absReturn + " " + op.getName() + "(");
                List<Parameter> absParams = op.getParameters();
                for (int i = 0; i < absParams.size(); i++) {
                    Parameter p = absParams.get(i);
                    if (i > 0) writer.print(", ");
                    writer.print(GeneratorUtils.removePackageNames(p.getType()) + " " + p.getName());
                }
                writer.println(");");
                continue;
            }
            // tem diagrama de sequência corpo real gerado a partir dele
            Interaction interaction = op.getSequenceDiagram();
            if (interaction != null) {
                generateOperation(writer, cls, op, interaction, associations);
            } else {
                // sem diagrama assinatura + nada dentro so que lança UnsupportedOperationException
                String visibility = op.getVisibility();
                String returnType = GeneratorUtils.removePackageNames(op.getReturnType());
                String name = op.getName();
                writer.println("    " + visibility + " " + returnType + " " + name + "(");

                List<Parameter> parameters = op.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    Parameter param = parameters.get(i);
                    String paramType = GeneratorUtils.removePackageNames(param.getType());
                    String paramName = param.getName();
                    writer.print("        " + paramType + " " + paramName);
                    writer.println(i < parameters.size() - 1 ? "," : "");
                }

                writer.println("    ) {");
                writer.println("        throw new UnsupportedOperationException(\"Not implemented yet\");");
                writer.println("    }");
            }
        }
    }

    // gera a operação, assinatura, precondições e corpo
    public static void generateOperation(PrintWriter writer, Class cls, Operation operation,
                                         Interaction interaction, List<Association> associations) {
        Message entry = findEntryMessage(interaction);

        if (entry == null) {
            throw new IllegalArgumentException("Operação '" + operation.getName()
                    + "': o diagrama não tem mensagem de entrada (actor -> " + cls.getName()
                    + ") com o nome da operação.");
        }

        // a mensagem de entrada tem de corresponder à operação declarada (pelo nome)
        if (entry.getName() != null && !entry.getName().equals(operation.getName())) {
            throw new IllegalArgumentException(
                    "Diagrama da operação '" + operation.getName() + "': a mensagem de entrada '"
                    + entry.getName() + "' não corresponde ao nome da operação.");
        }

        // tipo de retorno da mensagem de entrada se especificado | senão da operação (diagrama de classes) | senão void
        String returnType;
        if (entry.getReturnType() != null && !entry.getReturnType().isBlank()) {
            returnType = entry.getReturnType();
        } else if (operation.getReturnType() != null) {
            returnType = operation.getReturnType();
        } else {
            returnType = "void";
        }

        // assinatura a partir da mensagem de entrada o tipo de cada argumento é obrigatório
        writer.println();
        writer.print("    public " + returnType + " " + entry.getName() + "(");
        for (int i = 0; i < entry.getArguments().size(); i++) {
            if (i > 0) writer.print(", ");
            MessageArgument arg = entry.getArguments().get(i);
            if (arg.getType() == null || arg.getType().isBlank()) {
                throw new IllegalArgumentException(
                        "Diagrama da operação '" + operation.getName() + "': o argumento '"
                        + arg.getName() + "' não tem tipo (ex: '" + arg.getName() + " : Tipo').");
            }
            writer.print(GeneratorUtils.removePackageNames(arg.getType()) + " " + arg.getName());
        }
        writer.println(") {");

        for (Precondition pre : operation.getPreconditions()){
            writer.println("        if (!(" + pre.getJavaExpression() + ")) {");
            writer.println("            throw new IllegalStateException(\"" + GeneratorUtils.fixErrorMessage(pre.getMessage()) + "\");");
            writer.println("        }");
        }

        // @pre snapshots para capturar o estado antes de executar
        // dedup por varName: várias postconditions na mesma op podem mencionar o mesmo @pre
        Map<String, PreSnapshot> snapshots = new LinkedHashMap<>();
        for (Postcondition post : operation.getPostconditions()) {
            for (PreSnapshot snap : post.getPreSnapshots()) {
                snapshots.putIfAbsent(snap.getVarName(), snap);
            }
        }
        for (PreSnapshot snap : snapshots.values()) {
            writer.println("        " + snap.getJavaType() + " " + snap.getVarName()
                    + " = " + snap.getJavaInit() + ";");
        }

        generateElements(writer, interaction.getElements(), entry, cls, associations, 2);

        // a expressão de return vem da reply message para o actor | sem reply, não é feito return
        String returnExpr = GeneratorUtils.findReplyToActor(interaction.getElements());
        generateReturnAndPostconditions(writer, operation, returnType, returnExpr, "this");

        writer.println("    }");
    }

    // Fim de uma op com contedo, escreve o 'result' (se uma pós-condição o usar), as pós-condições OCL e o return.
    //
    // selfVar = como o OCL escreve o "self":
    //   - entity -> "this"  (o corpo corre na própria entidade)
    //   - Spring -> a var carregada do repo (e "member")
    //   getJavaExpression(selfVar) troca o alvo:  "this.getX()" -> "member.getX()"
    public static void generateReturnAndPostconditions(PrintWriter writer, Operation op, String returnType,
                                                       String returnExpr, String selfVar) {
        boolean needsResult = op.getPostconditions().stream()
                .anyMatch(p -> {
                    String je = p.getJavaExpression(selfVar);
                    return je != null && je.matches(".*\\bresult\\b.*");
                });

        if (!"void".equals(returnType) && returnExpr == null) {
            throw new IllegalArgumentException("Operação '" + op.getName() + "': devolve '" + returnType
                    + "' mas o diagrama não tem mensagem de resposta (reply) para o ator.");
        }
        if (needsResult && returnExpr == null) {
            throw new IllegalArgumentException("Operação '" + op.getName()
                    + "': uma pós-condição refere 'result' mas o diagrama não tem mensagem de resposta (reply).");
        }
        if (needsResult && "void".equals(returnType)) {
            throw new IllegalArgumentException("Operação '" + op.getName()
                    + "': uma pós-condição refere 'result' mas a operação é void (não tem valor de retorno).");
        }

        if (needsResult && returnExpr != null && !"void".equals(returnType)) {
            writer.println("        " + returnType + " result = " + returnExpr + ";");
        }

        for (Postcondition post : op.getPostconditions()) {
            writer.println("        if (!(" + post.getJavaExpression(selfVar) + ")) {");
            writer.println("            throw new IllegalStateException(\"" + GeneratorUtils.fixErrorMessage(post.getMessage()) + "\");");
            writer.println("        }");
        }

        if (!"void".equals(returnType) && returnExpr != null) {
            writer.println("        return " + (needsResult ? "result" : returnExpr) + ";");
        }
    }

    // Itera as messages e fragments, usa generateMessage / generateFragment
    // so emite mensages enviadas pelo dono do metodo o resto ignora, o resto seria outro diagrama
    private static void generateElements(PrintWriter writer, List<Object> elements,
                                         Message entry, Class cls, List<Association> associations, int indent) {
        String indentStr = "    ".repeat(indent);
        for (Object el : elements) {
            if (el instanceof Message) {
                Message msg = (Message) el;
                if (msg.getSource() == null || msg.getSource().getType() == null) continue;
                if (!msg.getSource().getId().equals(entry.getTarget().getId())) continue;
                if ("reply".equals(msg.getMessageSort())) continue; // replies só serverm para o return — tratadas em findReplyToActor
                generateMessage(writer, msg, entry, cls, associations, indentStr);
            } else if (el instanceof Fragment) {
                generateFragment(writer, (Fragment) el, entry, cls, associations, indent);
            }
        }
    }

    /**
     * Gera o código de uma mensagem
     * - target igual ao entry -> "this", senão o nome da lifeline (erro se não tiver nome)
     * - campo Map (<campo>.add/remove) -> put/remove pela chave do qualifier
     * - caso geral: receiver.name(args), com assign se tiver variável de atribuição
     */
    private static void generateMessage(PrintWriter writer, Message msg, Message entry,
                                        Class cls, List<Association> associations, String indent) {
        ////////// Create message

        // mensagem «create»: gera "Type var = new Type(args)".
        if ("createMessage".equals(msg.getMessageSort())) {
            String type = msg.getTarget().getType();
            String var = msg.getAssignVariable() != null ? msg.getAssignVariable()
                    : (msg.getTarget().getRepresents() != null && !msg.getTarget().getRepresents().isBlank()
                        ? msg.getTarget().getRepresents()
                        : GeneratorUtils.lowercaseFirst(type));
            StringBuilder a = new StringBuilder();
            if (msg.getArguments() != null) {
                for (int i = 0; i < msg.getArguments().size(); i++) {
                    if (i > 0) a.append(", ");
                    a.append(msg.getArguments().get(i).getName());
                }
            }
            writer.println(indent + type + " " + var + " = new " + type + "(" + a + ");");
            return;
        }

        ////////// Declarações ou atribuições de vars

        // declaração: "text : String" -> "String text;"
        if (msg.getAssignVariable() != null && msg.getAssignExpression() == null
                && msg.getName() == null
                && msg.getReturnType() != null && !msg.getReturnType().isEmpty()) {
            writer.println(indent + msg.getReturnType() + " " + msg.getAssignVariable() + ";");
            return;
        }

        // atribuição simples (quando nao tem chamadas a metodos): "x = expr" ou "x = expr : Type"
        // se tem ": Type"  declara (Type x = expr) senao -> reatribui (x = expr)
        if (msg.isPlainAssignment()) {
            String declType = (msg.getReturnType() != null && !msg.getReturnType().isEmpty())
                    ? msg.getReturnType() + " " : "";
            writer.println(indent + declType + msg.getAssignVariable() + " = " + msg.getAssignExpression() + ";");
            return;
        }

        ///////////// calls

        String receiver;
        if (msg.getTarget().getId().equals(entry.getTarget().getId())) {
            receiver = "this";
        } else {
            receiver = msg.getTarget().getName();
            if (receiver == null || receiver.isEmpty()) {
                throw new IllegalArgumentException(
                        "Diagrama de sequência: a lifeline do tipo '" + msg.getTarget().getType()
                        + "' não tem nome — o nome tem de ser igual ao parâmetro/variável que está a ser chamado.");
            }
        }

        String name = msg.getName();
        List<MessageArgument> args = msg.getArguments();

        // "alunos.add(a)" -> attrName="alunos", op="add"
        String attrName = null;
        String op = name;
        int sep = name.indexOf(".");
        if (sep > 0) {
            attrName = name.substring(0, sep);
            op = name.substring(sep + 1);
        }

        if (args != null && args.size() == 1 && ("add".equals(op) || "remove".equals(op))) {
            MessageArgument arg = args.get(0);
            String ownerType = msg.getTarget().getType();

            // se a mensagem for "alunos.add(a)", procura a associação com esse campo (alunos).
            // se for só "add(a)", sem campo à frente, trata como uma chamada normal (fallback).
            Association assoc = null;
            if (attrName != null) {
                assoc = findAssocByAttribute(associations, cls, ownerType, attrName);
            }

            if (assoc != null) {
                String base = receiver + "." + attrName;
                String code;
                if (assoc.hasQualifier()) {
                    // map com PUT(key, value) ou remove(key)
                    String q = assoc.getQualifierName();
                    String getter = "get" + Character.toUpperCase(q.charAt(0)) + q.substring(1) + "()";
                    code = "add".equals(op)
                            ? base + ".put(" + arg.getName() + "." + getter + ", " + arg.getName() + ");"
                            : base + ".remove(" + arg.getName() + "." + getter + ");";
                } else {
                    // coleção simples: ADD/REMOVE
                    code = base + "." + op + "(" + arg.getName() + ");";
                }
                writer.println(indent + code);
                return;
            }
        }

        // fallback: chamada literal receiver.name(args)
        StringBuilder call = new StringBuilder();
        call.append(receiver).append(".").append(name).append("(");
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) call.append(", ");
                call.append(args.get(i).getName());
            }
        }
        call.append(")");

        if (msg.getAssignVariable() != null) {
            // se ": Type" presente -> declara, se ausente -> reatribui a variável porque já tem de ser existente
            String declType = (msg.getReturnType() != null && !msg.getReturnType().isEmpty())
                    ? msg.getReturnType() + " " : "";
            writer.println(indent + declType + msg.getAssignVariable() + " = " + call + ";");
        } else {
            writer.println(indent + call + ";");
        }
    }

    /**
     * So dif no add/remove
     * um campo Map qualificado (<campo>.add/remove) vira put/remove, tudo o resto é literal.
     */
    private static void generateMessage2(PrintWriter writer, Message msg, Message entry,
                                        Class cls, List<Association> associations, String indent) {
        // mensagem «create»: gera "Type var = new Type(args)".
        if ("createMessage".equals(msg.getMessageSort())) {
            String type = msg.getTarget().getType();
            String var = msg.getAssignVariable() != null ? msg.getAssignVariable()
                    : (msg.getTarget().getRepresents() != null && !msg.getTarget().getRepresents().isBlank()
                        ? msg.getTarget().getRepresents()
                        : GeneratorUtils.lowercaseFirst(type));
            StringBuilder a = new StringBuilder();
            if (msg.getArguments() != null) {
                for (int i = 0; i < msg.getArguments().size(); i++) {
                    if (i > 0) a.append(", ");
                    a.append(msg.getArguments().get(i).getName());
                }
            }
            writer.println(indent + type + " " + var + " = new " + type + "(" + a + ");");
            return;
        }

        // atribuição simples: "x = expr" ou "x = expr : Type"
        if (msg.isPlainAssignment()) {
            String declType = (msg.getReturnType() != null && !msg.getReturnType().isEmpty())
                    ? msg.getReturnType() + " " : "";
            writer.println(indent + declType + msg.getAssignVariable() + " = " + msg.getAssignExpression() + ";");
            return;
        }

        String receiver;
        if (msg.getTarget().getId().equals(entry.getTarget().getId())) {
            receiver = "this";
        } else {
            receiver = msg.getTarget().getName();
            if (receiver == null || receiver.isEmpty()) {
                throw new IllegalArgumentException(
                        "Diagrama de sequência: a lifeline do tipo '" + msg.getTarget().getType()
                        + "' não tem nome — o nome tem de ser igual ao parâmetro/variável que está a ser chamado.");
            }
        }

        String name = msg.getName();
        List<MessageArgument> args = msg.getArguments();

        // "alunos.add(a)" -> attrName="alunos", op="add"
        String attrName = null;
        String op = name;
        int sep = name.indexOf(".");
        if (sep > 0) {
            attrName = name.substring(0, sep);
            op = name.substring(sep + 1);
        }

        // só o caso Map: <campo>.add(x)/.remove(x) sobre um campo map -> put/remove por chave.
        // listas normais e chamadas normais caem no fallback (transcrição literal).
        if (args != null && args.size() == 1 && attrName != null
                && ("add".equals(op) || "remove".equals(op))) {
            Association assoc = findAssocByAttribute(associations, cls, msg.getTarget().getType(), attrName);
            if (assoc != null && assoc.hasQualifier()) {
                MessageArgument arg = args.get(0);
                String q = assoc.getQualifierName();
                String getter = "get" + Character.toUpperCase(q.charAt(0)) + q.substring(1) + "()";
                String base = receiver + "." + attrName;
                writer.println(indent + ("add".equals(op)
                        ? base + ".put(" + arg.getName() + "." + getter + ", " + arg.getName() + ");"
                        : base + ".remove(" + arg.getName() + "." + getter + ");"));
                return;
            }
        }

        // fallback: chamada literal receiver.name(args)
        StringBuilder call = new StringBuilder();
        call.append(receiver).append(".").append(name).append("(");
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) call.append(", ");
                call.append(args.get(i).getName());
            }
        }
        call.append(")");

        if (msg.getAssignVariable() != null) {
            String declType = (msg.getReturnType() != null && !msg.getReturnType().isEmpty())
                    ? msg.getReturnType() + " " : "";
            writer.println(indent + declType + msg.getAssignVariable() + " = " + call + ";");
        } else {
            writer.println(indent + call + ";");
        }
    }

    /**
     * fragments para estruturas Java:
     *  - loop com guard "x: coll"  -> for (x : coll)
     *  - loop sem iterador         -> while (guard)
     *  - opt                       -> if (guard)
     *  - alt                       -> if / else if / else conforme os guards dos operandos
     */
    private static void generateFragment(PrintWriter writer, Fragment frag, Message entry,
                                         Class cls, List<Association> associations, int indent) {
        String indentStr = "    ".repeat(indent);
        String op = frag.getOperator();

        // fragmento sem mensagens do dono -> bloco vazio (for/if {})
        if ("loop".equals(op)) {
            Operand operand = frag.getOperands().get(0);
            String guard = operand.getGuard() != null ? operand.getGuard().trim() : "";

            // o guard escreve o loop: "for (Type var : toiter)" ou "while (cond)"
            if (guard.startsWith("for")) {
                String inner = GeneratorUtils.extractParen(guard);                 // "Aluno a : alunos"
                int colon = inner.indexOf(':');
                if (colon == -1) {
                    throw new IllegalArgumentException(
                            "Guard de loop 'for' tem de ser 'for (Tipo var : coleção)': '" + guard + "'");
                }
                String left = inner.substring(0, colon).trim();      // "Aluno a"
                String collection = inner.substring(colon + 1).trim().replace("self.", "this.");
                collection = adaptCollectionAccess(collection, cls, associations);   // Map -> .values()
                writer.println(indentStr + "for (" + left + " : " + collection + ") {");
                generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1);
                writer.println(indentStr + "}");
            } else if (guard.startsWith("while")) {
                writer.println(indentStr + "while (" + GeneratorUtils.extractParen(guard) + ") {");
                generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1);
                writer.println(indentStr + "}");
            } else {
                throw new IllegalArgumentException(
                        "Guard de loop tem de ser 'for (...)' ou 'while (...)': '" + guard + "'");
            }
        } else if ("opt".equals(op)) {
            Operand operand = frag.getOperands().get(0);
            String guard = operand.getGuard();
            if (guard == null || guard.isBlank()) {
                throw new IllegalArgumentException("Fragmento 'opt' tem de ter uma condição (guarda).");
            }
            writer.println(indentStr + "if (" + guard + ") {");
            generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1);
            writer.println(indentStr + "}");
        } else if ("alt".equals(op)) {
            List<Operand> ops = frag.getOperands();
            for (int i = 0; i < ops.size(); i++) {
                Operand operand = ops.get(i);
                String guard = operand.getGuard();
                if (i == 0) {
                    if (guard == null || guard.isBlank()) {
                        throw new IllegalArgumentException(
                                "Operação '" + cls.getName() + "': a primeira ramificação de um 'alt' tem de ter uma condição.");
                    }
                    writer.println(indentStr + "if (" + guard + ") {");
                } else if (guard != null && !guard.isEmpty() && !guard.equalsIgnoreCase("else")) {
                    writer.println(indentStr + "} else if (" + guard + ") {");
                } else {
                    writer.println(indentStr + "} else {");
                }
                generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1);
            }
            writer.println(indentStr + "}");
        }
    }

    // se a coleção é uma associação (Map), adiciona .values()
    // aceita "this.alunos" ou só "alunos"
    private static String adaptCollectionAccess(String expr, Class cls, List<Association> associations) {
        String attributeName = expr.startsWith("this.") ? expr.substring("this.".length()).trim() : expr.trim();

        for (Association assoc : associations) {
            // isOrInheritsFrom (não igual) porque a associação pode ser herdada de um pai
            boolean sourceSide = cls.isOrInheritsFrom(assoc.getSourceClass())
                    && assoc.isTargetNavigable()
                    && attributeName.equals(GeneratorUtils.makeAttributeName(assoc.getTargetClass(), assoc.getTargetRole(), assoc.getTargetMultiplicity()));
            boolean targetSide = cls.isOrInheritsFrom(assoc.getTargetClass())
                    && assoc.isSourceNavigable()
                    && attributeName.equals(GeneratorUtils.makeAttributeName(assoc.getSourceClass(), assoc.getSourceRole(), assoc.getSourceMultiplicity()));

            if ((sourceSide || targetSide) && assoc.hasQualifier()) {
                return expr + ".values()";
            }
        }
        return expr;
    }

    // ownerType é a classe desta ponta da associação?
    // "herda de" para uma subclasse reconhecer associações declaradas no pai
    private static boolean participates(Class cls, String ownerType, String assocEndClass) {
        if (ownerType != null && ownerType.equals(assocEndClass)) return true;
        return cls != null && cls.getName().equals(ownerType) && cls.isOrInheritsFrom(assocEndClass);
    }

    // procura a associação entre ownerType e otherType, navegável do lado do owner
    private static Association findAssoc(List<Association> assocs, Class cls, String ownerType, String otherType) {
        for (Association a : assocs){
            if (participates(cls, ownerType, a.getSourceClass()) && a.getTargetClass().equals(otherType) && a.isTargetNavigable())
                return a;
            if (participates(cls, ownerType, a.getTargetClass()) && a.getSourceClass().equals(otherType) && a.isSourceNavigable())
                return a;
        }
        return null;
    }

    private static String attributeNameFor(Association a, Class cls, String ownerType) {
        // lado a partir do owner, se owner é (ou herda) o source, o campo refere o target, e vice-versa
        if (participates(cls, ownerType, a.getSourceClass())) {
            return GeneratorUtils.makeAttributeName(a.getTargetClass(), a.getTargetRole(), a.getTargetMultiplicity());
        }
        return GeneratorUtils.makeAttributeName(a.getSourceClass(), a.getSourceRole(), a.getSourceMultiplicity());
    }

    // a mensagem do actor para lifeline, source pode ser null ou um Actor (type == null)
    public static Message findEntryMessage(Interaction interaction) {
        if (interaction == null) return null;
        return firstMessage(interaction.getElements());
    }

    private static Message firstMessage(List<Object> elements) {
        for (Object el : elements) {
            if (el instanceof Message m) return m;
            if (el instanceof Fragment f) {
                for (Operand op : f.getOperands()) {
                    Message inner = firstMessage(op.getElements());
                    if (inner != null) return inner;
                }
            }
        }
        return null;
    }

    // procura associação cujo field name no lado do owner é attrName
    private static Association findAssocByAttribute(List<Association> assocs, Class cls, String ownerType, String attrName) {
        for (Association a : assocs) {
            if (!participates(cls, ownerType, a.getSourceClass()) && !participates(cls, ownerType, a.getTargetClass())) continue;
            if (attrName.equals(attributeNameFor(a, cls, ownerType))) return a;
        }
        return null;
    }

}
