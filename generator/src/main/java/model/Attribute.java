package model;

/*
    * Representa um atributo de uma classe UML.
 */
public class Attribute {
    private String name;
    private String type;
    private String visibility;

    private String collectionType; // "List"/"Set"

    private String initExpression; // expressão ocl para inits
    private String javaInitExpression; // expressão java

    private String deriveExpression;
    private String javaDeriveExpression;

    // configs opcional da tab Persistence
    private boolean unique;      // @Column(unique = true)
    private Integer precision;   // decimais total de dígitos
    private Integer scale;       // decimais casas decimais

    public Attribute() {
        this.name = "";
        this.type = "";
        this.visibility = "";
    }

    public Attribute(String name, String type, String visibility) {
        this.name = name;
        this.type = type;
        this.visibility = visibility;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCollectionType() {
        return collectionType;
    }
    public void setCollectionType(String collectionType) {
        this.collectionType = collectionType;
    }
    public boolean isCollection() {
        return collectionType != null;
    }

    // "List<String>" para listas ou sets senao o tipo normal
    public String getAttributeType() {
        if (isCollection()) {
            return collectionType + "<" + type + ">";
        } else {
            return type;
        }
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getInitExpression() {
        return initExpression;
    }

    public void setInitExpression(String initExpression) {
        this.initExpression = initExpression;
    }

    public boolean hasInit() {
        return initExpression != null && !initExpression.isEmpty();
    }

    public String getJavaInitExpression() { return javaInitExpression; }
    public void setJavaInitExpression(String javaInitExpression) { this.javaInitExpression = javaInitExpression; }

    public String getDeriveExpression() {
        return deriveExpression;
    }

    public void setDeriveExpression(String deriveExpression) {
        this.deriveExpression = deriveExpression;
    }

    public String getJavaDeriveExpression() { return javaDeriveExpression; }
    public void setJavaDeriveExpression(String javaDeriveExpression) { this.javaDeriveExpression = javaDeriveExpression; }

    public boolean hasDerive() { return deriveExpression != null && !deriveExpression.isEmpty(); }

    public boolean isUnique() { return unique; }
    public void setUnique(boolean unique) { this.unique = unique; }

    public Integer getPrecision() { return precision; }
    public void setPrecision(Integer precision) { this.precision = precision; }

    public Integer getScale() { return scale; }
    public void setScale(Integer scale) { this.scale = scale; }

    public boolean hasColumnConfig() {
        return unique || precision != null || scale != null;
    }

    @Override
    public String toString() {
        return "Atribute {" +
                "name:'" + name + '\'' +
                ", type:'" + type + '\'' +
                ", visibility:'" + visibility + '\'' +
                '}';

    }

}
