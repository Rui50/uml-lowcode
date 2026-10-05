package ocl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduz uma expressão OCL para java
 * utilizando o parser gerado pelo ANTLR4 a partir da gramática OCL.g4
 *
 * existe um metodo visit para cada regra da gramática, e a tradução é feita recursivamente
 *
 * assume sempre sempre listas ou sets na tradução, se for maps o ValidationGenerator da fix a isso com o fixmapaccess
 *
 * OCL NÃO SUPORTADO lançam IllegalArgumentException ("Operação OCL não suportada")
 *
 * no fim de visitPostfixTranslate e o erro chega à frontend pelo AnnotationService.
 *
 *  - tipos: oclIsKindOf, oclIsTypeOf, oclAsType, oclType, oclAsSet,
 *           oclIsUndefined, oclIsInvalid, oclIsNew
 *  - coleções: union, intersection, symmetricDifference, excludesAll, prepend,
 *              append, one, closure, sortedBy, asBag, asOrderedSet, reverse, at, indexOf
 *
 * (não dá para suportar no validate() de uma entidade):
 *  - allInstances()  — todas as instâncias
 *  - @pre / oclIsNew / oclIsInvalid — pós-condição (estado antes/depois)
 *
 * problemas (traduz, mas com problemas):
 *  - sum() assume lista de Integer (não double/float)
 *  - só fica == com um número literal de um lado (como "x = 0")  senão faz Objects.equals
 */
public class OCLToJavaVisitor extends OCLBaseVisitor<String> {

    // mapa de 'let': nome -> tradução Java do valor. usado para substituir vez que o nome é usado.
    private final Map<String, String> letScope = new HashMap<>();


    public static String toJava(String oclExpression) {
        if (oclExpression == null || oclExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("Expressão OCL vazia.");
        }

        OCLLexer lexer = new OCLLexer(CharStreams.fromString(oclExpression));
        OCLParser parser = new OCLParser(new CommonTokenStream(lexer));

        // por defeito  o ANTLR, ao ter um erro de sintaxe imprime o erro e continua com uma árvore invalida que
        // depois rebenta no visitor com um NullPointerException  (HTTP 500).

        // para evitar isso, substituímos o listener de erro por um que lança exceção
        // logo no primeiro erro de sintaxe, com uma mensagem clara e antes de a árvore ser visitada
        //https://www.antlr.org/api/Java/org/antlr/v4/runtime/BaseErrorListener.html
        org.antlr.v4.runtime.BaseErrorListener throwing = new org.antlr.v4.runtime.BaseErrorListener() {
            @Override
            public void syntaxError(org.antlr.v4.runtime.Recognizer<?, ?> r, Object sym,
                                    int line, int pos, String msg,
                                    org.antlr.v4.runtime.RecognitionException e) {
                // versão com detalhe do ANTLR
                throw new IllegalArgumentException(
                        "Expressão OCL inválida: \"" + oclExpression + "\" (" + msg + ")");

                // versão mais simples, sem detalhes técnicos
                // throw new IllegalArgumentException("Expressão OCL inválida: \"" + oclExpression + "\"");
            }
        };
        lexer.removeErrorListeners();   // remove o listener por defeito (imprime + recupera)
        lexer.addErrorListener(throwing);
        parser.removeErrorListeners();
        parser.addErrorListener(throwing);

        return new OCLToJavaVisitor().visit(parser.mainOclExpression());
    }

    // mainOclExpression adicionado ao .g4 original, para ignorar a parte do "context X inv Y:"
    // context nao vai ser necessário porque ja vai estar associado a classe
    // mainOclExpression : expression EOF
    @Override
    public String visitMainOclExpression(OCLParser.MainOclExpressionContext ctx) {
        return visit(ctx.expression());
    }

