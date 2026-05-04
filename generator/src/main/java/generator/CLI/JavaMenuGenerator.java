package generator.CLI;

import model.Association;
import model.Attribute;
import model.Class;
import model.Enumeration;
import model.Operation;
import model.Parameter;
import model.sequence.*;
import generator.OperationGenerator;

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
 */
public class JavaMenuGenerator {

    private String baseOutputPath;
    private Set<String> enumNames = new HashSet<>();
    private List<Interaction> interactions;

    public JavaMenuGenerator(String baseOutputPath, List<Interaction> interactions) {
        this.baseOutputPath = baseOutputPath;
        this.interactions = interactions;
    }

    /**
     * entrada: gera todos os menus (um por entidade + MainMenu)
     */
    public void generateMenus(List<Class> classes, List<Association> associations) throws IOException {
        generateMenus(classes, associations, new ArrayList<>());
    }

    public void generateMenus(List<Class> classes, List<Association> associations, List<Enumeration> enumerations) throws IOException {
        for (Enumeration e : enumerations) {
            enumNames.add(e.getName());
        }
        System.out.println("Generating Java Menus...");

        String menusPath = baseOutputPath + "/src/menus";
        new File(menusPath).mkdirs();

        for (Class cls : classes ){
            generateEntityMenu(cls, classes, associations, menusPath);
        }

        generateMainMenu(classes, associations, menusPath);
        System.out.println("Java menu generation completed.");
    }

