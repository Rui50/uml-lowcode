package ocl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * Traduz uma expressão OCL para java
 * utilizando o parser gerado pelo ANTLR4 a partir da gramática OCL.g4
 *
 * existe um metodo visit para cada regra da gramática, e a tradução é feita recursivamente
 *
 * assume sempre sempre listas ou sets na tradução, se for maps o ValidationGenerator da fix a isso
 *
 * OCL NÃO SUPORTADO
 * Estas caem no fallback "return base + text" (fim de visitPostfixTranslate) e geram
 * Java que NÃO compila, sem mensagem de erro. Melhoria futura: falhar de forma explícita.
 *  - tipos: oclIsKindOf, oclIsTypeOf, oclAsType, oclType, oclAsSet,
 *           oclIsUndefined, oclIsInvalid, oclIsNew
 *  - coleções: union, intersection, symmetricDifference, excludesAll, prepend,
 *              append, one, closure, sortedBy, asBag, asOrderedSet, reverse, at, indexOf
 *
 * FORA DE ÂMBITO (não dá para suportar no validate() de uma entidade):
 *  - allInstances()  — precisa de um repositório (todas as instâncias)
 *  - @pre / oclIsNew / oclIsInvalid — semântica de pós-condição (estado antes/depois)
 *  - closure         — fecho transitivo (precisa de ponto-fixo)
 *
 * LIMITAÇÕES (traduz, mas com cuidado):
 *  - sum() assume coleção de Integer (não double/float)
 *  - '=' numérico entre wrappers diferentes dá SEMPRE false (Objects.equals: Long vs Integer,
 *    Double vs Integer...). Afeta floor()/round() e ex. "double = int". Usar >=, <=, >, <.
 */
public class OCLToJavaVisitor extends OCLBaseVisitor<String> {

    // bindings de 'let' ativas: nome -> expressão Java já traduzida. Substituição inline
    // (let é puro em OCL): cada uso de x emite a tradução do seu valor. Ver visitBasicExpression.
    private final java.util.Map<String, String> letScope = new java.util.HashMap<>();

    public static String toJava(String oclExpression) {
        if (oclExpression == null || oclExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("Expressão OCL vazia.");
        }

        OCLLexer lexer = new OCLLexer(CharStreams.fromString(oclExpression));
        OCLParser parser = new OCLParser(new CommonTokenStream(lexer));

        // por defeito, num erro de sintaxe o ANTLR apenas imprime o erro e tenta
        // "recuperar", continua a construir uma árvore incompleta/inválida. Essa árvore
        // depois rebenta no visitor com um NullPointerException  (HTTP 500).

        //  para evitar isso, substituímos os listeners por defeito por um que lança exceção
        // logo no primeiro erro de sintaxe, com uma mensagem clara e antes de a árvore ser visitada
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

        return new OCLToJavaVisitor().visit(parser.standaloneExpression());
    }

    // standalone adicionado ao .g4 original, para ignorar a parte do "context X inv Y:"
    // context nao vai ser necessário porque ja vai estar associado a classe
    // standaloneExpression : expression EOF
    @Override
    public String visitStandaloneExpression(OCLParser.StandaloneExpressionContext ctx) {
        return visit(ctx.expression());
    }

    // expression : logicalExpression | conditionalExpression | letExpression
    // Uma expressão pode ser uma de tres coisas, verificamos qual é e visitamos
    @Override
    public String visitExpression(OCLParser.ExpressionContext ctx) {
        if (ctx.logicalExpression() != null) return visit(ctx.logicalExpression());
        if (ctx.conditionalExpression() != null) return visit(ctx.conditionalExpression());
        if (ctx.letExpression() != null) return visit(ctx.letExpression());
        return ctx.getText();
    }

