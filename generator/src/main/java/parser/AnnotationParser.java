package parser;

import generator.GeneratorUtils;
import generator.RelationshipInfo;
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

    // traduz e aplica as correções que precisam do modelo (Maps, size() de String)
    private static String toJava(String expression, Class cls, List<Association> associations) {
        String java = GeneratorUtils.fixMapAccess(OCLToJavaVisitor.toJava(expression), cls, associations);
        return GeneratorUtils.fixStringSize(java, cls);
    }

    public static Invariant parseInvariant(String name, String expression, String message,
                                           Class cls, List<Association> associations) {
        Invariant inv = new Invariant(name, expression, message);
        inv.setJavaExpression(toJava(expression, cls, associations));
        return inv;
    }

    public static Precondition parsePrecondition(String name, String expression, String message,
                                                 Class cls, List<Association> associations) {
        Precondition pre = new Precondition(name, expression, message);
        pre.setJavaExpression(toJava(expression, cls, associations));
        return pre;
    }

    public static Postcondition parsePostcondition(String name, String expression, String message,
                                                   Class cls, List<Association> associations) {
        Postcondition post = new Postcondition(name, expression, message);

        // scan por @pre
        String oclWithoutPre = processPreReferences(expression, cls, associations, post.getPreSnapshots());

        post.setJavaExpression(toJava(oclWithoutPre, cls, associations));
        return post;
    }

    public static void applyInit(Attribute a, String expression,
                                 Class cls, List<Association> associations) {
        a.setInitExpression(expression);
        a.setJavaInitExpression(toJava(expression, cls, associations));
    }

    public static void applyDerive(Attribute a, String expression,
                                   Class cls, List<Association> associations) {
        a.setDeriveExpression(expression);
        a.setJavaDeriveExpression(toJava(expression, cls, associations));
    }


    // Trata as referências @pre de uma pós condição antes de passar a expressão ao ANTLR
    // por cada self.x@pre (ou x@pre), regista um snapshot do estado anterior numa variável x_pre e
    // substitui o @pre por essa variável
    private static String processPreReferences(String ocl, Class cls, List<Association> associations,
                                               List<PreSnapshot> snapshots) {
        // apanha self.x@pre ou x@pre
        // so suporta @pre sobre um atributo simples (self.x@pre)
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

        String result = sb.toString();
        // se ainda tem @pre é porque esse nao é suportado
        // falha aqui em vez de deixar o ANTLR rebentar
        if (result.contains("@pre")) {
            throw new IllegalArgumentException(
                    "@pre só é suportado sobre um atributo simples (self.x@pre). Não suporta: " + ocl);
        }

        snapshots.addAll(snapshotsmap.values());
        return sb.toString();
    }

    private static PreSnapshot buildSnapshot(String attrName, String varName,
                                             Class cls, List<Association> associations) {
        // snapshot @pre guarda só o valor (primitivos/String/enum) ou os elementos que a coleção tinha nesse momento

        // atributo próprio ou herdado, coleção faz cópia
        for (Attribute a : cls.getAllAttributes()) {
            if (!a.getName().equals(attrName)) continue;
            String getter = "this.get" + GeneratorUtils.capitalizeFirst(attrName) + "()";
            if (a.isCollection()) {
                String copy = a.getCollectionType().equals("List") ? "ArrayList" : "HashSet";
                return new PreSnapshot(varName, "new " + copy + "<>(" + getter + ")", a.getAttributeType());
            }
            return new PreSnapshot(varName, getter, a.getType());
        }

        // relação própria ou herdada, pelo nome do campo gerado
        // só a ponta do qualifier é que é Map
        for (RelationshipInfo rel : RelationshipInfo.collectForMenus(cls, associations)) {
            if (!rel.attributeName().equals(attrName)) continue;
            String getter = "this.get" + GeneratorUtils.capitalizeFirst(attrName) + "()";
            if (rel.isMap()) {
                return new PreSnapshot(varName, "new HashMap<>(" + getter + ")",
                        "Map<" + rel.assoc.getQualifierType() + ", " + rel.otherClass + ">");
            }
            if (rel.isCollection()) {
                return new PreSnapshot(varName, "new ArrayList<>(" + getter + ")", "List<" + rel.otherClass + ">");
            }
            return new PreSnapshot(varName, getter, rel.otherClass);
        }
        throw new IllegalArgumentException("@pre references unknown property '" + attrName + "' on class " + cls.getName());
    }
}
