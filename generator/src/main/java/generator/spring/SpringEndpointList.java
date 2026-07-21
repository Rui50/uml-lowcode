package generator.spring;

import generator.RelationshipInfo;
import model.Association;
import model.Class;

import generator.GeneratorUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Que endpoints uma classe vai expor
 * para a frontend ter lista
 */
public class SpringEndpointList {

    public enum TYPE {
        CRUD,
        ASSOCIATION,
        CUSTOM
    }

    public enum DTOType {
        CREATE,
        UPDATE,
        RESPONSE
    }

    public record Endpoint(
        String httpMethod, // GET, etc
        String path, // /users, /users/{id}, etc
        TYPE type,
        String label, // label for the endpoint
        DTOType dtoType // CREATE, UPDATE, RESPONSE
    ) {};


    public static List<Endpoint> getClassEndpoints(Class cls, List<Association> associations) {
        List<Endpoint> endpoints = new ArrayList<>();
        if (cls.isAbstract()) return endpoints;

        String base = "/api/" + plural(cls.getName());

        endpoints.add(new Endpoint("GET",    base,           TYPE.CRUD, "List all",  DTOType.RESPONSE));
        endpoints.add(new Endpoint("GET",    base + "/{id}", TYPE.CRUD, "Get by id", DTOType.RESPONSE));
        endpoints.add(new Endpoint("POST",   base,           TYPE.CRUD, "Create",    DTOType.CREATE));
        endpoints.add(new Endpoint("PATCH",  base + "/{id}", TYPE.CRUD, "Update",    DTOType.UPDATE));
        endpoints.add(new Endpoint("PUT",    base + "/{id}", TYPE.CRUD, "Replace",   DTOType.UPDATE));
        endpoints.add(new Endpoint("DELETE", base + "/{id}", TYPE.CRUD, "Delete",    null));


        // now assoc endpoints
        for (Association assoc : associations) {
            // quem gera os endpoints desta associação (própria/pai abstrato); null => ignora
            String declarer = GeneratorUtils.assocDeclaringClass(cls, assoc);
            if (declarer == null) continue;

            boolean selfBidirectional = assoc.getSourceClass().equals(assoc.getTargetClass())
                    && assoc.isSourceNavigable() && assoc.isTargetNavigable();

            if (selfBidirectional) {
                addAssociationEndpoints(endpoints, base, new RelationshipInfo(assoc.getTargetClass(), assoc.getTargetRole(), assoc.getTargetMultiplicity(), assoc, true));
                addAssociationEndpoints(endpoints, base, new RelationshipInfo(assoc.getSourceClass(), assoc.getSourceRole(), assoc.getSourceMultiplicity(), assoc, false));
            } else {
                boolean isSource = assoc.getSourceClass().equals(declarer);
                addAssociationEndpoints(endpoints, base, new RelationshipInfo(
                        isSource ? assoc.getTargetClass() : assoc.getSourceClass(),
                        isSource ? assoc.getTargetRole()  : assoc.getSourceRole(),
                        isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity(),
                        assoc, isSource));
            }
        }
        return endpoints;
    }

    private static void addAssociationEndpoints(List<Endpoint> endpoints, String base, RelationshipInfo rel){
        String otherPath = rel.field().toLowerCase();
        String otherVar = GeneratorUtils.lowercaseFirst(rel.otherClass);

        // ManyToOne que gera  PUT /{id}/{other}/{otherId} + DELETE /{id}/{other}
        if (rel.inverseIsCollection() && !rel.isCollection()) {
            endpoints.add(new Endpoint("PUT",    base + "/{id}/" + otherPath + "/{" + otherVar + "Id}", TYPE.ASSOCIATION, "Assign " + rel.otherClass, null));
            endpoints.add(new Endpoint("DELETE", base + "/{id}/" + otherPath, TYPE.ASSOCIATION, "Unassign " + rel.otherClass, null));
        }
        // ManyToMany lado dono que gera POST + DELETE /{id}/{others}
        else if (rel.inverseIsCollection() && rel.isCollection() && rel.isOwner()) {
            endpoints.add(new Endpoint("POST",   base + "/{id}/" + otherPath, TYPE.ASSOCIATION, "Add " + plural(rel.otherClass), null));
            endpoints.add(new Endpoint("DELETE", base + "/{id}/" + otherPath, TYPE.ASSOCIATION, "Remove " + rel.otherClass, null));
        }
        // OneToMany unidir / Map POST + DELETE /{id}/{others}
        else if (rel.hasCollectionEndpoint()) {
            endpoints.add(new Endpoint("POST",   base + "/{id}/" + otherPath, TYPE.ASSOCIATION, "Add " + plural(rel.otherClass), null));
            endpoints.add(new Endpoint("DELETE", base + "/{id}/" + otherPath, TYPE.ASSOCIATION, "Remove " + rel.otherClass, null));
        }
    }

    private static String plural(String className) {
        String p = className.toLowerCase();
        return p.endsWith("s") ? p : p + "s";
    }
}
