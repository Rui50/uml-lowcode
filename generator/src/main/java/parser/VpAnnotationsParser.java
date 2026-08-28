package parser;

import model.annotations.Invariant;
import ocl.OCLToJavaVisitor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*https://st.inf.tu-dresden.de/files/general/OCLByExampleLecture.pdf
 * parsing OCL de notas
 * formato:
 *   context <ClassName> inv <InvariantName>: <OCLExpression>
 *
 *  "context <ClassName> não é necessário se ligar a nota a classe
 * */
public class VpAnnotationsParser {

    public static Invariant parseInvariant(String noteBody){
        if(noteBody == null || noteBody.isEmpty()){
            return null;
        }

        Pattern pattern = Pattern.compile(
            "inv\\s+(\\w+):\\s*(.+)",
                Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(noteBody.trim());

        if (matcher.find()) {
            String name = matcher.group(1);
            String oclExpression = matcher.group(2).trim();
            String message = "Invariant '" + name + "' violated.";

            oclExpression = oclExpression.replaceAll("\\s+", " ").trim();

            System.out.println("Parsed Invariant - Name: " + name + ", OCL Expression: " + oclExpression);

            Invariant inv = new Invariant(name, oclExpression, message);
            inv.setJavaExpression(OCLToJavaVisitor.toJava(oclExpression));
            return inv;
        }

        return null;
    }

    /**
     * Parsing de inits em ocl
     * formato: context ClassName::attrName : Type init: expression
     * String[2] com [0] = nome do atributo, [1] = expressão OCL, ou null se não match
     */
    public static String[] parseInit(String noteBody) {
        if (noteBody == null || noteBody.isEmpty()) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "context\\s+\\w+::(\\w+)\\s*:\\s*\\w+\\s+init\\s*:\\s*(.*)",
                Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(noteBody.trim());

        if (matcher.find()) {
            String attrName = matcher.group(1);
            String expression = matcher.group(2).replaceAll("\\s+", " ").trim();

            System.out.println("Parsed Init - Attribute: " + attrName + ", Expression: " + expression);

            return new String[]{attrName, expression};
        }

        return null;
    }

    /**
     * parsing de pre condições OCL
     * formato: context ClassName::operationName(params) pre name: expression
     * String[3] com [0] = nome da operação, [1] = nome da precondição, [2] = expressão OCL, ou null
     */
    public static String[] parsePreCondition(String noteBody) {
        if (noteBody == null || noteBody.isEmpty()) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "context\\s+\\w+::(\\w+)\\s*\\(.*?\\)\\s*pre\\s+(\\w+):\\s*(.*)",
                Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(noteBody.trim());

        if (matcher.find()) {
            String operationName = matcher.group(1);
            String preName = matcher.group(2);
            String expression = matcher.group(3).replaceAll("\\s+", " ").trim();

            System.out.println("Parsed Precondition - Operation: " + operationName +
                    ", Name: " + preName + ", Expression: " + expression);

            return new String[]{operationName, preName, expression};
        }

        return null;
    }

    public static String[] parseDerive(String noteBody){
        if(noteBody == null || noteBody.isEmpty()){
            return null;
        }

        Pattern pattern = Pattern.compile(
                "context\\s+\\w+::(\\w+)\\s*:\\s*\\w+\\s+derive\\s*:\\s*(.*)",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(noteBody.trim());
        if (matcher.find()) {
            String attrName = matcher.group(1);
            String expression = matcher.group(2).replaceAll("\\s+", " ").trim();

            System.out.println("Parsed Derive - Attribute: " + attrName + ", Expression: " + expression);

            return new String[]{attrName, expression};
        }

        return null;
    }
}
