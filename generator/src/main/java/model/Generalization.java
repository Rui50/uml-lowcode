package model;

public class Generalization extends Relationship {

    public Generalization() {
        super();
    }

    public Generalization(String childClass, String parentClass) {
        // sourceClass = child
        // targetClass = parent
        super(childClass, parentClass);
    }

    public String getChildClass() {
        return getSourceClass();
    }

    public String getParentClass() {
        return getTargetClass();
    }

    public void setChildClass(String childClass) {
        setSourceClass(childClass);
    }

    public void setParentClass(String parentClass) {
        setTargetClass(parentClass);
    }

    @Override
    public String toString() {
        return "Generalization {child:'" + getSourceClass() +
                "' extends parent:'" + getTargetClass() + "'}";
    }
}