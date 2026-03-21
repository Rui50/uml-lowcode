package model.sequence;

import java.util.ArrayList;
import java.util.List;

public class Interaction {
    private String id;
    private String name; // nome operação
    private String contextClass; // clase que contem metodo
    private List<Lifeline> lifelines;
    private List<Message> messages;

    public Interaction(){
        this.lifelines = new ArrayList<>();
        this.messages = new ArrayList<>();
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

    public String getContextClass() {
        return contextClass;
    }
    public void setContextClass(String contextClass) {
        this.contextClass = contextClass;
    }

    public List<Lifeline> getLifelines() {
        return lifelines;
    }
    public void setLifelines(List<Lifeline> lifelines) {
        this.lifelines = lifelines;
    }

    public void addLifeline(Lifeline lifeline) {
        this.lifelines.add(lifeline);
    }

    public List<Message> getMessages() {
        return messages;
    }
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

     @Override
    public String toString() {
         return "Interaction {" +
                 "id:'" + id + '\'' +
                 ", name:'" + name + '\'' +
                 ", contextClass:'" + contextClass + '\'' +
                 ", lifelines:" + lifelines +
                 ", messages:" + messages +
                 '}';
    }
}
