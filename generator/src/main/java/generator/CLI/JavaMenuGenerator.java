package generator.CLI;

import model.Association;
import model.Attribute;
import model.Class;
import model.Enumeration;
import model.Operation;
import model.Parameter;
import model.sequence.*;
import generator.GeneratorUtils;
import generator.OperationGenerator;
import generator.RelationshipInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * gera os menus cli.
 * para cada entidade cria CRUD + opções de associação (add/remove | assign/remove) + operações custom
 * MainMenu que cria os repos e delega para os sub-menus.
 *
 * nota:
 *  - sem persistência, repos são Map em mem, ids sempre Long
 *  - MainMenu cria uma instância de cada repo e partilha-a, senao cada menu teria o seu map e dados nao partilhavam.
 *  - os saves sao redundantes porque os objetos ja sao passados
 *
 * limitações:
 *  - sem transações logo se uma op falha a meio, as mudanças à entidade não são revertidas
 */
public class JavaMenuGenerator {

    private String baseOutputPath;
    private Set<String> enumNames = new HashSet<>();

    private String basePackage;

    public JavaMenuGenerator(String baseOutputPath, String basePackage) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
    }

    /**
     * gera todos os menus (um por entidade + MainMenu)
     */
    public void generateMenus(List<Class> classes, List<Association> associations, List<Enumeration> enumerations) throws IOException {
        enumNames.clear();
        for (Enumeration e : enumerations) {
            enumNames.add(e.getName());
        }
        System.out.println("Generating Java Menus...");

        String menusPath = baseOutputPath + "/src/" + basePackage.replace('.', '/') + "/menus";
        new File(menusPath).mkdirs();

        List<Class> classesToBuild = classes.stream()
                .filter(c -> !c.isAbstract())
                .toList();

        for (Class cls : classesToBuild) {
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

        Set<String> neededRepos = GeneratorUtils.getNeededRepositories(cls, associations, allClasses);

        try (PrintWriter w = new PrintWriter(filename)) {
            w.println("package " + basePackage + ".menus;");
            w.println();
            w.println("import " + basePackage + ".models.*;");
            w.println("import " + basePackage + ".repository.*;");
            w.println("import " + basePackage + ".util.InputUtils;");
            w.println("import java.util.*;");
            w.println("import java.time.*;");
            w.println();
            w.println("public class " + className + " {");
            w.println();

            // attributes
            w.println("    private Scanner scanner;");
            w.println("    private " + cls.getName() + "Repository " + GeneratorUtils.repoVariable(cls.getName()) + ";");
            for (String related : neededRepos) {
                w.println("    private " + related + "Repository " + GeneratorUtils.repoVariable(related) + ";");
            }
            w.println();

            // constructor
            generateMenuConstructor(w, cls, neededRepos);

            // run() loop principal do menu com switch
            generateRunMethod(w, cls, associations);

            // CRUD
            if (!cls.isAbstract()) generateCreateMethod(w, cls, associations);
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
     * gera construtor do menu
     */
    private void generateMenuConstructor(PrintWriter w, Class cls, Set<String> neededRepos) {
        w.print("    public " + cls.getName() + "Menu(Scanner scanner, ");
        w.print(cls.getName() + "Repository " + GeneratorUtils.repoVariable(cls.getName()));

        for (String related : neededRepos) {
            w.print(", " + related + "Repository " + GeneratorUtils.repoVariable(related));
        }
        w.println(") {");
        w.println("        this.scanner = scanner;");
        w.println("        this." + GeneratorUtils.repoVariable(cls.getName()) + " = " + GeneratorUtils.repoVariable(cls.getName()) + ";");
        for (String related : neededRepos) {
            w.println("        this." + GeneratorUtils.repoVariable(related) + " = " + GeneratorUtils.repoVariable(related) + ";");
        }
        w.println("    }");
        w.println();
    }

    /**
     * gera metodo run() (loop do menu)
     * crud (create, view all , view by id, update, delete)
     * depois as de associação
     *   - Multiplicidade * (OneToMany/ManyToMany): Add + Remove
     *   - Multiplicidade 1 (ManyToOne/OneToOne): Assign + Remove
     */
    private void generateRunMethod(PrintWriter w, Class cls, List<Association> associations) {
        // menu options
        // [0] = label para o menu, [1] = nome do metodo a chamar
        List<String[]> options = new ArrayList<>();
        if (!cls.isAbstract()) options.add(new String[]{"Create", "create"});
        options.add(new String[]{"View All", "viewAll"});
        options.add(new String[]{"View by ID", "viewById"});
        options.add(new String[]{"Update", "update"});
        options.add(new String[]{"Delete", "delete"});

        // adicionar das relações
        for (RelationshipInfo rel : RelationshipInfo.collectForMenus(cls, associations)) {
            String attributeName = rel.attributeName();
            String cap = GeneratorUtils.capitalizeFirst(attributeName);

            if (rel.isCollection()) {
                // OneToMany, ManyToMany ou Map: Add e Remove
                options.add(new String[]{"Add " + attributeName, "add" + cap}); //  { "Add turmas , "addTurmas" }
                options.add(new String[]{"Remove " + attributeName, "remove" + cap});
            } else {
                // ManyToOne/OneToOne: Assign e Remove
                options.add(new String[]{"Assign " + attributeName, "assign" + cap});
                options.add(new String[]{"Remove " + attributeName, "remove" + cap});
            }
        }

        // inclui ops herdadas de pai abstrato (senao a subclasse nao as tinha no menu)
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
    private void generateCreateMethod(PrintWriter w, Class cls, List<Association> associations) {
        w.println("    private void create() {");

        // recolher atributos do pai + próprios
        List<Attribute> allAttrs = new ArrayList<>();

        // atributos herdados
        allAttrs.addAll(GeneratorUtils.ancestorAttributes(cls).stream()
                .filter(a -> !a.hasDerive() && !a.hasInit() && !"id".equals(a.getName()))
                .toList());
        // atributos próprios da classe
        allAttrs.addAll(cls.getAttributes().stream()
                .filter(a -> !a.hasDerive() && !a.hasInit() && !"id".equals(a.getName()))
                .toList());

        // ler cada atributo
        for (Attribute attr : allAttrs) {
            generateReadInputForAttribute(w, attr);
        }
        w.println();

        // pede o id de cada relação single antes do validate(): senão uma relação obrigatória
        // (self.x <> null) nunca poderia ser preenchida e a classe ficava incriável. Vazio = null.
        List<RelationshipInfo> singleRels = RelationshipInfo.collectForMenus(cls, associations).stream()
                .filter(r -> !r.isCollection())
                .toList();
        for (RelationshipInfo rel : singleRels) {
            String other = rel.otherClass;
            String attribute = rel.attributeName();
            String in = attribute + "IdIn";
            w.println("        System.out.print(\"" + other + " ID para " + attribute + " (enter = nenhum): \");");
            w.println("        String " + in + " = scanner.nextLine().trim();");
            w.println("        " + other + " " + attribute + " = null;");
            w.println("        if (!" + in + ".isEmpty()) {");
            w.println("            try { " + attribute + " = " + GeneratorUtils.repoVariable(other)
                    + ".findById(Long.valueOf(" + in + ")); }");
            w.println("            catch (RuntimeException ex) { System.out.println(\"Invalid id.\"); return; }");
            w.println("            if (" + attribute + " == null) { System.out.println(\"" + other + " not found.\"); return; }");
            w.println("        }");
            w.println();
        }

        // criar entidade com construtor (pai + próprios)
        w.print("        " + cls.getName() + " entity = new " + cls.getName() + "(");
        for (int i = 0; i < allAttrs.size(); i++) {
            if (i > 0) w.print(", ");
            w.print(allAttrs.get(i).getName());
        }
        w.println(");");

        // liga as relações single no novo objeto: set do lado owner (+ inverso se bidirecional)
        for (RelationshipInfo rel : singleRels) {
            String var = rel.attributeName();
            w.println("        entity.set" + GeneratorUtils.capitalizeFirst(var) + "(" + var + ");");
            if (rel.isBidirectional()) {
                w.println("        if (" + var + " != null) {");
                writeInverseLink(w, rel, var, "entity", "            ");
                w.println("        }");
            }
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
        w.println("        " + GeneratorUtils.repoVariable(cls.getName()) + ".save(entity);");
        w.println("        System.out.println(\"Created: \" + entity);");
        w.println("    }");
        w.println();
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
        w.println("        Long id = InputUtils.readLong(scanner, \"ID: \");");
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
        for (RelationshipInfo rel : RelationshipInfo.collectForMenus(cls, associations)) {
            String attributeName = rel.attributeName();
            String cap = GeneratorUtils.capitalizeFirst(attributeName);
            String getter = "entity.get" + cap + "()";

            // .values() só se o campo desta ponta for mesmo um Map
            if (rel.isMap()) {
                w.println(indent + "System.out.println(\"  " + attributeName + ": \" + " + getter + ".values());");
            } else {
                w.println(indent + "System.out.println(\"  " + attributeName + ": \" + " + getter + ");");
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
        List<Attribute> allAttrs = new ArrayList<>();
        // atributos herdados
        allAttrs.addAll(GeneratorUtils.ancestorAttributes(cls).stream()
                .filter(a -> !a.hasDerive() && !"id".equals(a.getName()))
                .toList());
        allAttrs.addAll(cls.getAttributes().stream()
                .filter(a -> !a.hasDerive() && !"id".equals(a.getName()))
                .toList());
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
        w.println("        " + GeneratorUtils.repoVariable(cls.getName()) + ".save(entity);");
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
        w.println("        if (" + GeneratorUtils.repoVariable(cls.getName()) + ".deleteById(id)) {");
        w.println("            System.out.println(\"Deleted.\");");
        w.println("        } else {");
        w.println("            System.out.println(\"Not found.\");");
        w.println("        }");
        w.println("    }");
        w.println();
    }

    /**
     * Metodos de relações
     * - Multiplicidade 1 (ManyToOne/OneToOne): assign e remove
     * - Multiplicidade * (OneToMany/ManyToMany): add  e remove
     */

    /**
     * Itera sobre as associações da classe e gera os métodos apropriados.
     */
    private void generateRelationshipMethods(PrintWriter w, Class cls, List<Association> associations) {
        for (RelationshipInfo rel : RelationshipInfo.collectForMenus(cls, associations)) {
            if (rel.isCollection()) {
                generateAddMethod(w, cls.getName(), rel);
                generateRemoveFromListMethod(w, cls.getName(), rel);
            } else {
                generateAssignMethod(w, cls.getName(), rel);
                generateRemoveAssignMethod(w, cls.getName(), rel);
            }
        }
    }

    // escreve o campo do outro lado (Map -> put pela chave, coleção -> add, singular -> set)
    private void writeInverseLink(PrintWriter w, RelationshipInfo rel, String inverseVar, String ownVar, String indent) {
        String invCap = GeneratorUtils.capitalizeFirst(GeneratorUtils.getOtherSideAttributeName(rel.assoc, rel.isSource));
        if (rel.assoc.isMapEnd(!rel.isSource)) {
            String qualCap = GeneratorUtils.capitalizeFirst(rel.assoc.getQualifierName());
            w.println(indent + inverseVar + ".get" + invCap + "().put(" + ownVar + ".get" + qualCap + "(), " + ownVar + ");");
        } else if (rel.inverseIsCollection()) {
            w.println(indent + inverseVar + ".get" + invCap + "().add(" + ownVar + ");");
        } else {
            w.println(indent + inverseVar + ".set" + invCap + "(" + ownVar + ");");
        }
    }

    // mesma logica do writeInverseLink, para as opções de desligar
    private void writeInverseUnlink(PrintWriter w, RelationshipInfo rel, String inverseVar, String ownVar, String indent) {
        String invCap = GeneratorUtils.capitalizeFirst(GeneratorUtils.getOtherSideAttributeName(rel.assoc, rel.isSource));
        if (rel.assoc.isMapEnd(!rel.isSource)) {
            String qualCap = GeneratorUtils.capitalizeFirst(rel.assoc.getQualifierName());
            w.println(indent + inverseVar + ".get" + invCap + "().remove(" + ownVar + ".get" + qualCap + "());");
        } else if (rel.inverseIsCollection()) {
            w.println(indent + inverseVar + ".get" + invCap + "().remove(" + ownVar + ");");
        } else {
            w.println(indent + inverseVar + ".set" + invCap + "(null);");
        }
    }

    // Assign method (ManyToOne/OneToOne): entity.setOther(other)
    private void generateAssignMethod(PrintWriter w, String ownerClass, RelationshipInfo rel) {
        String otherClass = rel.otherClass;
        String capitalized = GeneratorUtils.capitalizeFirst(rel.attributeName());
        w.println("    private void assign" + capitalized + "() {");
        w.println("        Long ownerId = InputUtils.readLong(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " owner = " + GeneratorUtils.repoVariable(ownerClass) + ".findById(ownerId);");
        w.println("        if (owner == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        Long otherId = InputUtils.readLong(scanner, \"" + otherClass + " ID: \");");
        w.println("        " + otherClass + " other = " + GeneratorUtils.repoVariable(otherClass) + ".findById(otherId);");
        w.println("        if (other == null) { System.out.println(\"" + otherClass + " not found.\"); return; }");
        w.println();
        w.println("        owner.set" + capitalized + "(other);");
        if (rel.isBidirectional()) writeInverseLink(w, rel, "other", "owner", "        ");
        w.println("        " + GeneratorUtils.repoVariable(ownerClass) + ".save(owner);");
        w.println("        System.out.println(\"Assigned " + otherClass + " to " + ownerClass + ".\");");
        w.println("    }");
        w.println();
    }

    // remove assign
    private void generateRemoveAssignMethod(PrintWriter w, String ownerClass, RelationshipInfo rel) {
        String otherClass = rel.otherClass;
        String capitalized = GeneratorUtils.capitalizeFirst(rel.attributeName());
        w.println("    private void remove" + capitalized + "() {");
        w.println("        Long id = InputUtils.readLong(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " entity = " + GeneratorUtils.repoVariable(ownerClass) + ".findById(id);");
        w.println("        if (entity == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        if (rel.isBidirectional()) {
            w.println("        " + otherClass + " previous = entity.get" + capitalized + "();");
        }
        w.println("        entity.set" + capitalized + "(null);");
        if (rel.isBidirectional()) {
            w.println("        if (previous != null) {");
            writeInverseUnlink(w, rel, "previous", "entity", "            ");
            w.println("        }");
        }
        w.println("        System.out.println(\"" + otherClass + " removed from \" + entity);");
        w.println("    }");
        w.println();
    }

    // add (OneToMany/ManyToMany/Map): list.add(other) ou map.put(key, other)
    private void generateAddMethod(PrintWriter w, String ownerClass, RelationshipInfo rel) {
        String otherClass = rel.otherClass;
        String capitalized = GeneratorUtils.capitalizeFirst(rel.attributeName());
        w.println("    private void add" + capitalized + "() {");
        w.println("        Long ownerId = InputUtils.readLong(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " owner = " + GeneratorUtils.repoVariable(ownerClass) + ".findById(ownerId);");
        w.println("        if (owner == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        Long otherId = InputUtils.readLong(scanner, \"" + otherClass + " ID: \");");
        w.println("        " + otherClass + " other = " + GeneratorUtils.repoVariable(otherClass) + ".findById(otherId);");
        w.println("        if (other == null) { System.out.println(\"" + otherClass + " not found.\"); return; }");
        w.println();

        if (rel.isMap()) {
            // Map fazemos put com a chave sendo o qualifier do outro objeto
            // owner.getAlunos().put(other.getNumero(), other)
            String qualCap = GeneratorUtils.capitalizeFirst(rel.assoc.getQualifierName());
            w.println("        owner.get" + capitalized + "().put(other.get" + qualCap + "(), other);");
        } else {
            w.println("        owner.get" + capitalized + "().add(other);");
        }
        if (rel.isBidirectional()) writeInverseLink(w, rel, "other", "owner", "        ");

        w.println("        " + GeneratorUtils.repoVariable(ownerClass) + ".save(owner);");
        w.println("        System.out.println(\"Added " + otherClass + " to " + ownerClass + ".\");");
        w.println("    }");
        w.println();
    }

    // remove da lista/map
    private void generateRemoveFromListMethod(PrintWriter w, String ownerClass, RelationshipInfo rel) {
        String otherClass = rel.otherClass;
        String capitalized = GeneratorUtils.capitalizeFirst(rel.attributeName());
        w.println("    private void remove" + capitalized + "() {");
        w.println("        Long ownerId = InputUtils.readLong(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " owner = " + GeneratorUtils.repoVariable(ownerClass) + ".findById(ownerId);");
        w.println("        if (owner == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        Long otherId = InputUtils.readLong(scanner, \"" + otherClass + " ID: \");");
        w.println("        " + otherClass + " other = " + GeneratorUtils.repoVariable(otherClass) + ".findById(otherId);");
        w.println("        if (other == null) { System.out.println(\"" + otherClass + " not found.\"); return; }");
        w.println();

        if (rel.isMap()) {
            // map remove pela chave
            String qualCap = GeneratorUtils.capitalizeFirst(rel.assoc.getQualifierName());
            w.println("        owner.get" + capitalized + "().remove(other.get" + qualCap + "());");
        } else {
            w.println("        owner.get" + capitalized + "().remove(other);");
        }
        if (rel.isBidirectional()) writeInverseUnlink(w, rel, "other", "owner", "        ");

        w.println("        System.out.println(\"" + otherClass + " removed from \" + owner);");
        w.println("    }");
        w.println();
    }

    /**
     * opções para operações custom
     * pede owner por ID, lê cada parâmetro
     * chama owner.op(args)
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
            w.println("        Long ownerId = InputUtils.readLong(scanner, \"" + cls.getName() + " ID: \");");
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
                    w.println("        Long " + pName + "Id = InputUtils.readLong(scanner, \"" + pType + " ID (" + pName + "): \");");
                    w.println("        " + pType + " " + pName + " = " + GeneratorUtils.repoVariable(pType) + ".findById(" + pName + "Id);");
                    w.println("        if (" + pName + " == null) { System.out.println(\"" + pType + " not found.\"); return; }");
                } else {
                    // simples: input direto
                    Attribute tmp = new Attribute();
                    tmp.setName(pName);
                    tmp.setType(pType);
                    generateReadInputForAttribute(w, tmp);
                }
                w.println();
            }

            // try/catch para que uma pré/pós condição apanhada (IllegalStateException)
            // mostra o erro e volta ao menu, em vez de rebentar a app
            w.println("        try {");
            // guarda o retorno numa variável (com o tipo de retorno da op) e mostra-o se não for void.
            // grava só o owner
            String call = "owner." + op.getName() + "(" + String.join(", ", argNames) + ")";
            String rt;
            if (entry.getReturnType() != null && !entry.getReturnType().isBlank()) {
                rt = entry.getReturnType();   // o tipo do entry message tem prioridade
            } else if (op.getReturnType() != null) {
                rt = op.getReturnType();      // senão o declarado na operação
            } else {
                rt = "void";
            }
            boolean voidReturn = rt.isBlank() || "void".equals(rt);
            w.println("            " + (voidReturn ? "" : rt + " ret = ") + call + ";");
            // invariantes da classe depois da op
            if (cls.hasAnyInvariants()) {
                w.println("            List<String> errors = owner.validate();");
                w.println("            if (!errors.isEmpty()) {");
                w.println("                System.out.println(\"Validation errors:\");");
                w.println("                errors.forEach(e -> System.out.println(\"  - \" + e));");
                w.println("                return;");
                w.println("            }");
            }
            w.println("            " + GeneratorUtils.repoVariable(cls.getName()) + ".save(owner);");
            w.println("            System.out.println(\"" + op.getName() + " executed.\");");
            if (!voidReturn) w.println("            System.out.println(\"Result: \" + ret);");
            w.println("        } catch (RuntimeException ex) {");
            w.println("            System.out.println(\"Error: \" + ex.getMessage());");
            w.println("        }");
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
            w.println("package " + basePackage + ".menus;");
            w.println();
            w.println("import " + basePackage + ".repository.*;");
            w.println("import " + basePackage + ".util.InputUtils;");
            w.println("import java.util.*;");
            w.println("import java.time.*;");
            w.println();
            w.println("public class MainMenu {");
            w.println();
            w.println("    private Scanner scanner = new Scanner(System.in);");

            // um campo de repo por classe
            // raízes primeiro porque cada repo filho se inicializa com o repo da raiz
            List<Class> repoOrder = new ArrayList<>();
            for (Class c : classes) if (c.getParentClass() == null) repoOrder.add(c);   // raízes (donos do store)
            for (Class c : classes) if (c.getParentClass() != null) repoOrder.add(c);   // filhos (vistas sobre a raiz)
            for (Class cls : repoOrder) {
                String var = GeneratorUtils.repoVariable(cls.getName());
                if (cls.getParentClass() == null) {
                    w.println("    private " + cls.getName() + "Repository " + var + " = new " + cls.getName() + "Repository();");
                } else {
                    String rootVar = GeneratorUtils.repoVariable(cls.getRoot().getName());
                    w.println("    private " + cls.getName() + "Repository " + var + " = new " + cls.getName() + "Repository(" + rootVar + ");");
                }
            }
            w.println();

            // só classes concretas têm menu próprio (abstratas não)
            List<Class> concrete = classes.stream().filter(c -> !c.isAbstract()).toList();

            // run
            w.println("    public void run() {");
            w.println("        while (true) {");
            w.println("            System.out.println(\"\\n=== Main Menu ===\");");
            for (int i = 0; i < concrete.size(); i++) {
                w.println("            System.out.println(\"" + (i + 1) + ". " + concrete.get(i).getName() + " Menu\");");
            }
            w.println("            System.out.println(\"0. Exit\");");
            w.println();
            w.println("            int option = InputUtils.readInt(scanner, \"Option: \");");
            w.println("            switch (option) {");
            for (int i = 0; i < concrete.size(); i++) {
                Class cls = concrete.get(i);
                Set<String> neededRepos = GeneratorUtils.getNeededRepositories(cls, associations, classes);

                w.print("                case " + (i + 1) + ": new " + cls.getName() + "Menu(scanner, " +
                        GeneratorUtils.repoVariable(cls.getName()));
                for (String related : neededRepos) {
                    w.print(", " + GeneratorUtils.repoVariable(related));
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
    // expressão que converte um token (String) no tipo do elemento da coleção
    private String elementParse(String elementType, String token) {
        switch (elementType) {
            case "String":  return token;
            case "Integer": return "Integer.parseInt(" + token + ")";
            case "Long":    return "Long.parseLong(" + token + ")";
            case "Double":  return "Double.parseDouble(" + token + ")";
            case "Float":   return "Float.parseFloat(" + token + ")";
            case "Boolean": return "Boolean.parseBoolean(" + token + ")";
            default:
                if (isTimeType(elementType)) return elementType + ".parse(" + token + ")";
                throw new IllegalArgumentException("Elemento de coleção '" + elementType + "' não suportado no CLI.");
        }
    }

    private void generateReadInputForAttribute(PrintWriter w, Attribute attr) {
        String name = attr.getName();

        // coleção de tipos basicos, input separado por virg
        if (attr.isCollection()) {
            String container = attr.getCollectionType().equals("List") ? "ArrayList" : "HashSet";
            w.println("        System.out.print(\"" + name + " (separado por vírgulas): \");");
            w.println("        " + attr.getAttributeType() + " " + name + " = new " + container + "<>();");
            w.println("        String " + name + "Line = scanner.nextLine().trim();");
            w.println("        if (!" + name + "Line.isEmpty()) for (String s : " + name + "Line.split(\",\")) "
                    + name + ".add(" + elementParse(attr.getType(), "s.trim()") + ");");
            return;
        }

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
                w.println("        float " + name + " = InputUtils.readFloat(scanner, \"" + name + ": \");");
                break;
            case "double":
            case "Double":
                w.println("        double " + name + " = InputUtils.readDouble(scanner, \"" + name + ": \");");
                break;
            case "BigDecimal":
                w.println("        java.math.BigDecimal " + name + " = InputUtils.readBigDecimal(scanner, \"" + name + ": \");");
                break;
            case "BigInteger":
                w.println("        java.math.BigInteger " + name + " = InputUtils.readBigInteger(scanner, \"" + name + ": \");");
                break;
            case "boolean":
            case "Boolean":
                // parseBoolean: tudo o que não for "true" assume-se false
                w.println("        System.out.print(\"" + name + " (true/false): \");");
                w.println("        boolean " + name + " = Boolean.parseBoolean(scanner.nextLine());");
                break;
            default:
                if (enumNames.contains(attr.getType())) {
                    w.println("        " + attr.getType() + " " + name + " = null;");
                    w.println("        while (true) {");
                    w.println("            System.out.print(\"" + name + " \" + java.util.Arrays.toString(" + attr.getType() + ".values()) + \" (enter = nenhum): \");");
                    w.println("            String " + name + "In = scanner.nextLine().trim();");
                    w.println("            if (" + name + "In.isEmpty()) break;");
                    w.println("            try { " + name + " = " + attr.getType() + ".valueOf(" + name + "In); break; }");
                    w.println("            catch (IllegalArgumentException ex) { System.out.println(\"Invalid value. Try again.\"); }");
                    w.println("        }");
                } else if (isTimeType(attr.getType())) {
                    // mesmo padrão de retry do enum, com parse de data
                    String type = attr.getType();
                    w.println("        " + type + " " + name + " = null;");
                    w.println("        while (true) {");
                    w.println("            System.out.print(\"" + name + " (ex: " + timeHint(type) + ", enter = nenhum): \");");
                    w.println("            String " + name + "In = scanner.nextLine().trim();");
                    w.println("            if (" + name + "In.isEmpty()) break;");
                    w.println("            try { " + name + " = " + type + ".parse(" + name + "In); break; }");
                    w.println("            catch (Exception ex) { System.out.println(\"Invalid value. Try again.\"); }");
                    w.println("        }");
                } else {
                    throw new IllegalArgumentException("Atributo '" + name + "' do tipo '" + attr.getType()
                            + "': tipo não suportado no target CLI.");
                }
        }
    }

    /**
     * gera código para atualizar um atributo
     * mostra o valor atual, se user clicar enter mantém o valor, senão atualiza
     */
    private void generateUpdateAttribute(PrintWriter w, Attribute attr) {
        String name = attr.getName();
        String capitalized = GeneratorUtils.capitalizeFirst(name);

        // coleção de tipos basicos: input separado por vírgulas, enter mantém
        if (attr.isCollection()) {
            String container = attr.getCollectionType().equals("List") ? "ArrayList" : "HashSet";
            w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() + \"] (separado por vírgulas, enter mantém): \");");
            w.println("        String " + name + "Line = scanner.nextLine().trim();");
            w.println("        if (!" + name + "Line.isEmpty()) {");
            w.println("            " + attr.getAttributeType() + " " + name + " = new " + container + "<>();");
            w.println("            for (String s : " + name + "Line.split(\",\")) " + name + ".add(" + elementParse(attr.getType(), "s.trim()") + ");");
            w.println("            entity.set" + capitalized + "(" + name + ");");
            w.println("        }");
            return;
        }

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
                w.println("        if (!" + name + "Str.isEmpty()) {");
                w.println("            try { entity.set" + capitalized + "(Integer.parseInt(" + name + "Str.trim())); }");
                w.println("            catch (NumberFormatException ex) { System.out.println(\"Invalid value, keeping current.\"); }");
                w.println("        }");
                break;
            case "long":
            case "Long":
                w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() +" +
                        "\"]: \");");
                w.println("        String " + name + "Str = scanner.nextLine();");
                w.println("        if (!" + name + "Str.isEmpty()) {");
                w.println("            try { entity.set" + capitalized + "(Long.parseLong(" + name + "Str.trim())); }");
                w.println("            catch (NumberFormatException ex) { System.out.println(\"Invalid value, keeping current.\"); }");
                w.println("        }");
                break;
            case "float":
            case "Float":
                w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() +" +
                        "\"]: \");");
                w.println("        String " + name + "Str = scanner.nextLine();");
                w.println("        if (!" + name + "Str.isEmpty()) {");
                w.println("            try { entity.set" + capitalized + "(Float.parseFloat(" + name + "Str.trim())); }");
                w.println("            catch (NumberFormatException ex) { System.out.println(\"Invalid value, keeping current.\"); }");
                w.println("        }");
                break;
            case "double":
            case "Double":
                w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() +" +
                        "\"]: \");");
                w.println("        String " + name + "Str = scanner.nextLine();");
                w.println("        if (!" + name + "Str.isEmpty()) {");
                w.println("            try { entity.set" + capitalized + "(Double.parseDouble(" + name + "Str.trim())); }");
                w.println("            catch (NumberFormatException ex) { System.out.println(\"Invalid value, keeping current.\"); }");
                w.println("        }");
                break;
            case "BigDecimal":
            case "BigInteger":
                w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() +" +
                        "\"]: \");");
                w.println("        String " + name + "Str = scanner.nextLine();");
                w.println("        if (!" + name + "Str.isEmpty()) {");
                w.println("            try { entity.set" + capitalized + "(new java.math." + attr.getType()
                        + "(" + name + "Str.trim())); }");
                w.println("            catch (NumberFormatException ex) { System.out.println(\"Invalid value, keeping current.\"); }");
                w.println("        }");
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
                    // w.println("            try { entity.set" + capitalized + "(" + attr.getType() + ".valueOf(" + name + "Str.trim().toUpperCase())); break; }");
                    w.println("            try { entity.set" + capitalized + "(" + attr.getType() + ".valueOf(" + name + "Str.trim())); break; }");
                    w.println("            catch (IllegalArgumentException ex) { System.out.println(\"Invalid value. Try again.\"); }");
                    w.println("        }");
                } else if (isTimeType(attr.getType())) {
                    // enter mantém, valor inválido mantém com aviso
                    String type = attr.getType();
                    w.println("        System.out.print(\"" + name + " [\" + entity.get" + capitalized + "() + \"] (ex: " + timeHint(type) + "): \");");
                    w.println("        String " + name + "Str = scanner.nextLine();");
                    w.println("        if (!" + name + "Str.isEmpty()) {");
                    w.println("            try { entity.set" + capitalized + "(" + type + ".parse(" + name + "Str.trim())); }");
                    w.println("            catch (Exception ex) { System.out.println(\"Invalid value, keeping current.\"); }");
                    w.println("        }");
                } else {
                    // antes gerava entity.setX(String) para tipos não suportados e dava problema
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

    // exemplo de formato mostrado no prompt
    private static String timeHint(String type) {
        switch (type) {
            case "LocalDate":     return "2026-01-31";
            case "LocalDateTime": return "2026-01-31T10:30";
            default:              return "10:30";   // LocalTime
        }
    }

}