    // Regra: letExpression : 'let' letBinding (',' letBinding)* 'in' expression
    // 'let' nomeia sub-expressões para reuso/legibilidade. Não há variável local em Java
    // (traduzimos para UMA expressão), por isso fazemos substituição inline: cada uso do
    // nome no corpo emite a tradução do seu valor (ver visitBasicExpression).
    //
    // ex:  let x = self.preco, y = self.stock in x > y   ->   (this.preco) > (this.stock)
    //      preenche letScope { x -> "(this.preco)", y -> "(this.stock)" } e depois traduz "x > y"
    @Override
    public String visitLetExpression(OCLParser.LetExpressionContext ctx) {
        // repor o scope no fim: nomes novos saem, nomes que já existiam voltam ao valor antigo
        java.util.List<String> added = new java.util.ArrayList<>();
        java.util.Map<String, String> replaced = new java.util.HashMap<>();

        for (OCLParser.LetBindingContext b : ctx.letBinding()) {
            String name  = b.ID().getText();                  // ex: "x"
            String value = "(" + visit(b.expression()) + ")"; // ex: "(this.preco)"  (bindings anteriores já ativas)
            if (letScope.containsKey(name)) replaced.put(name, letScope.get(name)); // já existia -> guardar o antigo
            else added.add(name);                                                   // novo -> marcar p/ remover
            letScope.put(name, value);
        }

        String body = visit(ctx.expression());        // a expressão depois do 'in'

        // repor o scope ao estado anterior a este let
        for (String n : added) letScope.remove(n);    // os novos -> apagar
        letScope.putAll(replaced);                    // os tapados -> repor o valor antigo
        return body;
    }

    // Regra:
    // basicExpression
    //    : NULL_LITERAL              -> null
    //    | BOOLEAN_LITERAL           -> true / false
    //    | basicExpression '.' ID    -> acesso a propriedade (self.alunos)
    //    | basicExpression '(' expressionList? ')'  -> chamada de método (self.getName())
    //    | basicExpression '[' expression ']'       -> acesso por índice (self.alunos[0])
    //    | INT                       -> número inteiro (30)
    //    | FLOAT_LITERAL             -> número decimal (1.5)
    //    | STRING1_LITERAL           -> string com aspas duplas ("hello")
    //    | STRING2_LITERAL           -> string com aspas simples ('hello')
    //    | ENUMERATION_LITERAL       -> enumeração (Color::Red)
    //    | ID                        -> identificador simples (alunos, x)
    //    | '(' expression ')'        -> expressão entre parênteses
    //
    // ctx.XXXX() retorna o token se esta alternativa foi a que deu match, ou null se não foi.
    // se a expressão é "30", ctx.INT() retorna o token "30", e ctx.ID() retorna null.
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
        // string com aspas simples — OCL usa '', Java usa ""
        if (ctx.STRING2_LITERAL() != null) {
            String s = ctx.STRING2_LITERAL().getText();
            return "\"" + s.substring(1, s.length() - 1) + "\"";
        }

        // enum literal: EstadoAluno::ATIVO -> EstadoAluno.ATIVO
        if (ctx.ENUMERATION_LITERAL() != null) {
            return ctx.ENUMERATION_LITERAL().getText().replace("::", ".");
        }

        //  ( expressão )
        if (ctx.expression() != null && ctx.getChild(0).getText().equals("(")) {
            return "(" + visit(ctx.expression()) + ")";
        }

