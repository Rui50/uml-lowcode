package generator;

import model.Association;
import model.Class;

import java.util.ArrayList;
import java.util.List;

/**
 * Uma relação navegável vista a partir de uma classe
 */
public class RelationshipInfo {
    public final String otherClass;          // a outra ponta
    public final String role;                // role do lado da outra ponta
    public final String otherMultiplicity;   // multiplicidade da OUTRA ponta
    public final Association assoc;
    public final boolean isSource;           // a classe é o lado SOURCE desta associação?

    public RelationshipInfo(String otherClass, String role, String otherMultiplicity, Association assoc, boolean isSource) {
        this.otherClass = otherClass;
        this.role = role;
        this.otherMultiplicity = otherMultiplicity;
        this.assoc = assoc;
        this.isSource = isSource;
    }

    // nome do campo (baseado no role, sem role usa a classe)
    public String field() {
        return GeneratorUtils.makeAttributeName(otherClass, role, otherMultiplicity);
    }

    public boolean isCollection() {
        return GeneratorUtils.checkMultiplicity(otherMultiplicity) || assoc.isMapEnd(isSource);
    }

    // lado da propria classe
    public String thisClass() {
        return isSource ? assoc.getSourceClass() : assoc.getTargetClass();
    }
    public String thisRole() {
        return isSource ? assoc.getSourceRole() : assoc.getTargetRole();
    }
    public String thisMul() {
        return isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
    }
    // esta ponta diz "muitos" -> o campo do outro lado é uma coleção
    public boolean inverseIsCollection() {
        return GeneratorUtils.checkMultiplicity(thisMul()) || assoc.isMapEnd(!isSource);
    }

    // true se este lado é o dono da relação (tem a FK/@JoinColumn), false = lado inverso (mappedBy)
    public boolean isOwner() {
        return GeneratorUtils.isOwningSide(assoc, isSource);
    }
    public boolean isBidirectional() {
        return assoc.isSourceNavigable() && assoc.isTargetNavigable();
    }

    // esta ponta expõe endpoints de coleção (add/remove) quando é o Map (qualificada),
    // ou um OneToMany unidirecional (o lado * de uma relação não bidirecional)
    public boolean hasCollectionEndpoint() {
        boolean qualifierHere = assoc.isMapEnd(isSource);
        return qualifierHere || (!inverseIsCollection() && isCollection() && !isBidirectional());
    }

    /**
     * Relações navegáveis de uma classe, uma por associação, vistas do lado da própria classe.
     * incluii as herdadas de um pai abstrato
     */
    public static List<RelationshipInfo> collect(Class cls, List<Association> associations) {
        List<RelationshipInfo> rels = new ArrayList<>();
        for (Association assoc : associations) {
            String declarer = GeneratorUtils.assocDeclaringClass(cls, assoc);
            if (declarer == null) continue;

            // auto associação bidirecional: a classe é os dois lados, por isso emite os dois roles aqui.
            // (numa associação normal o outro lado ia estar0 no collect() da outra classe, mas aqui é a mesma.)
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

    /**
     * Como collect(), mas inclui também as relações herdadas de um pai normal para os menus
     * (o menu de AlunoTE gere as relações herdadas de Aluno). O collect() normal exclui porque a
     * entidade/DTO já herda o campo do pai
     */
    public static List<RelationshipInfo> collectForMenus(Class cls, List<Association> associations) {
        List<RelationshipInfo> rels = new ArrayList<>();
        for (Association assoc : associations) {
            if (cls.isOrInheritsFrom(assoc.getSourceClass()) && assoc.isTargetNavigable()) {
                rels.add(new RelationshipInfo(assoc.getTargetClass(), assoc.getTargetRole(),
                        assoc.getTargetMultiplicity(), assoc, true));
            }
            if (cls.isOrInheritsFrom(assoc.getTargetClass()) && assoc.isSourceNavigable()) {
                rels.add(new RelationshipInfo(assoc.getSourceClass(), assoc.getSourceRole(),
                        assoc.getSourceMultiplicity(), assoc, false));
            }
        }
        return rels;
    }

    // só as relações de referência sing <x>Id
    public static List<RelationshipInfo> collectSingle(Class cls, List<Association> associations) {
        List<RelationshipInfo> rels = new ArrayList<>();
        for (RelationshipInfo r : collect(cls, associations)) {
            if (!r.isCollection()) rels.add(r);
        }
        return rels;
    }
}
