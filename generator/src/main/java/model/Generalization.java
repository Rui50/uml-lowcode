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

    public String getChildClassName() {
        return getSourceClass();
    }

    public String getParentClassName() {
        return getTargetClass();
    }

    public void setChildClassName(String childClass) {
        setSourceClass(childClass);
    }

    public void setParentClassName(String parentClass) {
        setTargetClass(parentClass);
    }

    @Override
    public String toString() {
        return "Generalization {child:'" + getSourceClass() +
                "' extends parent:'" + getTargetClass() + "'}";
    }
}