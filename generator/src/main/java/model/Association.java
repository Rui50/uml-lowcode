package model;

public class Association extends Relationship {
    private String sourceRole;
    private String targetRole;

    private String sourceMultiplicity;
    private String targetMultiplicity;

    private String aggregationType; // "none", "shared", "composite"

    private boolean sourceNavigable;
    private boolean targetNavigable;

    // para os maps
    private String qualifierName;
    private String qualifierType;

    public Association() {
        super();
        this.sourceRole = "";
        this.targetRole = "";
        this.sourceMultiplicity = "";
        this.targetMultiplicity = "";
        this.aggregationType = "none";
        this.sourceNavigable = true;
        this.targetNavigable = true;
        this.qualifierName = "";
        this.qualifierType = "";
    }

    public Association(String sourceClass, String targetClass) {
        super(sourceClass, targetClass);
        this.sourceRole = "";
        this.targetRole = "";
        this.sourceMultiplicity = "";
        this.targetMultiplicity = "";
        this.aggregationType = "none";
        this.sourceNavigable = true;
        this.targetNavigable = true;
        this.qualifierName = "";
        this.qualifierType = "";
    }

    public String getSourceRole() {
        return sourceRole;
    }

    public void setSourceRole(String sourceRole) {
        this.sourceRole = sourceRole;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getSourceMultiplicity() {
        return sourceMultiplicity;
    }

    public void setSourceMultiplicity(String sourceMultiplicity) {
        this.sourceMultiplicity = sourceMultiplicity;
    }

    public String getTargetMultiplicity() {
        return targetMultiplicity;
    }

    public void setTargetMultiplicity(String targetMultiplicity) {
        this.targetMultiplicity = targetMultiplicity;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }

    public boolean isSourceNavigable() {
        return sourceNavigable;
    }

    public void setSourceNavigable(boolean sourceNavigable) {
        this.sourceNavigable = sourceNavigable;
    }

    public boolean isTargetNavigable() {
        return targetNavigable;
    }

    public void setTargetNavigable(boolean targetNavigable) {
        this.targetNavigable = targetNavigable;
    }

    public String getQualifierName() {
        return qualifierName;
    }

    public void setQualifierName(String qualifierName) {
        this.qualifierName = qualifierName;
    }

    public String getQualifierType() {
        return qualifierType;
    }

    public void setQualifierType(String qualifierType) {
        this.qualifierType = toJavaKeyType(qualifierType);
    }

    // as chaves de um Map têm de ser tipos de referência; o VP exporta os
    // primitivos em minúsculas ( "string", "int"), por isso normalizamos
    // para o tipo Java correspondente
    private static String toJavaKeyType(String t) {
        if (t == null) return null;
        switch (t.trim().toLowerCase()) {
            case "string":                 return "String";
            case "int": case "integer":    return "Integer";
            case "long":                   return "Long";
            case "short":                  return "Short";
            case "byte":                   return "Byte";
            case "boolean":                return "Boolean";
            case "char": case "character": return "Character";
            case "double":                 return "Double";
            case "float":                  return "Float";
            default:                       return t;   // tipo de referência definido pelo utilizador (classe/enum)
        }
    }

    public boolean hasQualifier() {
        return qualifierName != null && !qualifierName.isEmpty();
    }

    @Override
    public String toString() {
        return "Association {" +
                "sourceClass:'" + getSourceClass() + '\'' +
                ", targetClass:'" + getTargetClass() + '\'' +
                ", sourceRole:'" + sourceRole + '\'' +
                ", targetRole:'" + targetRole + '\'' +
                ", sourceMultiplicity:'" + sourceMultiplicity + '\'' +
                ", targetMultiplicity:'" + targetMultiplicity + '\'' +
                ", aggregationType:'" + aggregationType + '\'' +
                ", sourceNavigable:" + sourceNavigable +
                ", targetNavigable:" + targetNavigable +
                ", qualifierName:'" + qualifierName + '\'' +
                ", qualifierType:'" + qualifierType + '\'' +
                '}';
    }
}
