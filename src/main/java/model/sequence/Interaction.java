package model.sequence;

import java.util.ArrayList;
import java.util.List;

public class Interaction {
    private String id;
    private String name;
    private String contextClass;
    private List<Lifeline> lifelines;
    private List<Object> elements;  // Message ou Fragment, por ordem

    public Interaction() {
        this.lifelines = new ArrayList<>();
        this.elements = new ArrayList<>();
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

    public void addLifeline(Lifeline lifeline) {
        this.lifelines.add(lifeline);
    }

    public List<Object> getElements() {
        return elements;
    }

    public void addElement(Object element) {
        this.elements.add(element);
    }

    /**
     * Compatibilidade temporária — extrai só as Messages (root-level) da lista de elements.
     * TODO: remover quando os generators forem atualizados para usar getElements()
     */
    public List<Message> getMessages() {
        List<Message> msgs = new ArrayList<>();
        for (Object el : elements) {
            if (el instanceof Message) {
                msgs.add((Message) el);
            }
        }
        return msgs;
    }

    @Override
    public String toString() {
        return "Interaction{" +
                "name='" + name + '\'' +
                ", contextClass='" + contextClass + '\'' +
                ", lifelines=" + lifelines +
                ", elements=" + elements +
                '}';
    }
}