package service;

import model.ParsedModel;
import model.sequence.Interaction;
import parser.XMIParser;

import java.util.ArrayList;
import java.util.List;

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

        return new ParsedModel(
            parser.getClasses(),
            parser.getAssociations(),
            parser.getGeneralizations(),
            parser.getEnumerations(),
            psm,
            pim,
            all
        );

    }
}
