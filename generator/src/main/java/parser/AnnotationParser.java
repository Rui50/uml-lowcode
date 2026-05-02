package parser;

import model.Attribute;
import model.annotations.Invariant;
import model.annotations.Precondition;
import ocl.OCLToJavaVisitor;

public class AnnotationParser {
    public static Invariant parseInvariant(String name, String expression, String message) {
        Invariant inv = new Invariant(name, expression, message);
        inv.setJavaExpression(OCLToJavaVisitor.toJava(expression));
        return inv;
    }

    public static Precondition parsePrecondition(String name, String expression, String message) {
        Precondition pre = new Precondition(name, expression, message);
        pre.setJavaExpression(OCLToJavaVisitor.toJava(expression));
        return pre;
    }

    public static void applyInit(Attribute a, String expression) {
        a.setInitExpression(expression);
        a.setJavaInitExpression(OCLToJavaVisitor.toJava(expression));
    }

    public static void applyDerive(Attribute a, String expression) {
        a.setDeriveExpression(expression);
        a.setJavaDeriveExpression(OCLToJavaVisitor.toJava(expression));
    }}
