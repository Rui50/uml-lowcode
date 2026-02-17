package model;

import javax.management.relation.Relation;

public class Association {

    private String type; // tipo de associação "association" ...
    private String sourceClass; // classe de origem
    private String targetClass; // classe de destino


    private String sourceMultiplicity; // multiplicidade na origem
    private String targetMultiplicity; // multiplicidade no destino
    private String sourceRole; // opcional
    private String targetRole; // opcional
    private String aggregation; // pode ser shared, composite

    public Association(){
        this.type = "";
        this.sourceClass = "";
        this.targetClass = "";
        this.sourceMultiplicity = "";
        this.targetMultiplicity = "";
        this.sourceRole = "";
        this.targetRole = "";
    }

    public Association(String type, String sourceClass, String targetClass, String sourceMultiplicity, String targetMultiplicity, String sourceRole, String targetRole, String aggregation) {
        this.type = type;
        this.sourceClass = sourceClass;
        this.targetClass = targetClass;
        this.sourceMultiplicity = sourceMultiplicity;
        this.targetMultiplicity = targetMultiplicity;
        this.sourceRole = sourceRole;
        this.targetRole = targetRole;
        this.aggregation = aggregation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSourceClass() {
        return sourceClass;
    }

    public void setSourceClass(String sourceClass) {
        this.sourceClass = sourceClass;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
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

    public String getAggregation() {
        return aggregation;
    }

    public void setAggregation(String aggregation) {
        this.aggregation = aggregation;
    }

    @Override
    public String toString() {
        return "Association {" +
                "type:'" + type + '\'' +
                ", sourceClass:'" + sourceClass + '\'' +
                ", targetClass:'" + targetClass + '\'' +
                ", sourceMultiplicity:'" + sourceMultiplicity + '\'' +
                ", targetMultiplicity:'" + targetMultiplicity + '\'' +
                ", sourceRole:'" + sourceRole + '\'' +
                ", targetRole:'" + targetRole + '\'' +
                ", aggregation:'" + aggregation + '\'' +
                '}';
    }
}