    /*
        expression : logicalExpression | conditionalExpression | letExpression
    */
    // Uma expressão pode ser uma de tres coisas, verificamos qual é e visitamos
    @Override
    public String visitExpression(OCLParser.ExpressionContext ctx) {
        if (ctx.logicalExpression() != null) return visit(ctx.logicalExpression());
        if (ctx.conditionalExpression() != null) return visit(ctx.conditionalExpression());
        if (ctx.letExpression() != null) return visit(ctx.letExpression());
        return ctx.getText();
    }

    /*
        letExpression : 'let' letBinding (',' letBinding)* 'in' expression
    */
    // let basicamente dá nome a uma xpressão, so substitumos o nome (let x) pela expressão, sem vars
    //  let x = self.preco, y = self.stock in x > y   ->   (this.preco) > (this.stock)
    @Override
    public String visitLetExpression(OCLParser.LetExpressionContext ctx) {
        // regista cada binding no letScope e limpa-os quando o let acaba
        List<String> names = new ArrayList<>();
        for (OCLParser.LetBindingContext b : ctx.letBinding()) {
            String name  = b.ID().getText(); // id do binding = x
            String value = "(" + visit(b.expression()) + ")"; // expressão do binding = (this.preco)
            letScope.put(name, value);
            names.add(name);
        }

        String body = visit(ctx.expression());   // o corpo (depois do 'in') ja ve os bindings

        for (String n : names) letScope.remove(n);   // limpar no fim do let
        return body;
    }

    /*
     basicExpression
        : NULL_LITERAL              -> null
        | BOOLEAN_LITERAL           -> true / false
        | basicExpression '.' ID    -> acesso a propriedade (self.alunos)
        | basicExpression '(' expressionList? ')'  -> chamada de metodo (self.getName())
        | basicExpression '[' expression ']'       -> acesso por índice (self.alunos[0])
        | INT                       -> número inteiro (30)
        | FLOAT_LITERAL             -> número decimal (1.5)
        | STRING1_LITERAL           -> string com aspas duplas ("hello")
        | STRING2_LITERAL           -> string com aspas simples ('hello')
        | ENUMERATION_LITERAL       -> enumeração (Color::Red)
        | ID                        -> id simples (alunos, x)
        | '(' expression ')'        -> expressão entre parênteses
    */

