package model;

/*
    * Representa um atributo de uma classe UML.
 */
public class Attribute {
    private String name;
    private String type;
    private String visibility;

    private String initExpression; // expressão ocl para inicialização do atributo
    private String javaInitExpression; // expressão java

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

    @Override
    public String toString() {
        return "Atribute {" +
                "name:'" + name + '\'' +
                ", type:'" + type + '\'' +
                ", visibility:'" + visibility + '\'' +
                '}';

    }

}
