package model.annotations;

public class PreSnapshot {
    private final String varName;       //  "alunos_pre"
    private final String javaInit;      // "new ArrayList<>(this.getAlunos())"
    private final String javaType;      //"List<Aluno>" or "int"

    public PreSnapshot(String varName, String javaInit, String javaType) {
        this.varName = varName;
        this.javaInit = javaInit;
        this.javaType = javaType;
    }

    public String getVarName() { return varName; }
    public String getJavaInit() { return javaInit; }
    public String getJavaType() { return javaType; }

    // para spring, aponta o self ao selfVar (o javaInit já usa getters: this.getCampo())
    public String getJavaInit(String selfVar) {
        if (javaInit == null || selfVar == null || selfVar.equals("this")) {
            return javaInit;
        }
        return javaInit.replace("this.", selfVar + ".");
    }
}