    // ctx.XXXX() da o content se esta alternativa foi a que deu match, ou null se não foi.
    // se a expressão é "30", ctx.INT() retorna "30", e ctx.ID() retorna null.
    @Override
    public String visitBasicExpression(OCLParser.BasicExpressionContext ctx) {

        // self -> this
        if (ctx.getText().equals("self")) {
            return "this";
        }

        // Literais simples — igual em Java, devolvemos o texto como está
        if (ctx.NULL_LITERAL() != null) {
            return "null";
        }
        if (ctx.BOOLEAN_LITERAL() != null) {
            return ctx.BOOLEAN_LITERAL().getText();
        }
        if (ctx.INT() != null) {
            return ctx.INT().getText();
        }
        if (ctx.FLOAT_LITERAL() != null) {
            return ctx.FLOAT_LITERAL().getText();
        }

        // string com aspas duplas
        if (ctx.STRING1_LITERAL() != null) {
            return ctx.STRING1_LITERAL().getText();
        }
        // string com aspas simples OCL usa '', Java usa ""
        if (ctx.STRING2_LITERAL() != null) {
            String s = ctx.STRING2_LITERAL().getText();
            return "\"" + s.substring(1, s.length() - 1) + "\"";
        }

        // enum literal EstadoAluno::ATIVO -> EstadoAluno.ATIVO
        if (ctx.ENUMERATION_LITERAL() != null) {
            return ctx.ENUMERATION_LITERAL().getText().replace("::", ".");
        }

        //  ( expressão )
        if (ctx.expression() != null && ctx.getChild(0).getText().equals("(")) {
            return "(" + visit(ctx.expression()) + ")";
        }

        // basicExpression '(' expressionList? ')'

        // chamada de metodo com argumentos do tipo self.owner.matches("x")
        // a gramatica mete o nome do metodo (matches) no no da esqueda, e ctx.ID() null
        if (ctx.ID() == null && ctx.basicExpression() != null && ctx.getChildCount() > 1 && ctx.getChild(1).getText().equals("(")) {
            // 'left' é tudo menos os (args) em self.owner.matches('x') é self.owner.matches
            OCLParser.BasicExpressionContext left = ctx.basicExpression();

            // left tem de ter as duas partes basicExpression é o objeto que se ta a mexer, left.ID() é o metodo
            if (left.ID() != null && left.basicExpression() != null) {
                String recv = visit(left.basicExpression());   // self.owner -> this.getOwner()
                // pode ou nao ter argumentos
                String args = ctx.expressionList() != null ? visit(ctx.expressionList()) : "";
                // o nome do metodo
                String op   = left.ID().getText(); // "matches"

                // metodo da propria classe passa normal
                // se for um metodo de Java tem de ser traduzido para o equivalente
                String name;
                if ("this".equals(recv)) {
                    name = op;
                } else {
                    name = javaValueOp(op);
                }
                return recv + "." + name + "(" + args + ")";
            }
        }

        // basicExpression '.' ID  ->  acesso a propriedade em OCL tipo self.alunos
        // ctx.basicExpression() = o lado esquerdo (self) e  ctx.ID() = o nome (alunos)
        if (ctx.ID() != null && ctx.basicExpression() != null) {
            String base = visit(ctx.basicExpression()); // traduz o lado esquerdo

            // propriedade -> getter (this.getX()), não o campo
            // (campo herdado é private e getter herda-se e cobre também os derivados)
            String prop = ctx.ID().getText();
            String getter = "get" + prop.substring(0, 1).toUpperCase() + prop.substring(1);
            return base + "." + getter + "()";
        }

        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            if (letScope.containsKey(name)) {
                return letScope.get(name);   // é variável de let -> devolve a tradução,
            } else {
                return name;                 // não é -> devolve o próprio nome
            }
        }

