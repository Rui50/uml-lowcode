package model;

public class Generalization extends Relationship {
    public Generalization() {
        super();
    }

    public Generalization(String sourceClass, String targetClass) {
        super(sourceClass, targetClass);
    }

     @Override
    public String toString() {
        return "Generalization {" +
                ", sourceClass:'" + getSourceClass() + '\'' +
                " --|> " +
                ", targetClass:'" + getTargetClass() + '\'' +
                '}';
    }
}
