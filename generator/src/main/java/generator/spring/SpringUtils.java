package generator.spring;

import model.Class;
import model.Operation;
import model.Parameter;
import model.sequence.Interaction;
import model.sequence.Message;
import generator.GeneratorUtils;
import java.util.*;

public class SpringUtils {

    // encontra a mensagem de entrada Controller -> Service (na interação é Ator -> Service)
    public static Message findEntryMessage(Interaction interaction, Class cls, Operation op) {
        if (interaction == null) return null;
        String serviceType = cls.getName() + "Service";
        // devolve a 1 que der match
        for (Message msg : interaction.getMessages()) {
            if (msg.getTarget() != null
                    && serviceType.equals(msg.getTarget().getType())
                    && op.getName().equals(msg.getName())) {
                return msg;
            }
        }
        return null;
    }

    /**
     * Tabela que faz nome do dto para -> classe owner, porque os params/retornos das ops custom
     * aparecem no modelo como strings (e "LoanResponseDTO") e daí temos de descobrir que
     * classe (logo, que folder) importar.
     */
    public static Map<String, String> buildDTOOwnerMap(List<Class> classes) {
        Map<String, String> map = new HashMap<>();
        for (Class c : classes) {
            String n = c.getName();
            if (!c.isAbstract()) {          // classes abstratas não têm Create/Update DTO
                map.put(n + "CreateDTO", n);
                map.put(n + "UpdateDTO", n);
            }
            map.put(n + "ResponseDTO", n);
            for (String ep : c.getResponseOverrides().keySet())   // overrides, incl nomes custom
                map.put(SpringDTOGenerator.responseDTOName(c, ep), n);
        }
        return map;
    }

    /**
     * Um tipo -> a sua classe dona (ou null se não for classe nem DTO conhecido). O tipo pode ser:
     *   - uma classe/entidade ("Loan")
     *   - um nome de DTO ("LoanResponseDTO")
     */
    public static String ownerClassOf(String typeName, Set<String> knownClasses, Map<String, String> DTO_ToClass) {
        if (typeName == null || typeName.isBlank()) return null;
        String base = GeneratorUtils.extractBaseType(typeName);
        if (knownClasses.contains(base)) return base;   // é uma classe (param/retorno entidade)
        return DTO_ToClass.get(base);                      // é um DTO -> a sua classe dona
    }

    /**
     * folders DTO que o service/controller desta classe tem de importar
     */
    public static Set<String> referencedDTOClasses(Class cls, Set<String> knownClasses, Map<String, String> DTO_ToClass) {
        // TreeSet para classes ordenadas, para os imports saírem sempre pela mesma ordem
        Set<String> needed = new TreeSet<>();

        // 1 a própria classe, os seus CRUD/override DTOs vivem sempre na sua folder
        needed.add(cls.getName());

        // 2 percorrer as operações custom desta classe (só essasops é que podem referir DTOs de outras classes)
        for (Operation op : GeneratorUtils.effectiveOperations(cls)) {
            if (op.isJob() || op.getSequenceDiagram() == null) continue;   // jobs / ops sem diagrama não geram endpoint com DTOs

            // 3. tipo de return, da mensagem de entrada se especificado, senão o declarado na operação
            Class owner = GeneratorUtils.declaringClassOf(cls, op);
            Message entry = findEntryMessage(op.getSequenceDiagram(), owner, op);
            String rt = (entry != null && entry.getReturnType() != null && !entry.getReturnType().isBlank())
                    ? entry.getReturnType() : op.getReturnType();
            String returnOwner = ownerClassOf(rt, knownClasses, DTO_ToClass);   // resolve o retorno -> classe dona
            if (returnOwner != null) needed.add(returnOwner);                   // junta a folder dessa classe

            // 4. cada param, obtem o tipo -> classe owner e junta a sua folder
            for (Parameter p : op.getParameters()) {
                String paramOwner = ownerClassOf(p.getType(), knownClasses, DTO_ToClass);
                if (paramOwner != null) needed.add(paramOwner);
            }
        }

        return needed;
    }
}