        return ctx.getText();
    }

    /*
        expressionList : (expression ',')* expression
    */
    // lista de expressões separadas por vírgulas ( em argumentos )
    // vamos a cada expressão e juntamos com ", "
    @Override
    public String visitExpressionList(OCLParser.ExpressionListContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.expression().size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(visit(ctx.expression(i)));
        }
        return sb.toString();
    }

    /*
        logicalExpression
            : equalityExpression (('and' | '&' | 'or' | 'xor' | '=>' | 'implies') equalityExpression)* ;
     */
    // self.x > 0 and self.y > 0  ->  this.x > 0 && this.y > 0
    @Override
    public String visitLogicalExpression(OCLParser.LogicalExpressionContext ctx) {
        // se so tem uma equalityExpression, visita  diretamente (tipo self.x > 0)
        if (ctx.equalityExpression().size() == 1) {
            return visit(ctx.equalityExpression(0));
        }

        // se tem varias, junta com o operador correto
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.equalityExpression(0)));

        for (int i = 1; i < ctx.equalityExpression().size(); i++) {
            // O operador está entre as equalityExpressions
            //  0=expr, 1=op, 2=expr, 3=op, 4=expr ...
            // ent impar = operador, par = equalityExpression
            String op = ctx.getChild(2 * i - 1).getText();
            String right = visit(ctx.equalityExpression(i));
            switch (op) {
                case "and":
                case "&":
                    sb.append(" && ").append(right);
                    break;
                case "or":
                    sb.append(" || ").append(right);
                    break;
                case "xor":
                    // a xor b -> a ^ b (true quando diferem)
                    sb.append(" ^ ").append(right);
                    break;
                case "implies":
                case "=>":          // '=>' é só alternativa de 'implies'
                    // a implies b -> !(a) || (b)
                    String left = sb.toString();
                    sb.setLength(0);
                    sb.append("!(").append(left).append(") || (").append(right).append(")");
                    break;
                default:
                    sb.append(" ").append(op).append(" ").append(right);
                    break;
            }
        }
        return sb.toString();
    }

    /*
        equalityExpression : additiveExpression (('=' | '<' | '>' | '>=' | '<=' | '/=' | '<>' | ':' | '/:' | '<:') additiveExpression)*
    */
    // self.capacity > 100  ->  this.getCapacity() > 100
    // self.nome <> ''  ->  !java.util.Objects.equals(this.getNome(), "")

    @Override
    public String visitEqualityExpression(OCLParser.EqualityExpressionContext ctx) {
        if (ctx.additiveExpression().size() == 1) {
            return visit(ctx.additiveExpression(0));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.additiveExpression(0)));

        for (int i = 1; i < ctx.additiveExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            String right = visit(ctx.additiveExpression(i));

            // = e <> passam para Objects.equals (seguro para null, strings e enums),
            // exceto quando é um número (0, 2.5) usa ==/!=
            // senão o Objects.equals comparava Double com Integer e dava sempre false.
            if (op.equals("=") || op.equals("<>") || op.equals("/=")) {
                String left = sb.toString();
                boolean negate = !op.equals("=");
                sb.setLength(0);
                if (isNumber(left) || isNumber(right)) {
                    sb.append(left).append(negate ? " != " : " == ").append(right);
                } else {
                    if (negate) sb.append("!");
                    sb.append("Objects.equals(").append(left).append(", ").append(right).append(")");
                }
                continue;
            }

            switch (op) {
                case "<":  sb.append(" < "); break;
                case ">":  sb.append(" > "); break;
                case "<=": sb.append(" <= "); break;
                case ">=": sb.append(" >= "); break;
                default:   sb.append(" " + op + " "); break;
            }
            sb.append(right);
        }
        return sb.toString();
    }

    // fora da lista, lança já em vez de gerar Java que só rebenta ao compilar
    private static String javaValueOp(String oclOp) {
        switch (oclOp) {
            case "matches":     return "matches";
            case "concat":      return "concat";
            case "toUpperCase": return "toUpperCase";
            case "toLowerCase": return "toLowerCase";
            default:
                throw new IllegalArgumentException("Operação OCL não suportada: " + oclOp + "()");
        }
    }

    // true se a string é só um número (0, -3, 2.5, 10L) -> a comparação usa ==/!=
    private static boolean isNumber(String s) {
        return s != null && s.trim().matches("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?[fFdDlL]?");
    }

    /*
        additiveExpression : multiplicativeExpression (('+' | '-' | '..' | '|->') multiplicativeExpression)*
     */
    @Override
    public String visitAdditiveExpression(OCLParser.AdditiveExpressionContext ctx) {
        if (ctx.multiplicativeExpression().size() == 1) {
            return visit(ctx.multiplicativeExpression(0));
        }

        StringBuilder sb = new StringBuilder();

        sb.append(visit(ctx.multiplicativeExpression(0)));

        // a cada volta acrescenta "operador + expressão seguinte"
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            // expr, op, expr ,op
            String op = ctx.getChild(2 * i - 1).getText();

            // aqui é os + - entao adiciona direto
            sb.append(" " + op + " ");
            sb.append(visit(ctx.multiplicativeExpression(i)));
        }
        return sb.toString();
    }

    /*
        multiplicativeExpression : unaryExpression (('*' | '/' | 'mod' | 'div') unaryExpression)*
     */
    // como self.price * 1.2 -> this.getPrice() * 1.2
    @Override
    public String visitMultiplicativeExpression(OCLParser.MultiplicativeExpressionContext ctx) {
        if (ctx.unaryExpression().size() == 1) {
            return visit(ctx.unaryExpression(0));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.unaryExpression(0)));

        for (int i = 1; i < ctx.unaryExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            if ("mod".equals(op)) op = "%";
            if ("div".equals(op)) op = "/";
            sb.append(" " + op + " ");
            sb.append(visit(ctx.unaryExpression(i)));
        }
        return sb.toString();
    }

    /*
        unaryExpression : ('not' | '-' | '+' | '?' | '!') unaryExpression | navigationExpression
    */
    // como not self.active  ->  !this.getActive()
    @Override
    public String visitUnaryExpression(OCLParser.UnaryExpressionContext ctx) {
        if (ctx.navigationExpression() != null) {
            return visit(ctx.navigationExpression());
        }
        // 'not' -> '!'
        String op = ctx.getChild(0).getText();
        if ("not".equals(op)) op = "!";
        return op + visit(ctx.unaryExpression());
    }

    /*
        navigationExpression : primaryFactor (postfixSuffix)*
     */
    // constroi expressao da esquerda para a direita, aplicando cada sufixo sobre o resultado anterior
    // self.alunos->size()  o primaryFactor(self.alunos) + postfixSuffix(->size())
    @Override
    public String visitNavigationExpression(OCLParser.NavigationExpressionContext ctx) {
        String result = visit(ctx.primaryFactor());

        // aplica cada sufixo (->size(), ->select(...), ->isEmpty(), etc.) por ordem
        for (OCLParser.PostfixSuffixContext suffix : ctx.postfixSuffix()) {
            result = visitPostfixTranslate(result, suffix);
        }
        return result;
    }


    /*
        primaryFactor : setExpression | basicExpression
     */
    @Override
    public String visitPrimaryFactor(OCLParser.PrimaryFactorContext ctx) {
        if (ctx.basicExpression() != null) return visit(ctx.basicExpression());
        if (ctx.setExpression() != null) return visit(ctx.setExpression());
        return ctx.getText();
    }


    // metodo auxiliar para processar sufixos com o lado esquerdo já traduzido para Java
    // base="this.alunos", suffix="->size()" -> "this.alunos.size()"
    private String visitPostfixTranslate(String base, OCLParser.PostfixSuffixContext ctx) {
        String text = ctx.getText();

        // ->size() -> .size()
        // talvez (int) ...count() em vez de .size() (evita a lista quando vem de um ->select)
        if (text.equals("->size()")) return base + ".size()";
        if (text.equals("->isEmpty()")) return base + ".isEmpty()";
        if (text.equals("->notEmpty()")) return "!" + base + ".isEmpty()";

        // .size() -> .size()
        if (text.equals(".size()")) return base + ".size()";

        // ->includes(x) -> .contains(x)
        if (text.startsWith("->includes(")) {
            return base + ".contains(" + visit(ctx.expression(0)) + ")";
        }
        // ->excludes(x) -> !.contains(x)
        if (text.startsWith("->excludes(")) {
            return "!" + base + ".contains(" + visit(ctx.expression(0)) + ")";
        }

        // ->select(a | a.idade > 18) -> .stream().filter(a -> a.idade > 18).collect(Collectors.toList())
        if (text.startsWith("->select(")) {
            String var = ctx.identOptType().ID().getText();
            String condition = visit(ctx.expression(0));
            return base + ".stream().filter(" + var + " -> " + condition + ").collect(java.util.stream.Collectors.toList())";
        }

        // ->reject(a | a.idade < 18) -> .stream().filter(a -> !(a.idade < 18)).collect(Collectors.toList())
        if (text.startsWith("->reject(")) {
            String var = ctx.identOptType().ID().getText();
            String condition = visit(ctx.expression(0));
            return base + ".stream().filter(" + var + " -> !(" + condition + ")).collect(java.util.stream.Collectors.toList())";
        }

        // ->forAll(a | a.turma <> null) -> .stream().allMatch(a -> a.turma != null)
        if (text.startsWith("->forAll(")) {
            String var = ctx.identOptTypeList().identOptType(0).ID().getText();
            String condition = visit(ctx.expression(0));
            return base + ".stream().allMatch(" + var + " -> " + condition + ")";
        }

        // ->exists(a | a.idade > 18) -> .stream().anyMatch(a -> a.idade > 18)
        if (text.startsWith("->exists(")) {
            String var = ctx.identOptTypeList().identOptType(0).ID().getText();
            String condition = visit(ctx.expression(0));
            return base + ".stream().anyMatch(" + var + " -> " + condition + ")";
        }

        // ->collect(a | a.nome) -> .stream().map(a -> a.nome).collect(Collectors.toList())
        if (text.startsWith("->collect(")) {
            String var = ctx.identOptType().ID().getText();
            String body = visit(ctx.expression(0));
            return base + ".stream().map(" + var + " -> " + body + ").collect(java.util.stream.Collectors.toList())";
        }

        // ->includesAll(x) -> .containsAll(x)
        if (text.startsWith("->includesAll(")) {
            return base + ".containsAll(" + visit(ctx.expression(0)) + ")";
        }

        // ->sum() assume coleção de Integer; Double/Float não dá (o visitor não sabe o tipo aqui)
        // se quisermos suportar precisamos de pegar em informação do tipo aqui (o que para ja nao fazemos simplesmente)
        if (text.equals("->sum()")) return base + ".stream().mapToInt(Integer::intValue).sum()";

        // ->max() -> .stream().max(...).orElse(null)
        if (text.equals("->max()"))
            return base + ".stream().max(java.util.Comparator.naturalOrder()).orElse(null)";
        if (text.equals("->min()"))
            return base + ".stream().min(java.util.Comparator.naturalOrder()).orElse(null)";

        // ->count(x) -> nº de ocorrências de x na coleção
        if (text.startsWith("->count("))
            return "java.util.Collections.frequency(" + base + ", " + visit(ctx.expression(0)) + ")";

        // operações numéricas  (base é um número)
        if (text.equals("->abs()"))   return "Math.abs(" + base + ")";
        if (text.equals("->floor()")) return "(long) Math.floor(" + base + ")";
        if (text.equals("->round()")) return "Math.round(" + base + ")";

        // operações de string (assume base do tipo String)
        if (text.equals("->toUpperCase()")) return base + ".toUpperCase()";
        if (text.equals("->toLowerCase()")) return base + ".toLowerCase()";
        if (text.equals("->toInteger()"))   return "Integer.parseInt(" + base + ")";
        if (text.equals("->toReal()"))      return "Double.parseDouble(" + base + ")";
        if (text.equals("->toBoolean()"))   return "Boolean.parseBoolean(" + base + ")";
        if (text.equals("->characters()"))  return "java.util.Arrays.asList(" + base + ".split(\"\"))";
        if (text.startsWith("->equalsIgnoreCase("))
            return base + ".equalsIgnoreCase(" + visit(ctx.expression(0)) + ")";

        // ->first() -> .get(0)
        //if (text.equals("->first()")) return base + ".get(0)";

        // tem de ser stream para dar em Set

        // ->first() -> .stream().findFirst().orElse(null)
        // ->first().nome -> .stream().findFirst().orElse(null).getNome()
        if (text.startsWith("->first()")) {
            String result = base + ".stream().findFirst().orElse(null)";
            // há um ".propriedade" a seguir ao first()?
            if (!ctx.ID().isEmpty()) {
                String prop = ctx.ID(0).getText(); // o nome da propriedade, "nome"
                String getter = "get" + prop.substring(0, 1).toUpperCase() + prop.substring(1);
                result = result + "." + getter + "()";
            }
            return result;
        }

        // ->last() -> .get(<base>.size() - 1)
        //if (text.equals("->last()")) return base + ".get(" + base + ".size() - 1)";


        // ->last() -> .stream().reduce((a, b) -> b).orElse(null)
        if (text.startsWith("->last()")) {
            String result = base + ".stream().reduce((__a, __b) -> __b).orElse(null)";
            if (!ctx.ID().isEmpty()) {
                String prop = ctx.ID(0).getText();
                String getter = "get" + prop.substring(0, 1).toUpperCase() + prop.substring(1);
                result = result + "." + getter + "()";
            }
            return result;
        }

        // ->any() -> .stream().findAny().orElse(null)
        if (text.startsWith("->any()")) {
            String result = base + ".stream().findAny().orElse(null)";
            if (!ctx.ID().isEmpty()) {
                String prop = ctx.ID(0).getText();
                String getter = "get" + prop.substring(0, 1).toUpperCase() + prop.substring(1);
                result = result + "." + getter + "()";
            }
            return result;
        }

        // ->asSet() -> new HashSet<>(base)
        if (text.equals("->asSet()")) return "new HashSet<>(" + base + ")";

        // ->asSequence() -> new ArrayList<>(base)
        if (text.equals("->asSequence()")) return "new ArrayList<>(" + base + ")";

        // ->including(x) -> Stream.concat(base.stream(), Stream.of(x)).collect(toList())
        if (text.startsWith("->including(")) {
            String item = visit(ctx.expression(0));
            return "java.util.stream.Stream.concat(" + base + ".stream(), java.util.stream.Stream.of("
                    + item + ")).collect(java.util.stream.Collectors.toList())";
        }

        // ->excluding(x) -> base.stream().filter(v -> !java.util.Objects.equals(v,x)).collect(toList())
        if (text.startsWith("->excluding(")) {
            String item = visit(ctx.expression(0));
            return base + ".stream().filter(elemToCheck -> !Objects.equals(elemToCheck, " + item +
                    ")).collect(java.util.stream.Collectors.toList())";
        }

        // operação aceite pela gramática mas não suportada na tradução — falhar já,
        // em vez de gerar Java inválido que só rebenta ao compilar o projeto gerado
        throw new IllegalArgumentException("Operação OCL não suportada: " + text);
    }

    /*
        conditionalExpression : 'if' expression 'then' expression 'else' expression 'endif'
     */
    // -> ((cond) ? (then) : (else))
    @Override
    public String visitConditionalExpression(OCLParser.ConditionalExpressionContext ctx) {
        String condition = visit(ctx.expression(0));
        String thenPart = visit(ctx.expression(1));
        String elsePart = visit(ctx.expression(2));
        return "((" + condition + ") ? (" + thenPart + ") : (" + elsePart + "))";
    }

    /*
        setExpression : 'Set{' expressionList? '}' | 'Sequence{' ... | etc
     */
    // vazio  -> new HashSet<>()  (ou ArrayList/LinkedHashSet dependendo do tipo)
    // se tem itens -> new HashSet<>(java.util.Arrays.asList(itens))
    @Override
    public String visitSetExpression(OCLParser.SetExpressionContext ctx) {
        String first = ctx.getChild(0).getText();
        String javaCollection;

        if (first.equals("Map{")) {
            if (ctx.expressionList() == null) return "new HashMap<>()";
            // IllegalArgumentException para ser apanhada pelo pipeline de erros (AnnotationService)
            throw new IllegalArgumentException("Literais Map com entradas não são suportados");
        }

        if (first.equals("Set{")) javaCollection = "HashSet";
        else if (first.equals("Sequence{")) javaCollection = "ArrayList";
        else if (first.equals("Bag{")) javaCollection = "ArrayList";
        else if (first.equals("OrderedSet{")) javaCollection = "LinkedHashSet";
        else javaCollection = "ArrayList";

        if (ctx.expressionList() == null) {
            return "new " + javaCollection + "<>()";
        }

        String items = visit(ctx.expressionList());
        return "new " + javaCollection + "<>(java.util.Arrays.asList(" + items + "))";
    }
}
