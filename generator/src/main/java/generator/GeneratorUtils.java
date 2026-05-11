package generator;

import model.Association;
import model.Class;
import model.Operation;
import model.Parameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GeneratorUtils {

    public static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String lowercaseFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    // List<Teste.Aluno> -> List<Aluno>
    public static String removePackageNames(String type) {
        if (type == null) return null;
        return type.replaceAll("\\b([A-Za-z_$][\\w$]*\\.)+([A-Za-z_$][\\w$]*)\\b", "$2");
    }

    public static boolean checkMultiplicity(String multiplicity) {
        if (multiplicity == null) return false;
        return multiplicity.contains("*") ||
                (multiplicity.contains("..") && !multiplicity.equals("0..1"));
    }

    public static String makeAttributeName(String className, String role, String multiplicity) {
        if (role != null && !role.trim().isEmpty()) return role.trim();
        String base = lowercaseFirst(className);
        return checkMultiplicity(multiplicity) ? base + "s" : base;
    }

    // extrair o tipo "base" de retorno e parametros: List<Aluno> -> Aluno, Map<Integer, Turma> -> Turma
    public static String extractBaseType(String type) {
        if (type == null) return null;
        type = removePackageNames(type.trim());
        if (type.contains("<")) {
            int start = type.indexOf("<") + 1;
            int end = type.lastIndexOf(">");
            if (start < end) {
                String generic = type.substring(start, end).trim();
                if (generic.contains(",")) {
                    String[] parts = generic.split(",");
                    return extractBaseType(parts[parts.length - 1].trim());
                }
                return extractBaseType(generic);
            }
        }
        return type;
    }

    // entidades referenciadas em parametros / return de operações
    public static List<String> getModelsToImportFromOperations(Class cls, List<Class> classes) {
        Set<String> entityNames = new HashSet<>();
        for (Class c : classes) entityNames.add(c.getName());

        Set<String> entitiesToImport = new HashSet<>();
        for (Operation op : cls.getOperations()) {
            for (Parameter param : op.getParameters()) {
                String baseType = extractBaseType(param.getType());
                if (baseType != null && entityNames.contains(baseType) && !baseType.equals(cls.getName())) {
                    entitiesToImport.add(baseType);
                }
            }
            String returnType = extractBaseType(op.getReturnType());
            if (returnType != null && entityNames.contains(returnType) && !returnType.equals(cls.getName())) {
                entitiesToImport.add(returnType);
            }
        }
        return new ArrayList<>(entitiesToImport);
    }

    // entidades referenciadas em assocs navegaveis
    public static List<String> getModelsToImportFromAssociations(Class cls, List<Association> associations) {
        Set<String> entitiesToImport = new HashSet<>();
        for (Association assoc : associations) {
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                String relatedClass = assoc.getTargetClass();
                if (!relatedClass.equals(cls.getName())) entitiesToImport.add(relatedClass);
            }
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                String relatedClass = assoc.getSourceClass();
                if (!relatedClass.equals(cls.getName())) entitiesToImport.add(relatedClass);
            }
        }
        return new ArrayList<>(entitiesToImport);
    }

    public static Class findClassByName(List<Class> classes, String name) {
        for (Class c : classes) {
            if (c.getName().equals(name)) return c;
        }
        return null;
    }

    /**
     * corrige acesso a Maps (campos com qualifier): .stream() -> .values().stream()
     */
    public static String fixMapAccess(String java, Class cls, List<Association> associations) {
        if (java == null) return null;
        List<String> mapFields = getMapFieldNames(cls, associations);
        for (String field : mapFields) {
            java = java.replace("this." + field + ".stream()", "this." + field + ".values().stream()");
        }
        return java;
    }

    /**
     * devolve nomes dos campos que são Map (assoc tem qualifier)
     */
    public static List<String> getMapFieldNames(Class cls, List<Association> associations) {
        List<String> mapFields = new ArrayList<>();
        for (Association assoc : associations) {
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable() && assoc.hasQualifier()) {
                String role = assoc.getTargetRole();
                if (role != null && !role.trim().isEmpty()) {
                    mapFields.add(role.trim());
                } else {
                    String base = assoc.getTargetClass().substring(0, 1).toLowerCase() + assoc.getTargetClass().substring(1);
                    mapFields.add(base + "s");
                }
            }
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable() && assoc.hasQualifier()) {
                String role = assoc.getSourceRole();
                if (role != null && !role.trim().isEmpty()) {
                    mapFields.add(role.trim());
                } else {
                    String base = assoc.getSourceClass().substring(0, 1).toLowerCase() + assoc.getSourceClass().substring(1);
                    mapFields.add(base + "s");
                }
            }
        }
        return mapFields;
    }
}