        // accesso a propriedade ou chamada de metod
        // self.alunos, self.getName(), self.doSomething(x, y), self.alunos[0]
        // ctx.basicExpression() é o lado esquerdo (self")
        // ctx.ID() é o nome da propriedade/metodo ("alunos", "getName")
        if (ctx.ID() != null && ctx.basicExpression() != null) {
            String base = visit(ctx.basicExpression()); // lado esquerdo recursivamente

            // chamada de metodos com argumentos como self.getAge("arg)
            if (ctx.expressionList() != null) {
                return base + "." + ctx.ID().getText() + "(" + visit(ctx.expressionList()) + ")";
            }
            // chamada de metodo sem argumentos como self.getId()
            // Verificamos se o último filho é ")"
            if (ctx.getChildCount() > 2 && ctx.getChild(ctx.getChildCount() - 1).getText().equals(")")) {
                return base + "." + ctx.ID().getText() + "()";
            }
            // acesso por índice: self.alunos[0] -> this.alunos.get(0)
            if (ctx.expression() != null) {
                return base + ".get(" + visit(ctx.expression()) + ")";
            }

            // accesso a propriedade
            // se base é "this" (self) -> acesso direto: this.alunos
            // se base é outro objeto -> usar getter: a.getNumero()
            String prop = ctx.ID().getText();
            if ("this".equals(base)) {
                return base + "." + prop;
            } else {
                String getter = "get" + prop.substring(0, 1).toUpperCase() + prop.substring(1);
                return base + "." + getter + "()";
            }
        }

        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            if (letScope.containsKey(name)) {
                return letScope.get(name);   // é variável de let -> devolve a tradução, ex "(this.preco)"
            } else {
                return name;                 // não é -> devolve o próprio nome, ex "a"
            }
        }

        return ctx.getText();
    }

    // regra: expressionList : (expression ',')* expression
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

    // regra: logicalExpression : equalityExpression (('and' | 'or' | 'implies' ...) equalityExpression)*
    // : self.x > 0 and self.y > 0  ->  this.x > 0 && this.y > 0
    @Override
    public String visitLogicalExpression(OCLParser.LogicalExpressionContext ctx) {
        // se so tem uma equalityExpression, visita  diretamente
        if (ctx.equalityExpression().size() == 1) {
            return visit(ctx.equalityExpression(0));
        }

        // se tem varias, junta com o operador correto
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.equalityExpression(0)));

        for (int i = 1; i < ctx.equalityExpression().size(); i++) {
            // O operador está entre as equalityExpressions
            // pos : 0=expr, 1=op, 2=expr, 3=op, 4=expr...
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
                case "=>":          // '=>' é só notação alternativa de 'implies'
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

    // Regra: equalityExpression : additiveExpression (('=' | '<>' | '<' | '>' | '<=' | '>=')additiveExpression)*
    // como self.capacity > 100 -> this.capacity > 100
    // self.nome <> ''     ->  !this.nome.equals("")

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

            // = and <> usam Objects.equals para nao haver problemas com null ou strings etc
            if (op.equals("=")) {
                String left = sb.toString();
                sb.setLength(0);
                sb.append("java.util.Objects.equals(").append(left).append(", ").append(right).append(")");
                continue;
            }
            if (op.equals("<>") || op.equals("/=")) {
                String left = sb.toString();
                sb.setLength(0);
                sb.append("!java.util.Objects.equals(").append(left).append(", ").append(right).append(")");
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

    // Regra: additiveExpression : multiplicativeExpression (('+' | '-') multiplicativeExpression)*
    @Override
    public String visitAdditiveExpression(OCLParser.AdditiveExpressionContext ctx) {
        if (ctx.multiplicativeExpression().size() == 1) {
            return visit(ctx.multiplicativeExpression(0));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.multiplicativeExpression(0)));

        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            sb.append(" " + op + " ");
            sb.append(visit(ctx.multiplicativeExpression(i)));
        }
        return sb.toString();
    }

    // Regra: multiplicativeExpression : unaryExpression (('*' | '/' | 'mod' | 'div') unaryExpression)*
    // como self.price * 1.2 -> this.price * 1.2
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

    // Regra: unaryExpression : ('not' | '-' | '+') unaryExpression | navigationExpression
    // como not self.active -> !this.active
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

    // Regra: navigationExpression : primaryFactor (postfixSuffix)*
    // constroi expressao da esquerda para a direita, aplicando cada sufixo sobre o resultado anterior
    // self.alunos->size() é o primaryFactor(self.alunos) + postfixSuffix(->size())
    @Override
    public String visitNavigationExpression(OCLParser.NavigationExpressionContext ctx) {
        String result = visit(ctx.primaryFactor());

        // aplica cada sufixo (->size(), .nome, etc.) por ordem
        for (OCLParser.PostfixSuffixContext suffix : ctx.postfixSuffix()) {
            result = visitPostfixTranslate(result, suffix);
        }
        return result;
    }


    // Regra: primaryFactor : setExpression | basicExpression
    @Override
    public String visitPrimaryFactor(OCLParser.PrimaryFactorContext ctx) {
        if (ctx.basicExpression() != null) return visit(ctx.basicExpression());
        if (ctx.setExpression() != null) return visit(ctx.setExpression());
        return ctx.getText();
    }


    // metodo auxiliar para processar sufixos com a base já calculada
    // base="this.alunos", suffix="->size()" -> "this.alunos.size()"
    private String visitPostfixTranslate(String base, OCLParser.PostfixSuffixContext ctx) {
        String text = ctx.getText();

        // ->size() -> .size()
        if (text.equals("->size()")) return base + ".size()";
        if (text.equals("->isEmpty()")) return base + ".isEmpty()";
        if (text.equals("->notEmpty()")) return "!" + base + ".isEmpty()";

        // .size() -> .size() (também existe como dot operation)
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

        // ->sum() -> .stream().mapToInt(Integer::intValue).sum()
        // LIMITAÇÃO: assume coleção de inteiros. Somas de double/float não são suportadas
        // (Integer::intValue não compila sobre Double). Suportar tipos numéricos variados
        // exigiria informação de tipo que o visitor não tem a este nível.
        if (text.equals("->sum()")) return base + ".stream().mapToInt(Integer::intValue).sum()";

        // ->max() -> .stream().max(...).orElse(null) ; ->min() análogo
        if (text.equals("->max()"))
            return base + ".stream().max(java.util.Comparator.naturalOrder()).orElse(null)";
        if (text.equals("->min()"))
            return base + ".stream().min(java.util.Comparator.naturalOrder()).orElse(null)";

        // ->count(x) -> nº de ocorrências de x na coleção
        if (text.startsWith("->count("))
            return "java.util.Collections.frequency(" + base + ", " + visit(ctx.expression(0)) + ")";

        // operações numéricas escalares (base é um número)
        // LIMITAÇÃO: floor()/round() devolvem 'long'. Com COMPARAÇÕES (>=, <=, >, <)
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

        // .propriedade -> .propriedade
        if (ctx.getChild(0).getText().equals(".") && ctx.ID().size() > 0) {
            return base + "." + ctx.ID(0).getText();
        }

        // ->first() -> .get(0)
        //if (text.equals("->first()")) return base + ".get(0)";

        // ->first() -> .stream().findFirst().orElse(null)
        // ->first().nome -> .stream().findFirst().orElse(null).getNome()
        if (text.startsWith("->first()")) {
            String result = base + ".stream().findFirst().orElse(null)";
            if (!ctx.ID().isEmpty()) {
                String prop = ctx.ID(0).getText();
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
        //if (text.equals("->any()")) return base + ".stream().findAny().orElse(null)";
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
            return base + ".stream().filter(__v -> !java.util.Objects.equals(__v, " + item +
                    ")).collect(java.util.stream.Collectors.toList())";
        }

        // fallback
        return base + text;
    }

    // Regra: conditionalExpression : 'if' expression 'then' expression 'else' expression 'endif'
    // -> ((cond) ? (then) : (else))
    @Override
    public String visitConditionalExpression(OCLParser.ConditionalExpressionContext ctx) {
        String condition = visit(ctx.expression(0));
        String thenPart = visit(ctx.expression(1));
        String elsePart = visit(ctx.expression(2));
        return "((" + condition + ") ? (" + thenPart + ") : (" + elsePart + "))";
    }

    // Regra: setExpression : 'Set{' expressionList? '}' | 'Sequence{' ... | etc
    // -> new HashSet<>() / new ArrayList<>() / new HashMap<>() (with optional Arrays.asList for non-empty)
    @Override
    public String visitSetExpression(OCLParser.SetExpressionContext ctx) {
        String first = ctx.getChild(0).getText();
        String javaCollection;

        if (first.equals("Map{")) {
            if (ctx.expressionList() == null) return "new HashMap<>()";
            throw new UnsupportedOperationException("Map literals with entries not supported");
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
