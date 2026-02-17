package model;

public class Relationship {

    private String sourceClass; // classe de origem
    private String targetClass; // classe de destino

    public Relationship(){
        this.sourceClass = "";
        this.targetClass = "";
    }

    public Relationship(String sourceClass, String targetClass) {
        this.sourceClass = sourceClass;
        this.targetClass = targetClass;
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


    @Override
    public String toString() {
        return "Relationship {" +
                ", sourceClass:'" + sourceClass + '\'' +
                ", targetClass:'" + targetClass + '\'' +
                '}';
    }
}
