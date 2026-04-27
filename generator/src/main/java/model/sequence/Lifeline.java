package model.sequence;

public class Lifeline {
    private String id;
    private String name;  // nome dentro da lifeline
    private String represents; // variavel
    private String type; // class

    public Lifeline(){
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRepresents() {
        return represents;
    }

    public void setRepresents(String represents) {
        this.represents = represents;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Lifeline {" +
                "id:'" + id + '\'' +
                ", name:'" + name + '\'' +
                ", represents:'" + represents + '\'' +
                ", type:'" + type + '\'' +
                '}';
    }
}
