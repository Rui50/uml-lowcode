package model;

/**
 * Representa um parâmetro dentro de um metodo
 */

public class Parameter {
    private String name;
    private String type;

    public Parameter() {
        this.name = "";
        this.type = "";
    }

    public Parameter(String name, String type) {
        this.name = name;
        this.type = type;
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

    @Override
    public String toString() {
        return "Parameter {" +
                "name:'" + name + '\'' +
                ", type:'" + type + '\'' +
                '}';
    }
}
