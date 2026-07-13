package generator.CLI_JPA;

import generator.GeneratorUtils;
import generator.OperationGenerator;
import model.*;
import model.Class;
import model.Enumeration;
import model.sequence.Interaction;
import model.sequence.Message;
import model.sequence.MessageArgument;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gera os menus CLI
 *
 * para cada classe gera um menu com:
 *  - CRUD
 *  - opções de associação (add/remove ou assign/remove)
 *  - opções para operações custom (ligadas a uma Interaction)
 *
 * id: usar id da classe raiz no caso de ser herança;
 * create e update excluem sempre o atributo-id (auto-gerado / PK).
 */
public class CLIMenuGenerator {

    private final String baseOutputPath;
    private final String basePackage;
    private final Set<String> enumNames = new HashSet<>();
    private final List<Interaction> interactions;

    // nome -> Class, para resolver o tipo do id de outra classe nas relações
    private final Map<String, Class> classMap = new HashMap<>();

    public CLIMenuGenerator(String baseOutputPath, String basePackage, List<Interaction> interactions) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
        this.interactions = interactions;
    }

    // tipo do id de uma classe (pelo nome)
    private String idTypeOf(String className) {
        return GeneratorUtils.idType(classMap.get(className));
    }

    // metodo do InputUtils a usar
    private String idReaderOf(String className) {
        switch (idTypeOf(className)) {
            case "String":  return "readString";
            case "UUID":    return "readUuid";
            case "Integer": return "readInt";
            default:        return "readLong";
        }
    }

    /**
     * entrada: gera todos os menus (um por entidade + MainMenu)
     */
    public void generateMenus(List<Class> classes, List<Association> associations, List<Enumeration> enumerations) throws IOException {
        enumNames.clear();
        for (Enumeration e : enumerations) {
            enumNames.add(e.getName());
        }
        classMap.clear();
        for (Class c : classes) classMap.put(c.getName(), c);
        System.out.println("Generating Java Menus...");

        String menusPath = baseOutputPath + "/src/main/java/" + basePackage.replace('.', '/') + "/menus";
        new File(menusPath).mkdirs();

        List<Class> classesToBuild = classes.stream()
                .filter(c -> !c.isAbstract())
                .collect(Collectors.toList());

        for (Class cls : classesToBuild) {
            generateEntityMenu(cls, classes, associations, menusPath);
        }

        generateMainMenu(classesToBuild, associations, menusPath);
        System.out.println("Java menu generation completed.");
    }

    /*
    * gerar menu para cada entidade
     */
    private void generateEntityMenu(Class cls, List<Class> allClasses, List<Association> associations, String menusDir) throws IOException {
        String className = cls.getName() + "Menu";
        String filename = menusDir + "/" + className + ".java";

        // verificar que repos este menu precisa

        Set<String> neededRepos = GeneratorUtils.getNeededRepositories(cls, associations, allClasses);

        try (PrintWriter w = new PrintWriter(filename)) {
            w.println("package " + basePackage + ".menus;");
            w.println();
            w.println("import " + basePackage + ".models.*;");
            w.println("import " + basePackage + ".repository.*;");
            w.println("import " + basePackage + ".util.InputUtils;");
            w.println("import jakarta.persistence.EntityManager;");
            w.println("import java.util.*;");
            w.println("import java.time.*;");
            w.println();
            w.println("public class " + className + " {");
            w.println();

            // fields
            w.println("    private Scanner scanner;");
            w.println("    private EntityManager em;");
            w.println("    private " + cls.getName() + "Repository " + GeneratorUtils.repoVariable(cls.getName()) + ";");
            for (String related : neededRepos) {
                w.println("    private " + related + "Repository " + GeneratorUtils.repoVariable(related) + ";");
            }
            w.println();

            // constructor
            generateMenuConstructor(w, cls, neededRepos);

            // run(): loop principal do menu com switch
            generateRunMethod(w, cls, associations);

            // CRUD
            if (!cls.isAbstract()) generateCreateMethod(w, cls);
            generateViewAllMethod(w, cls, associations);
            generateViewByIdMethod(w, cls, associations);
            generateUpdateMethod(w, cls);
            generateDeleteMethod(w, cls);

            // methodos de associação
            generateRelationshipMethods(w, cls, associations);

            // metodos para operações custom (ligadas a uma Interaction)
            generateCustomOperationMethods(w, cls, allClasses);

            w.println("}");
        }
    }

    /**
     * Gera construtor do menu
     * Recebe Scanner e os repositórios necessários
     */
    private void generateMenuConstructor(PrintWriter w, Class cls, Set<String> neededRepos) {
        w.println("    public " + cls.getName() + "Menu(Scanner scanner, EntityManager em) {");
        w.println("        this.scanner = scanner;");
        w.println("        this.em = em;");
        w.println("        this." + GeneratorUtils.repoVariable(cls.getName()) + " = new " + cls.getName() + "Repository(em);");
        for (String related : neededRepos) {
            w.println("        this." + GeneratorUtils.repoVariable(related) + " = new " + related + "Repository(em);");
        }
        w.println("    }");
        w.println();
    }

    /**
     * Gera metodo run() (loop do menu)
     * crud (create, view all , view by id, update, delete)
     * depois as de associação
     *   - Multiplicidade * (OneToMany/ManyToMany): Add + Remove
     *   - Multiplicidade 1 (ManyToOne): Assign + Remove
     */
    private void generateRunMethod(PrintWriter w, Class cls, List<Association> associations) {
        // menu options
        // [0] = label para o menu, [1] = nome do método a chamar
        List<String[]> options = new ArrayList<>();
        if (!cls.isAbstract()) options.add(new String[]{"Create", "create"});
        options.add(new String[]{"View All", "viewAll"});
        options.add(new String[]{"View by ID", "viewById"});
        options.add(new String[]{"Update", "update"});
        options.add(new String[]{"Delete", "delete"});

        // adicionar das relações
        for (Association assoc : associations) {
            if (cls.isOrInheritsFrom(assoc.getSourceClass()) && assoc.isTargetNavigable()) {
                String other = assoc.getTargetClass();
                // nome do campo com base nas roles, evita problemas de 2 ou mais assoc entr
                // associações para a mesma classe, a ke  da opção tem de bater certo com o metodo
                // evitar problemas   case 6: addAuthors(); mas o metodo é addAuthor
                String fieldName = GeneratorUtils.makeAttributeName(other, assoc.getTargetRole(), assoc.getTargetMultiplicity());
                String cap = GeneratorUtils.capitalizeFirst(fieldName);

                if (GeneratorUtils.checkMultiplicity(assoc.getTargetMultiplicity()) || assoc.hasQualifier()) {
                    // OneToMany, ManyToMany ou Map: Add e Remove
                    options.add(new String[]{"Add " + fieldName, "add" + cap});
                    options.add(new String[]{"Remove " + fieldName, "remove" + cap});
                }
                else {
                    // ManyToOne: Assign e Remove
                    options.add(new String[]{"Assign " + fieldName, "assign" + cap});
                    options.add(new String[]{"Remove " + fieldName, "remove" + cap});
                }
            }
            if (cls.isOrInheritsFrom(assoc.getTargetClass()) && assoc.isSourceNavigable()) {
                String other = assoc.getSourceClass();
                String fieldName = GeneratorUtils.makeAttributeName(other, assoc.getSourceRole(), assoc.getSourceMultiplicity());
                String cap = GeneratorUtils.capitalizeFirst(fieldName);

                if (GeneratorUtils.checkMultiplicity(assoc.getSourceMultiplicity()) || assoc.hasQualifier()) {
                    options.add(new String[]{"Add " + fieldName, "add" + cap});
                    options.add(new String[]{"Remove " + fieldName, "remove" + cap});
                }
                else {
                    options.add(new String[]{"Assign " + fieldName, "assign" + cap});
                    options.add(new String[]{"Remove " + fieldName, "remove" + cap});
                }
            }
        }

        for (Operation op : GeneratorUtils.effectiveOperations(cls)) {
            Interaction interaction = op.getSequenceDiagram();
            if (interaction != null) {
                String label = GeneratorUtils.capitalizeFirst(op.getName());
                options.add(new String[]{label, op.getName()});
            }
        }

        // run method
        w.println("    public void run() {");
        w.println("        while (true) {");
        w.println("            System.out.println(\"\\n=== " + cls.getName() + " ===\");");
        for (int i = 0; i < options.size(); i++) {
            w.println("            System.out.println(\"" + (i + 1) + ". " + options.get(i)[0] + "\");");
        }
        w.println("            System.out.println(\"0. Back\");");
        w.println();
        w.println("            int option = InputUtils.readInt(scanner, \"Option: \");");
        w.println("            switch (option) {");
        for (int i = 0; i < options.size(); i++) {
            w.println("                case " + (i + 1) + ": " + options.get(i)[1] + "(); break;");
        }
        w.println("                case 0: return;");
        w.println("                default: System.out.println(\"Invalid option.\");");
        w.println("            }");
        w.println("        }");
        w.println("    }");
        w.println();
    }

    /**
     * create(), le cada atributo (pai + proprio)
     * nao le atributos derivados
     * valida invariantes
     */
    private void generateCreateMethod(PrintWriter w, Class cls) {
        w.println("    private void create() {");

        // recolher atributos do pai + próprios (mesma ordem que o construtor)
        // excluir o atributo-id
        String idName = cls.getRoot().getIdAttributeName();

        List<Attribute> allAttrs = new ArrayList<>();
        // atributos herdados de todos os pais (raiz-primeiro), não só do pai directo
        allAttrs.addAll(GeneratorUtils.ancestorAttributes(cls).stream()
                .filter(a -> !a.hasDerive() && !a.hasInit())
                .filter(a -> cls.isIdAssigned() || !a.getName().equals(idName))
                .collect(Collectors.toList()));
        allAttrs.addAll(cls.getAttributes().stream()
                .filter(a -> !a.hasDerive() && !a.hasInit())
                .filter(a -> !a.getName().equals(idName))
                .collect(Collectors.toList()));

        // ler cada atributo
        for (Attribute attr : allAttrs) {
            generateReadInputForAttribute(w, attr);
        }
        w.println();

        // criar entidade no-arg + setters
        w.println("        " + cls.getName() + " entity = new " + cls.getName() + "();");
        for (Attribute attr : allAttrs) {
            String cap = GeneratorUtils.capitalizeFirst(attr.getName());
            w.println("        entity.set" + cap + "(" + attr.getName() + ");");
        }

        // validation
        if (cls.hasAnyInvariants()) {
            w.println();
            w.println("        List<String> errors = entity.validate();");
            w.println("        if (!errors.isEmpty()) {");
            w.println("            System.out.println(\"Validation errors:\");");
            w.println("            errors.forEach(e -> System.out.println(\"  - \" + e));");
            w.println("            return;");
            w.println("        }");
        }

        w.println();
        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + GeneratorUtils.repoVariable(cls.getName()) + ".save(entity);");
        w.println("            em.getTransaction().commit();");
        w.println("            System.out.println(\"Created: \" + entity);");
        w.println("        } catch (RuntimeException ex) {");
        w.println("            if (em.getTransaction().isActive()) em.getTransaction().rollback();");
        w.println("            em.clear();");
        w.println("            System.out.println(\"Error: \" + ex.getMessage());");
        w.println("        }");
        w.println("    }");
    }

    /**
     * viewAll(): lista todas as entidades do repositório.
     */
    private void generateViewAllMethod(PrintWriter w, Class cls, List<Association> associations) {
        w.println("    private void viewAll() {");
        w.println("        List<" + cls.getName() + "> all = " + GeneratorUtils.repoVariable(cls.getName()) + ".findAll();");
        w.println("        if (all.isEmpty()) {");
        w.println("            System.out.println(\"No records found.\");");
        w.println("            return;");
        w.println("        }");
        w.println("        for (" + cls.getName() + " entity : all) {");
        w.println("            System.out.println(entity);");
        generateRelationShipPrints(w, cls, associations, "            ");
        w.println("        }");
        w.println("    }");
        w.println();
    }

    /**
     * viewById(): pede ID e mostra a entidade.
     */
    private void generateViewByIdMethod(PrintWriter w, Class cls, List<Association> associations) {
        w.println("    private void viewById() {");
        w.println("        " + idTypeOf(cls.getName()) + " id = InputUtils." + idReaderOf(cls.getName()) + "(scanner, \"ID: \");");
        w.println("        " + cls.getName() + " entity = " + GeneratorUtils.repoVariable(cls.getName()) + ".findById(id);");
        w.println("        if (entity == null) {");
        w.println("            System.out.println(\"Not found.\");");
        w.println("            return;");
        w.println("        }");
        w.println("        System.out.println(entity);");
        generateRelationShipPrints(w, cls, associations, "        ");
        w.println("    }");
        w.println();
    }

    /**
     * mete os prints das relações
     */
    private void generateRelationShipPrints(PrintWriter w, Class cls, List<Association> associations, String indent) {
        for (Association assoc : associations) {
            String other = null;
            String role = null;
            String mult = null;
            if (cls.isOrInheritsFrom(assoc.getSourceClass()) && assoc.isTargetNavigable()) {
                other = assoc.getTargetClass();
                role = assoc.getTargetRole();
                mult = assoc.getTargetMultiplicity();
            } else if (cls.isOrInheritsFrom(assoc.getTargetClass()) && assoc.isSourceNavigable()) {
                other = assoc.getSourceClass();
                role = assoc.getSourceRole();
                mult = assoc.getSourceMultiplicity();
            }
            if (other == null) continue;

            String fieldName = GeneratorUtils.makeAttributeName(other, role, mult);
            String cap = GeneratorUtils.capitalizeFirst(fieldName);
            String getter = "entity.get" + cap + "()";

            if (assoc.hasQualifier()) {
                w.println(indent + "System.out.println(\"  " + fieldName + ": \" + " + getter + ".values());");
            } else {
                w.println(indent + "System.out.println(\"  " + fieldName + ": \" + " + getter + ");");
            }
        }
    }

    /**
     *  update(): com ID, mostra valores atuais, permite alterar cada atributo.
     *  enter sem valor mantém o valor atual.
     *
     */
    private void generateUpdateMethod(PrintWriter w, Class cls) {
        w.println("    private void update() {");
        w.println("        " + idTypeOf(cls.getName()) + " id = InputUtils." + idReaderOf(cls.getName()) + "(scanner, \"ID: \");");
        w.println("        " + cls.getName() + " entity = " + GeneratorUtils.repoVariable(cls.getName()) + ".findById(id);");
        w.println("        if (entity == null) {");
        w.println("            System.out.println(\"Not found.\");");
        w.println("            return;");
        w.println("        }");
        w.println();
        w.println("        System.out.println(\"Current: \" + entity);");
        w.println("        System.out.println(\"(press Enter to keep current value)\");");
        w.println();

        // para cada atributo (pai + próprios) ler novo valor ou manter atual
        // PK não muda em update; excluir sempre o atributo-id (se houver)
        String idAttrName = cls.getRoot().getIdAttributeName();

        List<Attribute> allAttrs = new ArrayList<>();
        // atributos herdados de TODA a cadeia de pais (raiz-primeiro), não só do pai directo (nota #30)
        allAttrs.addAll(GeneratorUtils.ancestorAttributes(cls).stream()
                .filter(a -> !a.hasDerive())
                .filter(a -> !a.getName().equals(idAttrName))
                .collect(Collectors.toList()));
        allAttrs.addAll(cls.getAttributes().stream()
                .filter(a -> !a.hasDerive())
                .filter(a -> !a.getName().equals(idAttrName))
                .collect(Collectors.toList()));
        for (Attribute attr : allAttrs) {
            generateUpdateAttribute(w, attr);
        }

        // validation
        if (cls.hasAnyInvariants()) {
            w.println();
            w.println("        List<String> errors = entity.validate();");
            w.println("        if (!errors.isEmpty()) {");
            w.println("            System.out.println(\"Validation errors:\");");
            w.println("            errors.forEach(e -> System.out.println(\"  - \" + e));");
            w.println("            return;");
            w.println("        }");
        }

        w.println();
        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + GeneratorUtils.repoVariable(cls.getName()) + ".save(entity);");
        w.println("            em.getTransaction().commit();");
        w.println("            System.out.println(\"Updated: \" + entity);");
        w.println("        } catch (RuntimeException ex) {");
        w.println("            if (em.getTransaction().isActive()) em.getTransaction().rollback();");
        w.println("            em.clear();");
        w.println("            System.out.println(\"Error: \" + ex.getMessage());");
        w.println("        }");
        w.println("    }");
        w.println();
    }

    /**
     *  delete(): pede ID e remove do repositório.
     */
    private void generateDeleteMethod(PrintWriter w, Class cls) {
        w.println("    private void delete() {");
        w.println("        " + idTypeOf(cls.getName()) + " id = InputUtils." + idReaderOf(cls.getName()) + "(scanner, \"ID: \");");
        w.println("        " + cls.getName() + " entity = " + GeneratorUtils.repoVariable(cls.getName()) + ".findById(id);");
        w.println("        if (entity == null) {");
        w.println("            System.out.println(\"Not found.\");");
        w.println("            return;");
        w.println("        }");
        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + GeneratorUtils.repoVariable(cls.getName()) + ".deleteById(id);");
        w.println("            em.getTransaction().commit();");
        w.println("            System.out.println(\"Deleted.\");");
        w.println("        } catch (RuntimeException ex) {");
        w.println("            if (em.getTransaction().isActive()) em.getTransaction().rollback();");
        w.println("            em.clear();");
        w.println("            System.out.println(\"Error: \" + ex.getMessage());");
        w.println("        }");
        w.println("    }");
    }

    /**
     * Metodos de relações
     * - Multiplicidade 1 (ManyToOne): assign e remove
     * - Multiplicidade * (OneToMany/ManyToMany): add  e remove
     */

    /**
     * Itera sobre as associações da classe e gera os métodos apropriados.
     */
    private void generateRelationshipMethods(PrintWriter w, Class cls, List<Association> associations) {
        for (Association assoc : associations) {
            if (cls.isOrInheritsFrom(assoc.getSourceClass()) && assoc.isTargetNavigable()) {
                String other = assoc.getTargetClass();
                String fieldName = GeneratorUtils.makeAttributeName(other, assoc.getTargetRole(), assoc.getTargetMultiplicity());

                if (GeneratorUtils.checkMultiplicity(assoc.getTargetMultiplicity()) || assoc.hasQualifier()) {
                    generateAddMethod(w, cls.getName(), other, fieldName, assoc, true);
                    generateRemoveFromListMethod(w, cls.getName(), other, fieldName, assoc, true);
                }
                else {
                    generateAssignMethod(w, cls.getName(), other, fieldName);
                    generateRemoveAssignMethod(w, cls.getName(), other, fieldName);
                }
            }
            if (cls.isOrInheritsFrom(assoc.getTargetClass()) && assoc.isSourceNavigable()) {
                String other = assoc.getSourceClass();
                String fieldName = GeneratorUtils.makeAttributeName(other, assoc.getSourceRole(), assoc.getSourceMultiplicity());

                if (GeneratorUtils.checkMultiplicity(assoc.getSourceMultiplicity()) || assoc.hasQualifier()) {
                    generateAddMethod(w, cls.getName(), other, fieldName, assoc, false);
                    generateRemoveFromListMethod(w, cls.getName(), other, fieldName, assoc, false);
                }
                else {
                    generateAssignMethod(w, cls.getName(), other, fieldName);
                    generateRemoveAssignMethod(w, cls.getName(), other, fieldName);
                }
            }
        }
    }

    // valida a entidade que vai ser persistida (saveVar) antes do save. Sem isto, mudar uma relação
    // (assign/add/remove) nunca corria os invariantes OCL. Estilo dos menus: imprime os erros e return.
    private void emitRelationValidation(PrintWriter w, String saveClass, String saveVar) {
        Class c = classMap.get(saveClass);
        if (c == null || !c.hasAnyInvariants()) return;
        w.println("        List<String> errors = " + saveVar + ".validate();");
        w.println("        if (!errors.isEmpty()) {");
        w.println("            System.out.println(\"Validation errors:\");");
        w.println("            errors.forEach(e -> System.out.println(\"  - \" + e));");
        w.println("            return;");
        w.println("        }");
        w.println();
    }

    // Assign method (ManyToOne): entity.setOther(other)
    private void generateAssignMethod(PrintWriter w, String ownerClass, String otherClass, String fieldName) {
        String capitalized = GeneratorUtils.capitalizeFirst(fieldName);
        w.println("    private void assign" + capitalized + "() {");
        w.println("        " + idTypeOf(ownerClass) + " ownerId = InputUtils." + idReaderOf(ownerClass) + "(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " owner = " + GeneratorUtils.repoVariable(ownerClass) + ".findById(ownerId);");
        w.println("        if (owner == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        " + idTypeOf(otherClass) + " otherId = InputUtils." + idReaderOf(otherClass) + "(scanner, \"" + otherClass + " ID: \");");
        w.println("        " + otherClass + " other = " + GeneratorUtils.repoVariable(otherClass) + ".findById(otherId);");
        w.println("        if (other == null) { System.out.println(\"" + otherClass + " not found.\"); return; }");
        w.println();
        w.println("        owner.set" + capitalized + "(other);");
        emitRelationValidation(w, ownerClass, "owner");
        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + GeneratorUtils.repoVariable(ownerClass) + ".save(owner);");
        w.println("            em.getTransaction().commit();");
        w.println("            em.refresh(other);");
        w.println("            System.out.println(\"Assigned " + otherClass + " to " + ownerClass + ".\");");
        w.println("        } catch (RuntimeException ex) {");
        w.println("            if (em.getTransaction().isActive()) em.getTransaction().rollback();");
        w.println("            em.clear();");
        w.println("            System.out.println(\"Error: \" + ex.getMessage());");
        w.println("        }");
        w.println("    }");
        w.println();
    }

    // remove assign
    private void generateRemoveAssignMethod(PrintWriter w, String ownerClass, String otherClass, String fieldName) {
        String capitalized = GeneratorUtils.capitalizeFirst(fieldName);
        w.println("    private void remove" + capitalized + "() {");
        w.println("        " + idTypeOf(ownerClass) + " id = InputUtils." + idReaderOf(ownerClass) + "(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " entity = " + GeneratorUtils.repoVariable(ownerClass) + ".findById(id);");
        w.println("        if (entity == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        " + otherClass + " previous = entity.get" + capitalized + "();");
        w.println("        entity.set" + capitalized + "(null);");
        emitRelationValidation(w, ownerClass, "entity");
        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + GeneratorUtils.repoVariable(ownerClass) + ".save(entity);");
        w.println("            em.getTransaction().commit();");
        w.println("            if (previous != null) em.refresh(previous);");
        w.println("            System.out.println(\"" + otherClass + " removed from \" + entity);");
        w.println("        } catch (RuntimeException ex) {");
        w.println("            if (em.getTransaction().isActive()) em.getTransaction().rollback();");
        w.println("            em.clear();");
        w.println("            System.out.println(\"Error: \" + ex.getMessage());");
        w.println("        }");
        w.println("    }");
        w.println();
    }

    // add (OneToMany/ManyToMany/Map): list.add(other) ou map.put(key, other)
    private void generateAddMethod(PrintWriter w, String ownerClass, String otherClass, String fieldName, Association assoc, boolean isSource) {
        String capitalized = GeneratorUtils.capitalizeFirst(fieldName);
        w.println("    private void add" + capitalized + "() {");
        w.println("        " + idTypeOf(ownerClass) + " ownerId = InputUtils." + idReaderOf(ownerClass) + "(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " owner = " + GeneratorUtils.repoVariable(ownerClass) + ".findById(ownerId);");
        w.println("        if (owner == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        " + idTypeOf(otherClass) + " otherId = InputUtils." + idReaderOf(otherClass) + "(scanner, \"" + otherClass + " ID: \");");
        w.println("        " + otherClass + " other = " + GeneratorUtils.repoVariable(otherClass) + ".findById(otherId);");
        w.println("        if (other == null) { System.out.println(\"" + otherClass + " not found.\"); return; }");
        w.println();

        // uma relação só é persistida se for escrita pelo lado DONO.
        // Se o campo é o lado inverso (que tem mappedBy), gravar owner.getCampo().add(other) não escreve nada
        // temos de escrever o campo dono em 'other' e gravaresse  'other'.
        // (qualifier/Map sempre owner.)
        boolean isOwner = GeneratorUtils.isOwningSide(assoc, isSource);
        String saveVar, saveRepo, refreshVar, saveClass;

        if (assoc.hasQualifier()) {
            String qualCap = GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            w.println("        owner.get" + capitalized + "().put(other.get" + qualCap + "(), other);");
            saveVar = "owner"; saveRepo = GeneratorUtils.repoVariable(ownerClass); refreshVar = "other"; saveClass = ownerClass;
        } else if (isOwner) {
            w.println("        owner.get" + capitalized + "().add(other);");
            saveVar = "owner"; saveRepo = GeneratorUtils.repoVariable(ownerClass); refreshVar = "other"; saveClass = ownerClass;
        } else {
            // lado inverso escrevemos pelo lado DONO na var other
            String ownerCap = GeneratorUtils.capitalizeFirst(GeneratorUtils.getOtherSideAttributeName(assoc, isSource ? "target" : "source"));
            boolean ownerIsCollection = GeneratorUtils.checkMultiplicity(
                    isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity());
            // escrever só o lado DONO; o em.refresh(owner) no fim recarrega o lado inverso da BD
            w.println(ownerIsCollection
                    ? "        other.get" + ownerCap + "().add(owner);   // lado DONO: é o que persiste"
                    : "        other.set" + ownerCap + "(owner);   // lado DONO: é o que persiste");
            saveVar = "other"; saveRepo = GeneratorUtils.repoVariable(otherClass); refreshVar = "owner"; saveClass = otherClass;
        }

        emitRelationValidation(w, saveClass, saveVar);
        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + saveRepo + ".save(" + saveVar + ");");
        w.println("            em.getTransaction().commit();");
        w.println("            em.refresh(" + refreshVar + ");");
        w.println("            System.out.println(\"Added " + otherClass + " to " + ownerClass + ".\");");
        w.println("        } catch (RuntimeException ex) {");
        w.println("            if (em.getTransaction().isActive()) em.getTransaction().rollback();");
        w.println("            em.clear();");
        w.println("            System.out.println(\"Error: \" + ex.getMessage());");
        w.println("        }");
        w.println("    }");
    }

    // remove da lista/map
    private void generateRemoveFromListMethod(PrintWriter w, String ownerClass, String otherClass, String fieldName, Association assoc, boolean isSource) {
        String capitalized = GeneratorUtils.capitalizeFirst(fieldName);
        w.println("    private void remove" + capitalized + "() {");
        w.println("        " + idTypeOf(ownerClass) + " ownerId = InputUtils." + idReaderOf(ownerClass) + "(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " owner = " + GeneratorUtils.repoVariable(ownerClass) + ".findById(ownerId);");
        w.println("        if (owner == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        " + idTypeOf(otherClass) + " otherId = InputUtils." + idReaderOf(otherClass) + "(scanner, \"" + otherClass + " ID: \");");
        w.println("        " + otherClass + " other = " + GeneratorUtils.repoVariable(otherClass) + ".findById(otherId);");
        w.println("        if (other == null) { System.out.println(\"" + otherClass + " not found.\"); return; }");
        w.println();

        // mesmo coisa do add a remoção só persiste pelo lado DONO
        boolean isOwner = GeneratorUtils.isOwningSide(assoc, isSource);
        String saveVar, saveRepo, refreshVar, saveClass;

        if (assoc.hasQualifier()) {
            String qualCap = GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            w.println("        owner.get" + capitalized + "().remove(other.get" + qualCap + "());");
            saveVar = "owner"; saveRepo = GeneratorUtils.repoVariable(ownerClass); refreshVar = "other"; saveClass = ownerClass;
        } else if (isOwner) {
            w.println("        owner.get" + capitalized + "().remove(other);");
            saveVar = "owner"; saveRepo = GeneratorUtils.repoVariable(ownerClass); refreshVar = "other"; saveClass = ownerClass;
        } else {
            // lado inverso ent remover pelo lado DONO (em "other")
            String ownerCap = GeneratorUtils.capitalizeFirst(
                    GeneratorUtils.getOtherSideAttributeName(assoc, isSource ? "target" : "source"));
            boolean ownerIsCollection = GeneratorUtils.checkMultiplicity(
                    isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity());
            // escrever só o lado DONO, o em.refresh(owner) no fim recarrega o lado inverso da BD
            w.println(ownerIsCollection
                    ? "        other.get" + ownerCap + "().remove(owner);   // lado DONO: é o que persiste"
                    : "        other.set" + ownerCap + "(null);   // lado DONO: é o que persiste");
            saveVar = "other"; saveRepo = GeneratorUtils.repoVariable(otherClass); refreshVar = "owner"; saveClass = otherClass;
        }

        emitRelationValidation(w, saveClass, saveVar);
        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + saveRepo + ".save(" + saveVar + ");");
        w.println("            em.getTransaction().commit();");
        w.println("            em.refresh(" + refreshVar + ");");
        w.println("            System.out.println(\"" + otherClass + " removed from " + ownerClass + ".\");");
        w.println("        } catch (RuntimeException ex) {");
        w.println("            if (em.getTransaction().isActive()) em.getTransaction().rollback();");
        w.println("            em.clear();");
        w.println("            System.out.println(\"Error: \" + ex.getMessage());");
        w.println("        }");
        w.println("    }");
    }

    /**
     * opções para operações custom
     * pede owner por ID, lê cada parâmetro
     * invoca owner.op(args) e grava.
     */
    private void generateCustomOperationMethods(PrintWriter w, Class cls, List<Class> allClasses) {
        Set<String> classNames = new HashSet<>();
        for (Class c : allClasses) classNames.add(c.getName());

        for (Operation op : GeneratorUtils.effectiveOperations(cls)) {
            Interaction interaction = op.getSequenceDiagram();
            if (interaction == null) continue;

            // usar a assinatura do entry message do sequence diagram
            // (e não do class diagram) para ficar consistente
            Message entry = OperationGenerator.findEntryMessage(interaction);
            if (entry == null) continue;

            w.println("    private void " + op.getName() + "() {");
            w.println("        " + idTypeOf(cls.getName()) + " ownerId = InputUtils." + idReaderOf(cls.getName()) + "(scanner, \"" + cls.getName() + " ID: \");");
            w.println("        " + cls.getName() + " owner = " + GeneratorUtils.repoVariable(cls.getName()) + ".findById(ownerId);");
            w.println("        if (owner == null) { System.out.println(\"" + cls.getName() + " not found.\"); return; }");
            w.println();

            List<String> argNames = new ArrayList<>();
            for (MessageArgument p : entry.getArguments()) {
                String pName = p.getName();
                String pType = p.getType();
                argNames.add(pName);

                if (classNames.contains(pType)) {
                    // entidade: pedir ID e procurar no repo
                    w.println("        " + idTypeOf(pType) + " " + pName + "Id = InputUtils." + idReaderOf(pType) + "(scanner, \"" + pType + " ID (" + pName + "): \");");
                    w.println("        " + pType + " " + pName + " = " + GeneratorUtils.repoVariable(pType) + ".findById(" + pName + "Id);");
                    w.println("        if (" + pName + " == null) { System.out.println(\"" + pType + " not found.\"); return; }");
                } else {
                    // primitivo: input direto
                    Attribute tmp = new Attribute();
                    tmp.setName(pName);
                    tmp.setType(pType);
                    generateReadInputForAttribute(w, tmp);
                }
                w.println();
            }

            w.println("        em.getTransaction().begin();");
            w.println("        try {");
            w.print  ("            owner." + op.getName() + "(");
            for (int i = 0; i < argNames.size(); i++) {
                if (i > 0) w.print(", ");
                w.print(argNames.get(i));
            }
            w.println(");");
            w.println("            " + GeneratorUtils.repoVariable(cls.getName()) + ".save(owner);");
            w.println("            em.getTransaction().commit();");
            w.println("            System.out.println(\"" + op.getName() + " executed.\");");
            w.println("        } catch (RuntimeException ex) {");
            w.println("            if (em.getTransaction().isActive()) em.getTransaction().rollback();");
            w.println("            em.clear();");
            w.println("            System.out.println(\"Error: \" + ex.getMessage());");
            w.println("        }");
            w.println("    }");
        }
    }

    /***
     * MAIN MENU
     *  Cria todas as instâncias de repositórios e delega para os sub-menus.
     */

    private void generateMainMenu(List<Class> classes, List<Association> associations, String menusDir) throws IOException {
        String fileName = menusDir + "/MainMenu.java";

        try (PrintWriter w = new PrintWriter(fileName)) {
            w.println("package " + basePackage + ".menus;");
            w.println();
            w.println("import " + basePackage + ".util.InputUtils;");
            w.println("import jakarta.persistence.EntityManager;");
            w.println("import java.util.*;");
            w.println("import java.time.*;");
            w.println();
            w.println("public class MainMenu {");
            w.println();
            w.println("    private final Scanner scanner = new Scanner(System.in);");
            w.println("    private final EntityManager em;");
            w.println();
            w.println("    public MainMenu(EntityManager em) {");
            w.println("        this.em = em;");
            w.println("    }");
            w.println();
            w.println("    public void run() {");
            w.println("        while (true) {");
            w.println("            System.out.println(\"\\n=== Main Menu ===\");");
            for (int i = 0; i < classes.size(); i++) {
                w.println("            System.out.println(\"" + (i + 1) + ". " + classes.get(i).getName() + " Menu\");");
            }
            w.println("            System.out.println(\"0. Exit\");");
            w.println();
            w.println("            int option = InputUtils.readInt(scanner, \"Option: \");");
            w.println("            switch (option) {");
            for (int i = 0; i < classes.size(); i++) {
                Class cls = classes.get(i);
                w.println("                case " + (i + 1) + ": new " + cls.getName() + "Menu(scanner, em).run(); break;");
            }
            w.println("                case 0: System.out.println(\"Bye!\"); return;");
            w.println("                default: System.out.println(\"Invalid option.\");");
            w.println("            }");
            w.println("        }");
            w.println("    }");
            w.println("}");
        }
    }

    /***
     * UTILS HERE
     */
    private void generateReadInputForAttribute(PrintWriter w, Attribute attr) {
        String name = attr.getName();

        switch (attr.getType()) {
            case "String":
                w.println("        System.out.print(\"" + name + ": \");");
                w.println("        String " + name + " = scanner.nextLine();");
                break;
            case "int":
            case "Integer":
                w.println("        int " + name + " = InputUtils.readInt(scanner, \"" + name + ": \");");
                break;
            case "long":
            case "Long":
                w.println("        long " + name + " = InputUtils.readLong(scanner, \"" + name + ": \");");
                break;
            case "float":
            case "Float":
                w.println("        float " + name + " = (float) InputUtils.readDouble(scanner, \"" + name + ": \");");
                break;
            case "double":
            case "Double":
                w.println("        double " + name + " = InputUtils.readDouble(scanner, \"" + name + ": \");");
                break;
            case "boolean":
            case "Boolean":
                w.println("        System.out.print(\"" + name + " (true/false): \");");
                w.println("        boolean " + name + " = Boolean.parseBoolean(scanner.nextLine());");
                break;
            default:
                if (enumNames.contains(attr.getType())) {
                    w.println("        " + attr.getType() + " " + name + " = null;");
                    w.println("        while (" + name + " == null) {");
                    w.println("            System.out.print(\"" + name + " \" + java.util.Arrays.toString(" + attr.getType() + ".values()) + \": \");");
                    w.println("            try { " + name + " = " + attr.getType() + ".valueOf(scanner.nextLine().trim().toUpperCase()); }");
                    w.println("            catch (IllegalArgumentException ex) { System.out.println(\"Invalid value. Try again.\"); }");
                    w.println("        }");
                } else if (isTimeType(attr.getType())) {
                    // mesmo padrão de retry do enum, com parse de data
                    String type = attr.getType();
                    w.println("        " + type + " " + name + " = null;");
                    w.println("        while (" + name + " == null) {");
                    w.println("            System.out.print(\"" + name + " (ex: " + timeHint(type) + "): \");");
                    w.println("            try { " + name + " = " + type + ".parse(scanner.nextLine().trim()); }");
                    w.println("            catch (Exception ex) { System.out.println(\"Invalid value. Try again.\"); }");
                    w.println("        }");
                } else {
                    throw new IllegalArgumentException("Atributo '" + name + "' do tipo '" + attr.getType()
                            + "': tipo não suportado no target CLI.");
                }
        }
    }

    /**
     * Gera código para atualizar um atributo .
     * mostra o valor atual, se user clicar enter mantém o valor, senão atualiza
     */
    private void generateUpdateAttribute(PrintWriter w, Attribute attr) {
        String name = attr.getName();
        String capitalized = GeneratorUtils.capitalizeFirst(name);

        switch (attr.getType()) {
            case "String":
                w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() +" +
                        "\"]: \");");
                w.println("        String " + name + " = scanner.nextLine();");
                w.println("        if (!" + name + ".isEmpty()) entity.set" + capitalized + "(" + name +
                        ");");
                break;
            case "int":
            case "Integer":
                w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() +" +
                        "\"]: \");");
                w.println("        String " + name + "Str = scanner.nextLine();");
                w.println("        if (!" + name + "Str.isEmpty()) entity.set" + capitalized +
                        "(Integer.parseInt(" + name + "Str));");
                break;
            case "long":
            case "Long":
                w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() +" +
                        "\"]: \");");
                w.println("        String " + name + "Str = scanner.nextLine();");
                w.println("        if (!" + name + "Str.isEmpty()) entity.set" + capitalized +
                        "(Long.parseLong(" + name + "Str));");
                break;
            case "float":
            case "Float":
                w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() +" +
                        "\"]: \");");
                w.println("        String " + name + "Str = scanner.nextLine();");
                w.println("        if (!" + name + "Str.isEmpty()) entity.set" + capitalized +
                        "(Float.parseFloat(" + name + "Str));");
                break;
            case "double":
            case "Double":
                w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() +" +
                        "\"]: \");");
                w.println("        String " + name + "Str = scanner.nextLine();");
                w.println("        if (!" + name + "Str.isEmpty()) entity.set" + capitalized +
                        "(Double.parseDouble(" + name + "Str));");
                break;
            case "boolean":
            case "Boolean":
                w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() +" +
                        "\"]: \");");
                w.println("        String " + name + "Str = scanner.nextLine();");
                w.println("        if (!" + name + "Str.isEmpty()) entity.set" + capitalized +
                        "(Boolean.parseBoolean(" + name + "Str));");
                break;
            default:
                if (enumNames.contains(attr.getType())) {
                    w.println("        while (true) {");
                    w.println("            System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() + \"] \" + java.util.Arrays.toString(" + attr.getType() + ".values()) + \": \");");
                    w.println("            String " + name + "Str = scanner.nextLine();");
                    w.println("            if (" + name + "Str.isEmpty()) break;");
                    w.println("            try { entity.set" + capitalized + "(" + attr.getType() + ".valueOf(" + name + "Str.trim().toUpperCase())); break; }");
                    w.println("            catch (IllegalArgumentException ex) { System.out.println(\"Invalid value. Try again.\"); }");
                    w.println("        }");
                } else if (isTimeType(attr.getType())) {
                    // enter mantém; valor inválido mantém com aviso
                    String type = attr.getType();
                    w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() + \"] (ex: " + timeHint(type) + "): \");");
                    w.println("        String " + name + "Str = scanner.nextLine();");
                    w.println("        if (!" + name + "Str.isEmpty()) {");
                    w.println("            try { entity.set" + capitalized + "(" + type + ".parse(" + name + "Str.trim())); }");
                    w.println("            catch (Exception ex) { System.out.println(\"Invalid value, keeping current.\"); }");
                    w.println("        }");
                } else {
                    throw new IllegalArgumentException("Atributo '" + name + "' do tipo '" + attr.getType()
                            + "': tipo não suportado no target CLI.");
                }
                break;
        }
    }

    // tipos java.time suportados nos menus
    private static boolean isTimeType(String type) {
        return type.equals("LocalDate") || type.equals("LocalDateTime") || type.equals("LocalTime");
    }

    // mostar exemplo de formato para o user
    private static String timeHint(String type) {
        switch (type) {
            case "LocalDate":     return "2026-01-31";
            case "LocalDateTime": return "2026-01-31T10:30";
            default:              return "10:30";   // LocalTime
        }
    }



}
