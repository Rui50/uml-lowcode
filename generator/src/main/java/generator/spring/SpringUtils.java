package generator.spring;

import model.Class;
import model.Operation;
import model.sequence.Fragment;
import model.sequence.Interaction;
import model.sequence.Message;
import model.sequence.Operand;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SpringUtils {

    // encontra o diagrama de sequencia que corresponde a uma operação
    // procura o ponto de entrada: mensagem cujo source não é uma classe de domínio
    // e cujo target é {Cls}Controller ou {Cls}Service
    public static Interaction findInteractionForOperation(Class cls, Operation op,
                                                          List<Interaction> interactions,
                                                          List<Class> allClasses) {
        /*if (op.getSequenceDiagram() != null) return op.getSequenceDiagram();

        Set<String> classNames = allClasses.stream()
                .map(Class::getName)
                .collect(Collectors.toSet());

        String controllerType = cls.getName() + "Controller";
        String serviceType = cls.getName() + "Service";

        for (Interaction interaction : interactions) {
            for (Message msg : interaction.getMessages()) {
                if (msg.getSource() == null || msg.getTarget() == null) continue;

                String sourceType = msg.getSource().getType();
                if (classNames.contains(sourceType)) continue;

                if (msg.getName().equals(op.getName()) &&
                        msg.getTarget().getType() != null &&
                        (msg.getTarget().getType().equals(controllerType) ||
                                msg.getTarget().getType().equals(serviceType))) {
                    return interaction;
                }
                break;
            }
        }
        return null;*/
        return op.getSequenceDiagram();
    }

    // encontra a mensagem de entrada Controller -> Service (ou equivalente) dentro da interação
    // usada pelo controller e pelo service para ter a mesma assinatura
    public static Message findEntryMessage(Interaction interaction, Class cls, Operation op) {
        if (interaction == null) return null;
        String serviceType = cls.getName() + "Service";
        for (Message msg : interaction.getMessages()) {
            if (msg.getTarget() != null
                    && serviceType.equals(msg.getTarget().getType())
                    && msg.getName().equals(op.getName())) {
                return msg;
            }
        }
        return null;
    }
    /*
    public static Message findEntryMessage(Interaction interaction) {
        if (interaction == null) return null;
        return firstMessage(interaction.getElements());
    }

    private static Message firstMessage(List<Object> elements) {
        for (Object el : elements) {
            if (el instanceof Message m) return m;
            if (el instanceof Fragment f) {
                for (Operand op : f.getOperands()) {
                    Message inner = firstMessage(op.getElements());
                    if (inner != null) return inner;
                }
            }
        }
        return null;
    }*/
}
