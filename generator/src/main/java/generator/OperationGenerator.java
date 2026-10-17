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
 * passo a passo
 *   1. encontra a entry message (primeira mensagem, actor->dono)
 *   2. valida que o nome da entry === nome da operação
 *   3. ve o tipo do return
 *   4. gera a assinatura a partir dos argumentos da entry
 *   5. gera as pré conds
 *   6. snaptchots @pre declarados antes do corpo.
 *   7. expressao de retorno -> findReplyToActor (última reply para o actor), nonvoid sem reply -> erro
 *   8. gera o corpo, no reply vai o return (result + post conds)
 *   9. se for void, pos conds no fim.
 *
 * generateElements
 *   Só emite uma Message se a sua source for o DONO (entry.getTarget()) e não for reply
 *
 * generateMessage — por tipo de mensagem:
 *   - «create»                  -> Type var = new Type(args)
 *   - "x : Tipo" (sem valor)    -> Tipo x;
 *   - atribuição simples        -> [Type] x = expr
 *   - add/remove numa coleção   -> getX().add/remove(x)  (Map: put/remove pela chave)
 *   - break / continue          -> break; / continue;
 *   - save / delete (só com BD) -> Db.save/delete(x)
 *   - geral (chamada)           -> receiver.metodo(args)  (receiver = this se for o dono, senão o nome da lifeline)
 */
public class OperationGenerator {

    // o Db só é gerado no cli-jpa, nos outros alvos não existe
    private static boolean hasDb;

