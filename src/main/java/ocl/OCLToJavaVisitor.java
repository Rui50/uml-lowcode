package ocl;

public class OCLToJavaVisitor extends OCLBaseVisitor<String> {

    // standalone foi adicionado ao .g4 original, para ignorar a parte do "context X inv Y:"
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
    // Exemplo: se a expressão é "30", ctx.INT() retorna o token "30", e ctx.ID() retorna null.
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

        if (ctx.ID() != null) return ctx.ID().getText();

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
            switch (op) {
                case "and":
                case "&":
                    sb.append(" && ");
                    break;
                case "or":
                    sb.append(" || ");
                    break;
                case "implies":
                    sb.append(" || !");
                    break; // a implies b  ->  !a || b (simplificado)
                default:
                    sb.append(" " + op + " ");
                    break;
            }
            sb.append(visit(ctx.equalityExpression(i)));
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
            switch (op) {
                case "=":  sb.append(" == "); break;
                case "<>": sb.append(" != "); break;
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
    // aqui juntamos base + sufixos
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
        if (text.equals("->sum()")) return base + ".stream().mapToInt(Integer::intValue).sum()";

        // .propriedade -> .propriedade
        if (ctx.getChild(0).getText().equals(".") && ctx.ID().size() > 0) {
            return base + "." + ctx.ID(0).getText();
        }

        // fallback
        return base + text;
    }
}
