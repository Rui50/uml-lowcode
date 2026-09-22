package service;

import model.Attribute;
import model.Class;
import model.Enumeration;
import model.Interface;
import model.ParsedModel;
import model.sequence.Interaction;
import parser.XMIParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParsingService {
    public ParsedModel parse(String xmiFilePath) throws Exception {
        XMIParser parser = new XMIParser();
        parser.loadModel(xmiFilePath);

        List<Interaction> all = parser.getInteractions();
        List<Interaction> psm = new ArrayList<>();
        List<Interaction> pim = new ArrayList<>();
        for (Interaction inter : all) {
            String name = inter.getName();
            if (name == null) continue;
            if (name.endsWith("_psm")) {
                inter.setName(name.substring(0, name.length() - 4));
                psm.add(inter);
            } else if (name.endsWith("_pim")) {
                inter.setName(name.substring(0, name.length() - 4));
                pim.add(inter);
            }
        }

        ParsedModel model = new ParsedModel(
            parser.getClasses(),
            parser.getAssociations(),
            parser.getGeneralizations(),
            parser.getEnumerations(),
            psm,
            pim,
            all,
                parser.getInterfaces()
        );

        // avisos de tipos que podem estar errados, nao cancela so avisa
        List<String> typeWarnings = collectTypeWarnings(model.getClasses(), model.getEnumerations(), model.getInterfaces());
        model.getWarnings().addAll(typeWarnings);

        return model;
    }

    private static final Set<String> SIMPLE_TYPES = Set.of(
        "int", "Integer", "long", "Long", "short", "Short", "byte", "Byte",
        "double", "Double", "float", "Float", "boolean", "Boolean", "char", "Character",
        "String", "BigDecimal", "BigInteger",
        "LocalDate", "LocalDateTime", "LocalTime"
    );

    private List<String> collectTypeWarnings(List<Class> classes, List<Enumeration> enums, List<Interface> interfaces) {
        Set<String> types = new HashSet<>(SIMPLE_TYPES);
        // add classes enums e interfaces como tipos
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