    public static void generateOperations(PrintWriter writer, Class cls, List<Association> associations, boolean hasDb) {
        OperationGenerator.hasDb = hasDb;
        if (cls.getOperations().isEmpty()) {
            return;
        }

        writer.println("    // Operations");

        for (Operation op : cls.getOperations()) {
            // op abstrata só a assinatura abstract
            if (op.isAbstract()) {
                writer.print("    " + op.getVisibility() + " abstract " + op.getReturnType() + " " + op.getName() + "(");
                List<Parameter> absParams = op.getParameters();
                for (int i = 0; i < absParams.size(); i++) {
                    Parameter p = absParams.get(i);
                    if (i > 0) writer.print(", ");
                    writer.print(p.getType() + " " + p.getName());
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
                String returnType = op.getReturnType();
                String name = op.getName();
                writer.println("    " + visibility + " " + returnType + " " + name + "(");

                List<Parameter> parameters = op.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    Parameter param = parameters.get(i);
                    String paramType = param.getType();
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
            writer.print(arg.getType() + " " + arg.getName());
        }
        writer.println(") {");

        // as pre conds
        for (Precondition pre : operation.getPreconditions()){
            writer.println("        if (!(" + pre.getJavaExpression() + ")) {");
            writer.println("            throw new IllegalStateException(\"" + GeneratorUtils.fixErrorMessage(pre.getMessage()) + "\");");
            writer.println("        }");
        }

        // @pre snapshots para capturar o estado antes de executar
        // dedup por varName so para evitar varias postconditions na mesma op com o mesmo @pre
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

        String returnExpr = GeneratorUtils.findReplyToActor(interaction.getElements());
        validateReturn(operation, returnType, returnExpr, "this");
        generateElements(writer, interaction.getElements(), entry, cls, associations, 2, operation, returnType, "this");
        // op void com pos condições ent não tem return e tem de por as pos condições no fim
        if ("void".equals(returnType)) {
            writePostconditions(writer, operation, "this", 2);
        }

        writer.println("    }");
    }


    // Fim de uma op com contedo, escreve o 'result' (se uma pos cond o usar), as pos conds OCL e o return
    public static void generateReturnAndPostconditions(PrintWriter writer, Operation op, String returnType,
                                                       String returnExpr, String selfVar) {
        validateReturn(op, returnType, returnExpr, selfVar);
        if (!"void".equals(returnType) && returnExpr != null) {
            writeReturn(writer, op, returnType, returnExpr, selfVar, 2);
        } else {
            writePostconditions(writer, op, selfVar, 2);
        }
    }

    // validaçao relacionada ao diagrama tar bem constuido
    // TODO op void com reply no diagrama emite "return x;" dar returna  erro
    public static void validateReturn(Operation op, String returnType, String returnExpr, String selfVar) {
        boolean needsResult = op.getPostconditions().stream().anyMatch(p -> {
            String code = p.getJavaExpression(selfVar);
            return code != null && code.matches(".*\\bresult\\b.*");
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
    }

    // pós-condições OCL
    private static void writePostconditions(PrintWriter writer, Operation op, String selfVar, int indent) {
        String ind = "    ".repeat(indent);
        for (Postcondition post : op.getPostconditions()) {
            writer.println(ind + "if (!(" + post.getJavaExpression(selfVar) + ")) {");
            writer.println(ind + "    throw new IllegalStateException(\"" + GeneratorUtils.fixErrorMessage(post.getMessage()) + "\");");
            writer.println(ind + "}");
        }
    }

    // return message
    //   1. se alguma pós-condição usar 'result', escreve <returnType> result = <expr>;
    //   2. as verificações das pós-condições OCL
    //   3. o return
    public static void writeReturn(PrintWriter writer, Operation op, String returnType,
                                   String expr, String selfVar, int indent) {
        String ind = "    ".repeat(indent);
        // se tem uma postcondição que precisa de result
        boolean needsResult = op.getPostconditions().stream().anyMatch(p -> {
            String code = p.getJavaExpression(selfVar);
            return code != null && code.matches(".*\\bresult\\b.*");
        });
        if (needsResult) {
            writer.println(ind + returnType + " result = " + expr + ";");
        }
        writePostconditions(writer, op, selfVar, indent);
        writer.println(ind + "return " + (needsResult ? "result" : expr) + ";");
    }

    // Itera as messages e fragments, usa generateMessage / generateFragment
    // so emite mensages enviadas pelo dono do metodo o resto ignora, o resto seria outro diagrama
    private static void generateElements(PrintWriter writer, List<Object> elements,
                                         Message entry, Class cls, List<Association> associations, int indent,
                                         Operation op, String returnType, String selfVar) {
        String indentStr = "    ".repeat(indent);
        for (Object el : elements) {
            if (el instanceof Message) {
                Message msg = (Message) el;
                // skip actor msg ou null
                if (msg.getSource() == null || msg.getSource().getType() == null) continue;
                // skips meng que nao sao do owner
                if (!msg.getSource().getId().equals(entry.getTarget().getId())) continue;
                if ("reply".equals(msg.getMessageSort())) {
                    // reply = return
                    String expr = msg.getRawName() != null ? msg.getRawName() : msg.getName();
                    if (expr != null && !expr.isBlank()) {
                        writeReturn(writer, op, returnType, expr.trim(), selfVar, indent);
                    }
                    continue;
                }
                generateMessage(writer, msg, entry, cls, associations, indentStr);
            } else if (el instanceof Fragment) {
                generateFragment(writer, (Fragment) el, entry, cls, associations, indent, op, returnType, selfVar);
            }
        }
    }

    /**
     * Gera o código de uma mensagem
     * receiver = "this" se o target for o dono, senão o nome da lifeline (erro se não tiver nome).
     */
    private static void generateMessage(PrintWriter writer, Message msg, Message entry,
                                        Class cls, List<Association> associations, String indent) {
        // break/continue sao mensagens reservadas (self sempre e se nao tiverem argumentos ou seja n sao metodos)
        String n = msg.getName();
        if (("break".equals(n) || "continue".equals(n)) && msg.getArguments().isEmpty()) {
            writer.println(indent + n + ";");
            return;
        }

        // save/delete sao mensagens reservadas
        //  do owner para a entidade a gravar, so faz sentido nos alvos com BD
        // sem BD sai como chamada normal (this.save(x)), que é o que foi desenhado
        if (hasDb && ("save".equals(n) || "delete".equals(n)) && msg.getArguments() != null && msg.getArguments().size() == 1) {
            String arg = msg.getArguments().get(0).getName().replace("self.", "this.");
            writer.println(indent + "Db." + n + "(" + arg + ");");
            return;
        }

        ////////// Create message

        // mensagem «create»: gera "Type var = new Type(args)".
        if ("createMessage".equals(msg.getMessageSort())) {
            String type = msg.getTarget().getType();
            // nome da var: o que a lifeline representa (o objeto criado é a lifeline), senão a var de atribuição
            String var;
            if (msg.getTarget().getRepresents() != null && !msg.getTarget().getRepresents().isBlank()) {
                var = msg.getTarget().getRepresents();
            } else {
                var = msg.getAssignVariable();
            }
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
            if (msg.getReturnType() != null && !msg.getReturnType().isEmpty()) {
                writer.println(indent + msg.getReturnType() + " " + msg.getAssignVariable() + " = " + msg.getAssignExpression() + ";");
            } else {
                writer.println(indent + msg.getAssignVariable() + " = " + msg.getAssignExpression() + ";");
            }
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
                // pelo getter: o campo é private e num pai ou noutra classe não se lhe chega diretamente
                String base = receiver + ".get" + GeneratorUtils.capitalizeFirst(attrName) + "()";
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
            if (msg.getReturnType() != null && !msg.getReturnType().isEmpty()) {
                writer.println(indent + msg.getReturnType() + " " + msg.getAssignVariable() + " = " + call + ";");
            } else {
                writer.println(indent + msg.getAssignVariable() + " = " + call + ";");
            }
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
                                         Class cls, List<Association> associations, int indent,
                                         Operation operation, String returnType, String selfVar) {
        String indentStr = "    ".repeat(indent);
        String op = frag.getOperator();

        // fragmento sem mensagens do dono -> bloco vazio (for/if {})
        if ("loop".equals(op)) {
            Operand operand = frag.getOperands().get(0);
            String guard = operand.getGuard() != null ? operand.getGuard().trim() : "";

            // o guard escreve o loop: "for (Type var : colecao)" ou "while (cond)"
            if (guard.startsWith("for")) {
                String inner = GeneratorUtils.extractParen(guard);                 // "Aluno a : alunos" ou "int i = 0; i < n; i++"
                if (inner.contains(";")) {
                    // for normal , transcreve tal e qual, so muda  self. -> this.
                    writer.println(indentStr + "for (" + inner.replace("self.", "this.") + ") {");
                } else {
                    // iterar uma colecao
                    int colon = inner.indexOf(':');
                    if (colon == -1) {
                        throw new IllegalArgumentException(
                                "Guard de loop 'for' tem de ser 'for (Tipo var : coleção)' ou 'for (i = 0; i < 1; i++)': '" + guard + "'");
                    }
                    String left = inner.substring(0, colon).trim();      // "Aluno a"
                    String collection = inner.substring(colon + 1).trim().replace("self.", "this.");
                    collection = adaptCollectionAccess(collection, cls, associations);   // Map -> .values()
                    writer.println(indentStr + "for (" + left + " : " + collection + ") {");
                }
                generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1, operation, returnType, selfVar);
                writer.println(indentStr + "}");
            } else if (guard.startsWith("while")) {
                writer.println(indentStr + "while (" + GeneratorUtils.extractParen(guard) + ") {");
                generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1, operation, returnType, selfVar);
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
            generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1, operation, returnType, selfVar);
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
                generateElements(writer, operand.getElements(), entry, cls, associations, indent + 1, operation, returnType, selfVar);
            }
            writer.println(indentStr + "}");
        } else {
            throw new IllegalArgumentException("Fragmento '" + op + "' não é suportado.");
        }
    }

    // se a coleção é uma associação (Map), adiciona .values()
    // aceita "this.alunos" ou só "alunos"
    private static String adaptCollectionAccess(String expr, Class cls, List<Association> associations) {
        // o guard escreve "self.<campo>" e vai pelo getter,
        // A associação só decide se precisa de .values() (ponta qualificada = Map).

        // variável local
        if (!expr.startsWith("this.")) return expr;
        // "this.tarefas" -> "tarefas"
        String attributeName = expr.substring("this.".length()).trim();
        // navegação, fica como está
        if (attributeName.contains(".")) return expr;
        // campo é private
        String getter = "this.get" + GeneratorUtils.capitalizeFirst(attributeName) + "()";

        for (Association assoc : associations) {
            // isOrInheritsFrom (não igual) porque a associação pode ser herdada de um pai
            boolean sourceSide = cls.isOrInheritsFrom(assoc.getSourceClass())
                    && assoc.isTargetNavigable()
                    && attributeName.equals(GeneratorUtils.makeAttributeName(assoc.getTargetClass(), assoc.getTargetRole(), assoc.getTargetMultiplicity()));
            boolean targetSide = cls.isOrInheritsFrom(assoc.getTargetClass())
                    && assoc.isSourceNavigable()
                    && attributeName.equals(GeneratorUtils.makeAttributeName(assoc.getSourceClass(), assoc.getSourceRole(), assoc.getSourceMultiplicity()));

            // Map
            if ((sourceSide || targetSide) && assoc.hasQualifier()) return getter + ".values()";
        }
        return getter;
    }

    // ownerType é a classe desta ponta da associação
    // "herda de" para uma subclasse reconhecer associações declaradas no pai
    private static boolean participates(Class cls, String ownerType, String assocEndClass) {

        if (ownerType != null && ownerType.equals(assocEndClass)) return true;
        return cls != null && cls.getName().equals(ownerType) && cls.isOrInheritsFrom(assocEndClass);
    }

    // nome do campo que owner (a classe que tem o campo) usa para esta associação
    // para ligar um "campo.add/remove" à sua associação
    private static String attributeNameFor(Association a, Class cls, String ownerType) {
        // se ownerType é (ou herda) o source da assoc, o campo aponta o target -> nome do target
        if (participates(cls, ownerType, a.getSourceClass())) {
            return GeneratorUtils.makeAttributeName(a.getTargetClass(), a.getTargetRole(), a.getTargetMultiplicity());
        }
        // senão -> do source
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