    /*
    * gerar menu para cada entidade
     */
    private void generateEntityMenu(Class cls, List<Class> allClasses, List<Association> associations, String menusDir) throws IOException {
        String className = cls.getName() + "Menu";
        String filename = menusDir + "/" + className + ".java";

        // verificar que repos este menu precisa

        Set<String> neededRepos = getNeededRepositories(cls, associations, allClasses);

        try (PrintWriter w = new PrintWriter(filename)) {
            w.println("package menus;");
            w.println();
            w.println("import models.*;");
            w.println("import repository.*;");
            w.println("import util.InputUtils;");
            w.println("import java.util.*;");
            w.println();
            w.println("public class " + className + " {");
            w.println();

            // fields
            w.println("    private Scanner scanner;");
            w.println("    private " + cls.getName() + "Repository " + repoVariable(cls.getName()) + ";");
            for (String related : neededRepos) {
                w.println("    private " + related + "Repository " + repoVariable(related) + ";");
            }
            w.println();

            // constructor
            generateMenuConstructor(w, cls, neededRepos);

            // run(): loop principal do menu com switch
            generateRunMethod(w, cls, associations);

            // CRUD
            generateCreateMethod(w, cls);
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
        w.print("    public " + cls.getName() + "Menu(Scanner scanner, ");
        w.print(cls.getName() + "Repository " + repoVariable(cls.getName()));

        for (String related : neededRepos) {
            w.print(", " + related + "Repository " + repoVariable(related));
        }
        w.println(") {");
        w.println("        this.scanner = scanner;");
        w.println("        this." + repoVariable(cls.getName()) + " = " + repoVariable(cls.getName()) + ";");
        for (String related : neededRepos) {
            w.println("        this." + repoVariable(related) + " = " + repoVariable(related) + ";");
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
        options.add(new String[]{"Create", "create"});
        options.add(new String[]{"View All", "viewAll"});
        options.add(new String[]{"View by ID", "viewById"});
        options.add(new String[]{"Update", "update"});
        options.add(new String[]{"Delete", "delete"});

        // adicionar das relações
        for (Association assoc : associations) {
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                String other = assoc.getTargetClass();
                String fieldName = makeFieldName(other, assoc.getTargetRole(), assoc.getTargetMultiplicity());

                if (isMultiple(assoc.getTargetMultiplicity()) || assoc.hasQualifier()) {
                    // OneToMany, ManyToMany ou Map: Add e Remove
                    options.add(new String[]{"Add " + other, "add" + other});
                    options.add(new String[]{"Remove " + other, "remove" + other});
                }
                else {
                    // ManyToOne: Assign e Remove
                    options.add(new String[]{"Assign " + other, "assign" + other});
                    options.add(new String[]{"Remove " + other, "remove" + other});
                }
            }
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                String other = assoc.getSourceClass();
                String fieldName = makeFieldName(other, assoc.getSourceRole(), assoc.getSourceMultiplicity());

                if (isMultiple(assoc.getSourceMultiplicity()) || assoc.hasQualifier()) {
                    options.add(new String[]{"Add " + other, "add" + other});
                    options.add(new String[]{"Remove " + other, "remove" + other});
                }
                else {
                    options.add(new String[]{"Assign " + other, "assign" + other});
                    options.add(new String[]{"Remove " + other, "remove" + other});
                }
            }
        }

        for (Operation op : cls.getOperations()) {
            Interaction interaction = OperationGenerator.findInteractionForOperation(cls, op, interactions);
            if (interaction != null) {
                String label = op.getName().substring(0, 1).toUpperCase() + op.getName().substring(1);
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
        List<Attribute> allAttrs = new ArrayList<>();
        if (cls.getParentClass() != null) {
            allAttrs.addAll(cls.getParentClass().getAttributes().stream()
                    .filter(a -> !a.isDerived() && !a.hasInit())
                    .collect(Collectors.toList()));
        }
        allAttrs.addAll(cls.getAttributes().stream()
                .filter(a -> !a.isDerived() && !a.hasInit())
                .collect(Collectors.toList()));

        // ler cada atributo
        for (Attribute attr : allAttrs) {
            generateReadInputForAttribute(w, attr);
        }
        w.println();

        // criar entidade com construtor (pai + próprios)
        w.print("        " + cls.getName() + " entity = new " + cls.getName() + "(");
        for (int i = 0; i < allAttrs.size(); i++) {
            if (i > 0) w.print(", ");
            w.print(allAttrs.get(i).getName());
        }
        w.println(");");

        // validation
        if (!cls.getInvariants().isEmpty()) {
            w.println();
            w.println("        List<String> errors = entity.validate();");
            w.println("        if (!errors.isEmpty()) {");
            w.println("            System.out.println(\"Validation errors:\");");
            w.println("            errors.forEach(e -> System.out.println(\"  - \" + e));");
            w.println("            return;");
            w.println("        }");
        }

        w.println();
        w.println("        " + repoVariable(cls.getName()) + ".save(entity);");
        w.println("        System.out.println(\"Created: \" + entity);");
        w.println("    }");
        w.println();
    }

    /**
     * viewAll(): lista todas as entidades do repositório.
     */
    private void generateViewAllMethod(PrintWriter w, Class cls, List<Association> associations) {
        w.println("    private void viewAll() {");
        w.println("        List<" + cls.getName() + "> all = " + repoVariable(cls.getName()) + ".findAll();");
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
        w.println("        Long id = InputUtils.readLong(scanner, \"ID: \");");
        w.println("        " + cls.getName() + " entity = " + repoVariable(cls.getName()) + ".findById(id);");
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
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                other = assoc.getTargetClass();
                role = assoc.getTargetRole();
                mult = assoc.getTargetMultiplicity();
            } else if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                other = assoc.getSourceClass();
                role = assoc.getSourceRole();
                mult = assoc.getSourceMultiplicity();
            }
            if (other == null) continue;

            String fieldName = makeFieldName(other, role, mult);
            String cap = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            String getter = "entity.get" + cap + "()";

            if (assoc.hasQualifier()) {
                w.println(indent + "System.out.println(\"  " + fieldName + ": \" + " + getter + ".values());");
            } else if (isMultiple(mult)) {
                w.println(indent + "System.out.println(\"  " + fieldName + ": \" + " + getter + ");");
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
        w.println("        Long id = InputUtils.readLong(scanner, \"ID: \");");
        w.println("        " + cls.getName() + " entity = " + repoVariable(cls.getName()) + ".findById(id);");
        w.println("        if (entity == null) {");
        w.println("            System.out.println(\"Not found.\");");
        w.println("            return;");
        w.println("        }");
        w.println();
        w.println("        System.out.println(\"Current: \" + entity);");
        w.println("        System.out.println(\"(press Enter to keep current value)\");");
        w.println();

        // para cada atributo (pai + próprios) ler novo valor ou manter atual
        List<Attribute> allAttrs = new ArrayList<>();
        if (cls.getParentClass() != null) {
            allAttrs.addAll(cls.getParentClass().getAttributes().stream()
                    .filter(a -> !a.isDerived() && !a.hasInit())
                    .collect(Collectors.toList()));
        }
        allAttrs.addAll(cls.getAttributes().stream()
                .filter(a -> !a.isDerived() && !a.hasInit())
                .collect(Collectors.toList()));
        for (Attribute attr : allAttrs) {
            generateUpdateAttribute(w, attr);
        }

        // validation
        if (!cls.getInvariants().isEmpty()) {
            w.println();
            w.println("        List<String> errors = entity.validate();");
            w.println("        if (!errors.isEmpty()) {");
            w.println("            System.out.println(\"Validation errors:\");");
            w.println("            errors.forEach(e -> System.out.println(\"  - \" + e));");
            w.println("            return;");
            w.println("        }");
        }

        w.println();
        w.println("        " + repoVariable(cls.getName()) + ".save(entity);");
        w.println("        System.out.println(\"Updated: \" + entity);");
        w.println("    }");
        w.println();
    }

    /**
     *  delete(): pede ID e remove do repositório.
     */
    private void generateDeleteMethod(PrintWriter w, Class cls) {
        w.println("    private void delete() {");
        w.println("        Long id = InputUtils.readLong(scanner, \"ID: \");");
        w.println("        if (" + repoVariable(cls.getName()) + ".deleteById(id)) {");
        w.println("            System.out.println(\"Deleted.\");");
        w.println("        } else {");
        w.println("            System.out.println(\"Not found.\");");
        w.println("        }");
        w.println("    }");
        w.println();
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
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                String other = assoc.getTargetClass();
                String fieldName = makeFieldName(other, assoc.getTargetRole(), assoc.getTargetMultiplicity());

                if (isMultiple(assoc.getTargetMultiplicity()) || assoc.hasQualifier()) {
                    generateAddMethod(w, cls.getName(), other, fieldName, assoc);
                    generateRemoveFromListMethod(w, cls.getName(), other, fieldName, assoc);
                }
                else {
                    generateAssignMethod(w, cls.getName(), other, fieldName);
                    generateRemoveAssignMethod(w, cls.getName(), other, fieldName);
                }
            }
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                String other = assoc.getSourceClass();
                String fieldName = makeFieldName(other, assoc.getSourceRole(), assoc.getSourceMultiplicity());

                if (isMultiple(assoc.getSourceMultiplicity()) || assoc.hasQualifier()) {
                    generateAddMethod(w, cls.getName(), other, fieldName, assoc);
                    generateRemoveFromListMethod(w, cls.getName(), other, fieldName, assoc);
                }
                else {
                    generateAssignMethod(w, cls.getName(), other, fieldName);
                    generateRemoveAssignMethod(w, cls.getName(), other, fieldName);
                }
            }
        }
    }

    // Assign method (ManyToOne): entity.setOther(other)
    private void generateAssignMethod(PrintWriter w, String ownerClass, String otherClass, String fieldName) {
        String capitalized = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        w.println("    private void assign" + otherClass + "() {");
        w.println("        Long ownerId = InputUtils.readLong(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " owner = " + repoVariable(ownerClass) + ".findById(ownerId);");
        w.println("        if (owner == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        Long otherId = InputUtils.readLong(scanner, \"" + otherClass + " ID: \");");
        w.println("        " + otherClass + " other = " + repoVariable(otherClass) + ".findById(otherId);");
        w.println("        if (other == null) { System.out.println(\"" + otherClass + " not found.\"); return; }");
        w.println();
        w.println("        owner.set" + capitalized + "(other);");
        w.println("        " + repoVariable(ownerClass) + ".save(owner);");
        w.println("        System.out.println(\"Assigned " + otherClass + " to " + ownerClass + ".\");");
        w.println("    }");
        w.println();
    }

    // remove assign
    private void generateRemoveAssignMethod(PrintWriter w, String ownerClass, String otherClass, String fieldName) {
        String capitalized = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        w.println("    private void remove" + otherClass + "() {");
        w.println("        Long id = InputUtils.readLong(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " entity = " + repoVariable(ownerClass) + ".findById(id);");
        w.println("        if (entity == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        entity.set" + capitalized + "(null);");
        w.println("        System.out.println(\"" + otherClass + " removed from \" + entity);");
        w.println("    }");
        w.println();
    }

    // add (OneToMany/ManyToMany/Map): list.add(other) ou map.put(key, other)
    private void generateAddMethod(PrintWriter w, String ownerClass, String otherClass, String fieldName, Association assoc) {
        String capitalized = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        w.println("    private void add" + otherClass + "() {");
        w.println("        Long ownerId = InputUtils.readLong(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " owner = " + repoVariable(ownerClass) + ".findById(ownerId);");
        w.println("        if (owner == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        Long otherId = InputUtils.readLong(scanner, \"" + otherClass + " ID: \");");
        w.println("        " + otherClass + " other = " + repoVariable(otherClass) + ".findById(otherId);");
        w.println("        if (other == null) { System.out.println(\"" + otherClass + " not found.\"); return; }");
        w.println();

        if (assoc.hasQualifier()) {
            // Map fazemos put com a chave sendo o qualifier do outro objeto
            // owner.getAlunos().put(other.getNumero(), other)
            String qualifierName = assoc.getQualifierName();
            String qualCap = qualifierName.substring(0, 1).toUpperCase() + qualifierName.substring(1);
            w.println("        owner.get" + capitalized + "().put(other.get" + qualCap + "(), other);");
        } else {
            w.println("        owner.get" + capitalized + "().add(other);");
        }

        w.println("        " + repoVariable(ownerClass) + ".save(owner);");
        w.println("        System.out.println(\"Added " + otherClass + " to " + ownerClass + ".\");");
        w.println("    }");
        w.println();
    }

    // remove da lista/map
    private void generateRemoveFromListMethod(PrintWriter w, String ownerClass, String otherClass, String fieldName, Association assoc) {
        String capitalized = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        w.println("    private void remove" + otherClass + "() {");
        w.println("        Long ownerId = InputUtils.readLong(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " owner = " + repoVariable(ownerClass) + ".findById(ownerId);");
        w.println("        if (owner == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        Long otherId = InputUtils.readLong(scanner, \"" + otherClass + " ID: \");");
        w.println("        " + otherClass + " other = " + repoVariable(otherClass) + ".findById(otherId);");
        w.println("        if (other == null) { System.out.println(\"" + otherClass + " not found.\"); return; }");
        w.println();

        if (assoc.hasQualifier()) {
            // map remove pela chave
            String qualifierName = assoc.getQualifierName();
            String qualCap = qualifierName.substring(0, 1).toUpperCase() + qualifierName.substring(1);
            w.println("        owner.get" + capitalized + "().remove(other.get" + qualCap + "());");
        } else {
            w.println("        owner.get" + capitalized + "().remove(other);");
        }

        w.println("        System.out.println(\"" + otherClass + " removed from \" + owner);");
        w.println("    }");
        w.println();
    }

    /**
     * opções para operações custom
     * pede owner por ID, lê cada parâmetro
     * invoca owner.op(args) e grava.
     */
    private void generateCustomOperationMethods(PrintWriter w, Class cls, List<Class> allClasses) {
        Set<String> classNames = new HashSet<>();
        for (Class c : allClasses) classNames.add(c.getName());

        for (Operation op : cls.getOperations()) {
            Interaction interaction = OperationGenerator.findInteractionForOperation(cls, op, interactions);
            if (interaction == null) continue;

            // usar a assinatura do entry message do sequence diagram
            // (e não do class diagram) para ficar consistente
            Message entry = OperationGenerator.findEntryMessage(interaction);
            if (entry == null) continue;

            w.println("    private void " + op.getName() + "() {");
            w.println("        Long ownerId = InputUtils.readLong(scanner, \"" + cls.getName() + " ID: \");");
            w.println("        " + cls.getName() + " owner = " + repoVariable(cls.getName()) + ".findById(ownerId);");
            w.println("        if (owner == null) { System.out.println(\"" + cls.getName() + " not found.\"); return; }");
            w.println();

            List<String> argNames = new ArrayList<>();
            for (MessageArgument p : entry.getArguments()) {
                String pName = p.getName();
                String pType = p.getType();
                argNames.add(pName);

                if (classNames.contains(pType)) {
                    // entidade: pedir ID e procurar no repo
                    w.println("        Long " + pName + "Id = InputUtils.readLong(scanner, \"" + pType + " ID (" + pName + "): \");");
                    w.println("        " + pType + " " + pName + " = " + repoVariable(pType) + ".findById(" + pName + "Id);");
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

            w.print("        owner." + op.getName() + "(");
            for (int i = 0; i < argNames.size(); i++) {
                if (i > 0) w.print(", ");
                w.print(argNames.get(i));
            }
            w.println(");");
            w.println("        " + repoVariable(cls.getName()) + ".save(owner);");
            w.println("        System.out.println(\"" + op.getName() + " executed.\");");
            w.println("    }");
            w.println();
        }
    }

    /***
     * MAIN MENU
     *  Cria todas as instâncias de repositórios e delega para os sub-menus.
     */

    private void generateMainMenu(List<Class> classes, List<Association> associations, String menusDir) throws IOException {
        String fileName = menusDir + "/MainMenu.java";

        try (PrintWriter w = new PrintWriter(fileName)) {
            w.println("package menus;");
            w.println();
            w.println("import repository.*;");
            w.println("import util.InputUtils;");
            w.println("import java.util.*;");
            w.println();
            w.println("public class MainMenu {");
            w.println();
            w.println("    private Scanner scanner = new Scanner(System.in);");

            // all repositories
            for (Class cls : classes) {
                w.println("    private " + cls.getName() + "Repository " + repoVariable(cls.getName()) + " = " +
                        "new " + cls.getName() + "Repository();");
            }
            w.println();

            // run
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
                Set<String> neededRepos = getNeededRepositories(cls, associations, classes);

                w.print("                case " + (i + 1) + ": new " + cls.getName() + "Menu(scanner, " +
                        repoVariable(cls.getName()));
                for (String related : neededRepos) {
                    w.print(", " + repoVariable(related));
                }
                w.println(").run(); break;");
            }

            w.println("                case 0: System.out.println(\"Bye!\"); return;");
            w.println("                default: System.out.println(\"Invalid option.\");");
            w.println("            }");
            w.println("        }");
            w.println("    }");
            w.println();

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
                    w.println("        System.out.print(\"" + name + " \" + java.util.Arrays.toString(" + attr.getType() + ".values()) + \": \");");
                    w.println("        " + attr.getType() + " " + name + " = " + attr.getType() + ".valueOf(scanner.nextLine().trim().toUpperCase());");
                } else {
                    w.println("        System.out.print(\"" + name + ": \");");
                    w.println("        String " + name + "Str = scanner.nextLine();");
                }
        }
    }

    /**
     * Gera código para atualizar um atributo .
     * mostra o valor atual, se user clicar enter mantém o valor, senão atualiza
     */
    private void generateUpdateAttribute(PrintWriter w, Attribute attr) {
        String name = attr.getName();
        String capitalized = name.substring(0, 1).toUpperCase() + name.substring(1);

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
                    w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() + \"] \" + java.util.Arrays.toString(" + attr.getType() + ".values()) + \": \");");
                    w.println("        String " + name + "Str = scanner.nextLine();");
                    w.println("        if (!" + name + "Str.isEmpty()) entity.set" + capitalized + "(" + attr.getType() + ".valueOf(" + name + "Str.trim().toUpperCase()));");
                } else {
                    w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() +" +
                            "\"]: \");");
                    w.println("        String " + name + " = scanner.nextLine();");
                    w.println("        if (!" + name + ".isEmpty()) entity.set" + capitalized + "(" + name +
                            ");");
                }
                break;
        }
    }

    /**
     * pega no conjunto de outras classes cujos repositórios este menu precisa
     * as associadas (navegáveis) + as usadas como parâmetro de operações custom.
     */
    private Set<String> getNeededRepositories(Class cls, List<Association> associations, List<Class> allClasses) {
        Set<String> related = new LinkedHashSet<>();
        for (Association assoc : associations) {
            if (assoc.getSourceClass().equals(cls.getName()) && assoc.isTargetNavigable()) {
                related.add(assoc.getTargetClass());
            }
            if (assoc.getTargetClass().equals(cls.getName()) && assoc.isSourceNavigable()) {
                related.add(assoc.getSourceClass());
            }
        }
        for (Operation op : cls.getOperations()) {
            Interaction interaction = OperationGenerator.findInteractionForOperation(cls, op, interactions);
            if (interaction == null) continue;
            for (Parameter param : op.getParameters()) {
                String type = param.getType();
                // only include if it's a domain class (matches one of the known classes)
                for (Class other : allClasses) {
                    if (other.getName().equals(type) && !type.equals(cls.getName())) {
                        related.add(type);
                    }
                }
            }
        }
        return related;
    }

    // nome da variável do repositório: classe com letra minúscula + "Repository"
    private String repoVariable(String className) {
        return className.substring(0, 1).toLowerCase() + className.substring(1) + "Repository";
    }

    private String makeFieldName(String className, String role, String multiplicity) {
        if (role != null && !role.trim().isEmpty()) {
            return role.trim();
        }
        String base = className.substring(0, 1).toLowerCase() + className.substring(1);
        return isMultiple(multiplicity) ? base + "s" : base;
    }

    private boolean isMultiple(String multiplicity) {
        if (multiplicity == null) return false;
        return multiplicity.contains("*") ||
                (multiplicity.contains("..") && !multiplicity.equals("0..1"));
    }
}
