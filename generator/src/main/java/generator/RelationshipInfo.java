package generator;

import model.Association;
import model.Class;

import java.util.ArrayList;
import java.util.List;

/**
 * Uma relação navegável vista a partir de uma classe. Guarda a outra ponta (otherClass,
 * role, multiplicity), a associação, e de que lado desta associação está a classe (isSource).
 * Tudo o resto (nome do campo, lado próximo, dono/inverso, etc.) deriva-se daqui via os
 * métodos abaixo. Partilhado pelos geradores para falarem o mesmo vocabulário.
 */
public class RelationshipInfo {
    public final String otherClass;      // a outra ponta
    public final String role;            // role do lado da outra ponta
    public final String multiplicity;    // multiplicidade do lado da outra ponta
    public final Association assoc;
    public final boolean isSource;       // a classe é o lado SOURCE desta associação?

    public RelationshipInfo(String otherClass, String role, String multiplicity, Association assoc, boolean isSource) {
        this.otherClass = otherClass;
        this.role = role;
        this.multiplicity = multiplicity;
        this.assoc = assoc;
        this.isSource = isSource;
    }

    // nome do campo (baseado no role; sem role usa a classe) — igual em entidade/DTO/service
    public String field() {
        return GeneratorUtils.makeAttributeName(otherClass, role, multiplicity);
    }

    // coleção OU Map
    public boolean isCollection() {
        return otherMultiple() || assoc.hasQualifier();
    }
    public boolean otherMultiple() {
        return GeneratorUtils.checkMultiplicity(multiplicity);
    }

    // lado da PRÓPRIA classe
    // a classe deste lado (= a que declara a associação, ela ou um pai abstrato)
    public String thisClass() {
        return isSource ? assoc.getSourceClass() : assoc.getTargetClass();
    }
    public String thisRole() {
        return isSource ? assoc.getSourceRole() : assoc.getTargetRole();
    }
    public String thisMul() {
        return isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
    }
    public boolean thisMultiple() {
        return GeneratorUtils.checkMultiplicity(thisMul());
    }

    // este lado é o DONO da relação vs inverso (mappedBy)
    public boolean isOwner() {
        return GeneratorUtils.isOwningSide(assoc, isSource);
    }
    public boolean isBidirectional() {
        return assoc.isSourceNavigable() && assoc.isTargetNavigable();
    }

    /**
     * Relações navegáveis de uma classe (inclui as herdadas de um pai abstrato, via
     * assocDeclaringClass), uma por associação, vistas do lado da própria classe.
     */
    public static List<RelationshipInfo> collect(Class cls, List<Association> associations) {
        List<RelationshipInfo> rels = new ArrayList<>();
        for (Association assoc : associations) {
            String declarer = GeneratorUtils.assocDeclaringClass(cls, assoc);
            if (declarer == null) continue;
            boolean isSource = assoc.getSourceClass().equals(declarer);
            rels.add(new RelationshipInfo(
                    isSource ? assoc.getTargetClass() : assoc.getSourceClass(),
                    isSource ? assoc.getTargetRole() : assoc.getSourceRole(),
                    isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity(),
                    assoc, isSource));
        }
        return rels;
    }

    // só as relações de referência singular (não-coleção): campo <x>Id
    public static List<RelationshipInfo> collectSingle(Class cls, List<Association> associations) {
        List<RelationshipInfo> rels = new ArrayList<>();
        for (RelationshipInfo r : collect(cls, associations)) {
            if (!r.isCollection()) rels.add(r);
        }
        return rels;
    }
}
