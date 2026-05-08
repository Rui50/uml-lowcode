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
