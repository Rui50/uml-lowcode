package model;

public class Association extends Relationship {
    private String sourceRole;
    private String targetRole;

    private String sourceMultiplicity;
    private String targetMultiplicity;

    private String aggregationType; // "none", "shared", "composite"

    public Association() {
        super();
        this.sourceRole = "";
        this.targetRole = "";
        this.sourceMultiplicity = "";
        this.targetMultiplicity = "";
        this.aggregationType = "none";
    }

    public Association(String sourceClass, String targetClass) {
        super(sourceClass, targetClass);
        this.sourceRole = "";
        this.targetRole = "";
        this.sourceMultiplicity = "";
        this.targetMultiplicity = "";
        this.aggregationType = "none";
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
                '}';
    }
}
