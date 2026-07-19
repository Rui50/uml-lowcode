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

    // coleção OU Map. O qualifier só torna coleção a ponta que DONA do Map (isQualifierOnSource),
    public boolean isCollection() {
        // return otherMultiple() || assoc.hasQualifier();
        return otherMultiple() || (assoc.hasQualifier() && isSource == assoc.isQualifierOnSource());
    }
    public boolean otherMultiple() {
        return GeneratorUtils.checkMultiplicity(multiplicity);
    }

    // lado da PRÓPRIA classe (a que declara a associação — ela própria ou um pai abstrato)
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

    // esta ponta expõe endpoints de coleção (add/remove) owner d Map  ,
    // OU um OneToMany unidirecional (o lado * de uma relação não-bidirecional)
    public boolean hasCollectionEndpoint() {
        boolean qualifierHere = assoc.hasQualifier() && isSource == assoc.isQualifierOnSource();
        return qualifierHere || (!thisMultiple() && otherMultiple() && !isBidirectional());
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

            // auto-associação bidirecional: a classe é os DOIS lados -> emite ambos os roles (como o
            // controller/service já fazem). Senão só sairia um lado e os DTOs/ligação perdiam o outro
            // (o "outro lado" normalmente vem da chamada da OUTRA classe, que aqui não existe).
            boolean selfBidirectional = assoc.getSourceClass().equals(assoc.getTargetClass())
                    && assoc.isSourceNavigable() && assoc.isTargetNavigable();
            if (selfBidirectional) {
                rels.add(new RelationshipInfo(assoc.getTargetClass(), assoc.getTargetRole(), assoc.getTargetMultiplicity(), assoc, true));
                rels.add(new RelationshipInfo(assoc.getSourceClass(), assoc.getSourceRole(), assoc.getSourceMultiplicity(), assoc, false));
            } else {
                boolean isSource = assoc.getSourceClass().equals(declarer);
                rels.add(new RelationshipInfo(
                        isSource ? assoc.getTargetClass() : assoc.getSourceClass(),
                        isSource ? assoc.getTargetRole() : assoc.getSourceRole(),
                        isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity(),
                        assoc, isSource));
            }
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
