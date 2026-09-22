package service;

import generator.GeneratorUtils;
import model.Association;
import model.Attribute;
import model.Class;
import model.Enumeration;
import model.Interface;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModelValidator {

    /**
     * numa auto associação (classe é os dois lados) cada ponta navegável precisa de um role distinto
     * sem isso os campos/parâmetros gerados colidem.
     */
    public static List<String> findSelfAssociationErrors(List<Association> associations) {
        List<String> messages = new ArrayList<>();
        if (associations == null) return messages;
        for (Association a : associations) {
            if (a.getSourceClass() == null || !a.getSourceClass().equals(a.getTargetClass())) continue;
            String srcRole = a.getSourceRole();
            String tgtRole = a.getTargetRole();
            boolean srcMissing = a.isSourceNavigable() && (srcRole == null || srcRole.isBlank());
            boolean tgtMissing = a.isTargetNavigable() && (tgtRole == null || tgtRole.isBlank());
            boolean sameRole = a.isSourceNavigable() && a.isTargetNavigable()
                    && srcRole != null && !srcRole.isBlank() && srcRole.equals(tgtRole);
            if (srcMissing || tgtMissing || sameRole) {
                messages.add("Self association on '" + a.getSourceClass() + "': each navigable end needs a "
                        + "distinct role (name). Without distinct roles the generated "
                        + "fields and parameters collide (the class is both ends of the association).");
            }
        }
        return messages;
    }

    /**
     * duas associações sem roles para a mesma classe geram o mesmo campo
     */
    public static List<String> findRelationFieldCollisions(List<Association> associations) {
        List<String> messages = new ArrayList<>();
        if (associations == null) return messages;
        List<String> seen = new ArrayList<>();   // campos já gerados, no formato "Classe.campo"
        for (Association a : associations) {
            // o campo vive na classe do lado OPOSTO à ponta navegável
            if (a.isTargetNavigable() && a.getSourceClass() != null) {
                String attribute = GeneratorUtils.makeAttributeName(a.getTargetClass(), a.getTargetRole(), a.getTargetMultiplicity());
                String key = a.getSourceClass() + "." + attribute;
                if (seen.contains(key)) {
                    messages.add("Class '" + a.getSourceClass() + "': two or more associations generate the same field '"
                            + attribute + "'. Give the navigable ends distinct roles to separate them.");
                } else {
                    seen.add(key);
                }
            }
            if (a.isSourceNavigable() && a.getTargetClass() != null) {
                String attribute = GeneratorUtils.makeAttributeName(a.getSourceClass(), a.getSourceRole(), a.getSourceMultiplicity());
                String key = a.getTargetClass() + "." + attribute;
                if (seen.contains(key)) {
                    messages.add("Class '" + a.getTargetClass() + "': two or more associations generate the same field '"
                            + attribute + "'. Give the navigable ends distinct roles to separate them.");
                } else {
                    seen.add(key);
                }
            }
        }
        return messages;
    }

    // tipos normais aceites
    private static final Set<String> SIMPLE_TYPES = Set.of(
        "int", "Integer", "long", "Long", "short", "Short", "byte", "Byte",
        "double", "Double", "float", "Float", "boolean", "Boolean", "char", "Character",
        "String", "BigDecimal", "BigInteger",
        "LocalDate", "LocalDateTime", "LocalTime"
    );

    /**
     * tipos de atributo que não são normais nem correspondem a nenhuma classe/enum/interface do modelo
     * so avisa
     */
    public static List<String> collectTypeWarnings(List<Class> classes, List<Enumeration> enums, List<Interface> interfaces) {
        Set<String> types = new HashSet<>(SIMPLE_TYPES);
        for (Class c : classes) types.add(c.getName());
        for (Enumeration e : enums) types.add(e.getName());
        for (Interface i : interfaces) types.add(i.getName());

        List<String> warnings = new ArrayList<>();
        for (Class c : classes) {
            for (Attribute a : c.getAttributes()) {
                String t = a.getType();
                if (t == null || types.contains(t)) continue;
                warnings.add("Type '" + t + "' on attribute '" + c.getName() + "." + a.getName()
                    + "' doesnt seem like a normal type, check it's correct (may fail to compile).");
            }
        }
        return warnings;
    }
}
