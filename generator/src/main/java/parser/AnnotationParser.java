package parser;

import generator.GeneratorUtils;
import model.Association;
import model.Attribute;
import model.Class;
import model.annotations.Invariant;
import model.annotations.Postcondition;
import model.annotations.Precondition;
import ocl.OCLToJavaVisitor;

import model.annotations.PreSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.List;

public class AnnotationParser {

    public static Invariant parseInvariant(String name, String expression, String message,
                                           Class cls, List<Association> associations) {
        Invariant inv = new Invariant(name, expression, message);
        inv.setJavaExpression(GeneratorUtils.fixMapAccess(OCLToJavaVisitor.toJava(expression), cls, associations));
        return inv;
    }

    public static Precondition parsePrecondition(String name, String expression, String message,
                                                 Class cls, List<Association> associations) {
        Precondition pre = new Precondition(name, expression, message);
        pre.setJavaExpression(GeneratorUtils.fixMapAccess(OCLToJavaVisitor.toJava(expression), cls, associations));
        return pre;
    }

    public static Postcondition parsePostcondition(String name, String expression, String message,
                                                   Class cls, List<Association> associations) {
        Postcondition post = new Postcondition(name, expression, message);

        // scan por @pre
        String oclWithoutPre = processPreReferences(expression, cls, associations, post.getPreSnapshots());

        post.setJavaExpression(GeneratorUtils.fixMapAccess(OCLToJavaVisitor.toJava(oclWithoutPre), cls, associations));
        return post;
    }

    public static void applyInit(Attribute a, String expression,
                                 Class cls, List<Association> associations) {
        a.setInitExpression(expression);
        a.setJavaInitExpression(GeneratorUtils.fixMapAccess(OCLToJavaVisitor.toJava(expression), cls, associations));
    }

    public static void applyDerive(Attribute a, String expression,
                                   Class cls, List<Association> associations) {
        a.setDeriveExpression(expression);
        a.setJavaDeriveExpression(GeneratorUtils.fixMapAccess(OCLToJavaVisitor.toJava(expression), cls, associations));
    }


    // Trata as referências @pre de uma pós-condição antes de passar a expressão ao ANTLR.
    // O @pre não faz parte da gramática OCL, por isso é resolvemos aqui fora
    // por cada self.x@pre (ou x@pre), regista um snapshot do estado anterior numa variável x_pre e
    // substitui o @pre por essa variável, deixando uma expressão que o ANTLR já aceita.
    // Devolve a expressão com os @pre substituídos.
    private static String processPreReferences(String ocl, Class cls, List<Association> associations,
                                               List<PreSnapshot> snapshots) {
        // apanha self.x@pre ou x@pre
        // so suporta @pre sobre um atributo simples (self.x@pre).
        // @pre sobre navegação aninhada (self.a.b@pre) ou expressões (self.alunos->size()@pre)
        // se nao for reconhecido, fica como literal no OCL.
        Pattern p = Pattern.compile("(self\\.)?([a-zA-Z_][a-zA-Z0-9_]*)@pre");
        Matcher m = p.matcher(ocl);

        Map<String, PreSnapshot> snapshotsmap = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();

        while(m.find()) {
            String attrName = m.group(2);
            String varName = attrName + "_pre";
            if(!snapshotsmap.containsKey(varName)) {
                PreSnapshot snap = buildSnapshot(attrName, varName, cls, associations);
                snapshotsmap.put(varName, snap);
            }
            // substitui "self.xx@pre" ou "xx@pre" por "xx_pre" no OCL
            m.appendReplacement(sb, Matcher.quoteReplacement(varName));
        }
        m.appendTail(sb);

        snapshots.addAll(snapshotsmap.values());
        return sb.toString();
    }

    private static PreSnapshot buildSnapshot(String attrName, String varName,
                                             Class cls, List<Association> associations) {
        // o snapshot @pre é raso: guarda o valor (primitivos/String/enum) ou os elementos que a
        // coleção tinha nesse momento, mas não o estado interno de cada objeto. Serve para
        // comparar valores ou tamanhos antes/depois, não o conteúdo profundo dos objetos.
        // ver se é atributo — snapshot é cópia por valor
        for (Attribute a : cls.getAttributes()) {
            if (a.getName().equals(attrName)) {
                return new PreSnapshot(varName, "this.get" + GeneratorUtils.capitalizeFirst(attrName) + "()", a.getType());
            }
        }
        // associação (multiplicidade *) ou referência única
        for (Association assoc : associations) {
            String otherClass, mult;
            if (cls.getName().equals(assoc.getSourceClass()) && attrName.equals(assoc.getTargetRole())) {
                otherClass = assoc.getTargetClass();
                mult = assoc.getTargetMultiplicity();
            } else if (cls.getName().equals(assoc.getTargetClass()) && attrName.equals(assoc.getSourceRole()))
            {
                otherClass = assoc.getSourceClass();
                mult = assoc.getSourceMultiplicity();
            } else {
                continue;
            }
            if (assoc.hasQualifier()) {
                // associação qualifier ->  é Map<QualifierType, OtherClass>
                return new PreSnapshot(varName,
                        "new HashMap<>(this.get" + GeneratorUtils.capitalizeFirst(attrName) + "())",
                        "Map<" + assoc.getQualifierType() + ", " + otherClass + ">");
            }
            if (mult != null && mult.contains("*")) {
                return new PreSnapshot(varName, "new ArrayList<>(this.get" + GeneratorUtils.capitalizeFirst(attrName) + "())", "List<" + otherClass + ">");
            }
            return new PreSnapshot(varName, "this.get" + GeneratorUtils.capitalizeFirst(attrName) + "()", otherClass);
        }
        throw new IllegalArgumentException("@pre references unknown property '" + attrName + "' on class " + cls.getName());
    }
}
