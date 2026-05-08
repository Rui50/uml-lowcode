package model.annotations;

import java.util.regex.Pattern;

public class PreSnapshot {
    private final String varName;       //  "alunos_pre"
    private final String javaInit;      // "new ArrayList<>(this.alunos)"
    private final String javaType;      //"List<Aluno>" or "int"

    public PreSnapshot(String varName, String javaInit, String javaType) {
        this.varName = varName;
        this.javaInit = javaInit;
        this.javaType = javaType;
    }

    public String getVarName() { return varName; }
    public String getJavaInit() { return javaInit; }
    public String getJavaType() { return javaType; }

    // para spring, substitui "this.campo" por "selfVar.getCampo()"
    public String getJavaInit(String selfVar) {
        if (javaInit == null || selfVar == null || selfVar.equals("this")) {
            return javaInit;
        }
        return Pattern.compile("this\\.(\\w+)").matcher(javaInit).replaceAll(mr -> {
            String f = mr.group(1);
            return selfVar + ".get" + Character.toUpperCase(f.charAt(0)) + f.substring(1) + "()";
        });
    }
}
