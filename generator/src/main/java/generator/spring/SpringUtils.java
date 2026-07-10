package generator.spring;

import model.Class;
import model.Operation;
import model.sequence.Interaction;
import model.sequence.Message;

public class SpringUtils {

    // encontra a mensagem de entrada Controller -> Service (ou equivalente) dentro da interação
    // usada pelo controller e pelo service para ter a mesma assinatura
    public static Message findEntryMessage(Interaction interaction, Class cls, Operation op) {
        if (interaction == null) return null;
        String serviceType = cls.getName() + "Service";
        for (Message msg : interaction.getMessages()) {
            if (msg.getTarget() != null
                    && serviceType.equals(msg.getTarget().getType())
                    && op.getName().equals(msg.getName())) {   // op primeiro: msg.getName() pode ser null (seta sem nome)
                return msg;
            }
        }
        return null;
    }
}
