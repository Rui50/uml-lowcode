package generator;

import model.Association;

/**
 * Dados crus de uma relação navegável vista a partir de uma classe: a outra ponta,
 * o role e a multiplicidade desse lado, e a associação de origem. Tudo o que se
 * deriva daqui (nome do campo, tipo de id, dono/inverso, etc.) calcula-se na altura
 * via GeneratorUtils. Partilhado pelos vários geradores para falarem o mesmo vocabulário.
 */
public class RelationshipInfo {
    public final String otherClass;
    public final String role;
    public final String multiplicity;
    public final Association assoc;

    public RelationshipInfo(String otherClass, String role, String multiplicity, Association assoc) {
        this.otherClass = otherClass;
        this.role = role;
        this.multiplicity = multiplicity;
        this.assoc = assoc;
    }
}
