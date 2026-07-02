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

    private String deriveExpression;
    private String javaDeriveExpression;

    // escondido do DTO gerado correspondente (anotação, default = visível em todos)
    private boolean hiddenInCreate;
    private boolean hiddenInUpdate;
    private boolean hiddenInResponse;

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

    public String getDeriveExpression() {
        return deriveExpression;
    }

    public void setDeriveExpression(String deriveExpression) {
        this.deriveExpression = deriveExpression;
    }

    public String getJavaDeriveExpression() { return javaDeriveExpression; }
    public void setJavaDeriveExpression(String javaDeriveExpression) { this.javaDeriveExpression = javaDeriveExpression; }

    public boolean hasDerive() { return deriveExpression != null; }

    public boolean isHiddenInCreate() { return hiddenInCreate; }
    public void setHiddenInCreate(boolean hiddenInCreate) { this.hiddenInCreate = hiddenInCreate; }

    public boolean isHiddenInUpdate() { return hiddenInUpdate; }
    public void setHiddenInUpdate(boolean hiddenInUpdate) { this.hiddenInUpdate = hiddenInUpdate; }

    public boolean isHiddenInResponse() { return hiddenInResponse; }
    public void setHiddenInResponse(boolean hiddenInResponse) { this.hiddenInResponse = hiddenInResponse; }

    @Override
    public String toString() {
        return "Atribute {" +
                "name:'" + name + '\'' +
                ", type:'" + type + '\'' +
                ", visibility:'" + visibility + '\'' +
                '}';

    }

}
