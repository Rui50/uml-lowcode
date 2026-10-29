package generator.CLI_JPA;

import generator.GeneratorUtils;
import generator.OperationGenerator;
import generator.RelationshipInfo;
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
 * gera os menus com (EntityManager /JPA).
 *
 * Notas:
 *  - toda as mudanças são transacionais (begin, commit, rollback,  em.clear() no erro);
 *  - invariantes validadas antes do save
 *  - relações bidirecionais validam os dois lados
 *
 */
public class CLIMenuGenerator {

    private final String baseOutputPath;
    private final String basePackage;
    private final Set<String> enumNames = new HashSet<>();

    // nome -> Class, para resolver o tipo do id de outra classe nas relações
    private final Map<String, Class> classMap = new HashMap<>();

    public CLIMenuGenerator(String baseOutputPath, String basePackage) {
        this.baseOutputPath = baseOutputPath;
        this.basePackage = basePackage;
    }

    // tipo do id de uma classe (pelo nome)
    private String idTypeOf(String className) {
        return GeneratorUtils.idType(classMap.get(className));
    }

    // converte o texto lido para o tipo do id da classe (o create lê como String p/ aceitar vazio=null)
    private String parseIdExpr(String className, String var) {
        switch (idTypeOf(className)) {
            case "String":  return var;
            case "UUID":    return "java.util.UUID.fromString(" + var + ")";
            case "Integer": return "Integer.valueOf(" + var + ")";
            default:        return "Long.valueOf(" + var + ")";
        }
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

    //  pede o id de <className> (prompt "<className> ID: "), guarda em <var>,
    // e volta ao menu (return) se a entidade não existir
    // para o assign / add / remove / custom
    private void writeReadOrReturn(PrintWriter w, String className, String var) {
        String idVar = var + "Id";
        w.println("        " + idTypeOf(className) + " " + idVar + " = InputUtils." + idReaderOf(className) + "(scanner, \"" + className + " ID: \");");
        w.println("        " + className + " " + var + " = " + GeneratorUtils.repoVariable(className) + ".findById(" + idVar + ");");
        w.println("        if (" + var + " == null) { " +
                "System.out.println(\"" + className + " not found.\"); return; }");
    }

    // pede o id da entidade do menu (prompt "ID: "), return se não existir
    // para o viewById / update / delete
    private void writeReadSelfOrReturn(PrintWriter w, String className) {
        w.println("        " + idTypeOf(className) + " id = InputUtils." + idReaderOf(className) + "(scanner, \"ID: \");");
        w.println("        " + className + " entity = " + GeneratorUtils.repoVariable(className) + ".findById(id);");
        w.println("        if (entity == null) {");
        w.println("            System.out.println(\"Not found.\");");
        w.println("            return;");
        w.println("        }");
    }

    // fecha o try da transação: } catch { rollback + em.clear() + print o erro }
    private void writeTransactionClose(PrintWriter w) {
        w.println("        } catch (RuntimeException ex) {");
        w.println("            if (em.getTransaction().isActive()) em.getTransaction().rollback();");
        w.println("            em.clear();");
        w.println("            System.out.println(\"Error: \" + ex.getMessage());");
        w.println("        }");
    }

    // sincroniza o lado inverso em memória (senão o outro lado gerido fica "stale").
    private void writeInverseLink(PrintWriter w, Association assoc, boolean isSource,
                                  String container, String element, String indent) {
        String invCap = GeneratorUtils.capitalizeFirst(GeneratorUtils.getOtherSideAttributeName(assoc, isSource));
        boolean invColl = GeneratorUtils.checkMultiplicity(isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity());
        if (assoc.hasQualifier()) {
            String qualCap = GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            w.println(indent + container + ".get" + invCap + "().put(" + element + ".get" + qualCap + "(), " + element + ");");
        } else if (invColl) {
            w.println(indent + container + ".get" + invCap + "().add(" + element + ");");
        } else {
            w.println(indent + container + ".set" + invCap + "(" + element + ");");
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

            // attributes
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
     * gera metodo run()
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

        for (RelationshipInfo rel : RelationshipInfo.collectForMenus(cls, associations)) {
            String attributeName = rel.attributeName();
            String cap = GeneratorUtils.capitalizeFirst(attributeName);

            if (rel.isCollection()) {
                // OneToMany, ManyToMany ou Map: Add e Remove
                options.add(new String[]{"Add " + attributeName, "add" + cap});
                options.add(new String[]{"Remove " + attributeName, "remove" + cap});
            } else {
                // ManyToOne/OneToOne: Assign e Remove
                options.add(new String[]{"Assign " + attributeName, "assign" + cap});
                options.add(new String[]{"Remove " + attributeName, "remove" + cap});
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
    private void generateCreateMethod(PrintWriter w, Class cls, List<Association> associations) {
        w.println("    private void create() {");

        // obter atributos do pai + próprios (mesma ordem que o construtor)
        // excluir o atributo id
        String idName = cls.getRoot().getIdAttributeName();

        List<Attribute> allAttrs = new ArrayList<>();

        allAttrs.addAll(GeneratorUtils.ancestorAttributes(cls).stream()
                .filter(a -> !a.hasDerive() && !a.hasInit())
                .filter(a -> cls.isIdAssigned() || !a.getName().equals(idName))
                .toList());
        // atributos próprios da classe,  ma chave strijng
        // (isIdAssigned) é atribuída pelo cliente, logo tem de ser pedida no create
        allAttrs.addAll(cls.getAttributes().stream()
                .filter(a -> !a.hasDerive() && !a.hasInit())
                .filter(a -> cls.isIdAssigned() || !a.getName().equals(idName))
                .toList());

        // ler cada atributo
        for (Attribute attr : allAttrs) {
            generateReadInputForAttribute(w, attr);
        }
        w.println();

        // chave string (id vem do utilizador), como o id não é null, o save() faz merge(), que faria update
        // se o id já existir, por isso verifica-se primeiro se existe, para não sobrescrever.
        if (cls.isIdAssigned() && idName != null) {
            w.println("        if (" + GeneratorUtils.repoVariable(cls.getName()) + ".findById(" + idName + ") != null) {");
            w.println("            System.out.println(\"" + cls.getName() + " with id '\" + " + idName + " + \"' already exists.\");");
            w.println("            return;");
            w.println("        }");
            w.println();
        }

        // Se uma relação é obrigatória (self.x <> null), tem de se poder preencher no create, senão a entidade
        // nunca passa na validação. Por isso pede-se aqui o id do outro objeto (enter = vazio para as opcionais).
        // single dos DOIS lados: o não dono (1:1 bidirecional) também tem de poder ser preenchido
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
            w.println("            try {");
            w.println("                " + attribute + " = " + GeneratorUtils.repoVariable(other)
                    + ".findById(" + parseIdExpr(other, in) + ");");
            w.println("            } catch (RuntimeException ex) { System.out.println(\"Invalid id.\"); return; }");
            w.println("            if (" + attribute + " == null) { System.out.println(\"" + other + " not found.\"); return; }");
            w.println("        }");
            w.println();
        }

        // criar entidade com no arg + setters
        w.println("        " + cls.getName() + " entity = new " + cls.getName() + "();");
        for (Attribute attr : allAttrs) {
            String cap = GeneratorUtils.capitalizeFirst(attr.getName());
            w.println("        entity.set" + cap + "(" + attr.getName() + ");");
        }
        for (RelationshipInfo rel : singleRels) {
            String var = rel.attributeName();
            w.println("        entity.set" + GeneratorUtils.capitalizeFirst(var) + "(" + var + ");");

            // sincroniza o inverso em memória, senão o outro lado fica com a coleção "stale"
            // (var pode ser null -> relação opcional -> guard)
            if (rel.assoc.isSourceNavigable() && rel.assoc.isTargetNavigable()) {
                w.println("        if (" + var + " != null) {");
                writeInverseLink(w, rel.assoc, rel.isSource, var, "entity", "            ");
                w.println("        }");
            }
        }

        // validation
        w.println();
        generateValidation(w, cls.getName(), "entity", null, null, false);

        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + GeneratorUtils.repoVariable(cls.getName()) + ".save(entity);");
        // lado não dono: quem grava a relação é o outro objeto (tem a FK)
        for (RelationshipInfo rel : singleRels) {
            if (rel.isOwner()) continue;
            String var = rel.attributeName();
            w.println("            if (" + var + " != null) " + GeneratorUtils.repoVariable(rel.otherClass)
                    + ".save(" + var + ");");
        }
        w.println("            em.getTransaction().commit();");
        w.println("            System.out.println(\"Created: \" + entity);");
        writeTransactionClose(w);
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
        writeReadSelfOrReturn(w, cls.getName());
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

            // .values() só se o campo desta ponta for mesmo um Map; senão imprime a referência
            if (rel.isMap()) {
                w.println(indent + "System.out.println(\"  " + attributeName + ": \" + new ArrayList<>(" + getter + ".values()));");
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
        writeReadSelfOrReturn(w, cls.getName());
        w.println();
        w.println("        System.out.println(\"Current: \" + entity);");
        w.println("        System.out.println(\"(press Enter to keep current value)\");");
        w.println();

        // para cada atributo (pai + próprios) ler novo valor ou manter atual
        // id não muda em update
        String idAttrName = cls.getRoot().getIdAttributeName();

        List<Attribute> allAttrs = new ArrayList<>();

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
        w.println();
        generateValidation(w, cls.getName(), "entity", null, null, false);

        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + GeneratorUtils.repoVariable(cls.getName()) + ".save(entity);");
        w.println("            em.getTransaction().commit();");
        w.println("            System.out.println(\"Updated: \" + entity);");
        writeTransactionClose(w);
        w.println("    }");
        w.println();
    }

    /**
     *  delete(): pede ID e remove do repositório.
     */
    private void generateDeleteMethod(PrintWriter w, Class cls) {
        w.println("    private void delete() {");
        writeReadSelfOrReturn(w, cls.getName());
        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + GeneratorUtils.repoVariable(cls.getName()) + ".deleteById(id);");
        w.println("            em.getTransaction().commit();");
        // limpar o cache do em, senão a entidade apagada continuava nas coleções já carregadas
        w.println("            em.clear();");
        w.println("            System.out.println(\"Deleted.\");");
        writeTransactionClose(w);
        w.println("    }");
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
            String attributeName = rel.attributeName();

            if (rel.isCollection()) {
                generateAddMethod(w, cls.getName(), rel.otherClass, attributeName, rel.assoc, rel.isSource);
                generateRemoveFromListMethod(w, cls.getName(), rel.otherClass, attributeName, rel.assoc, rel.isSource);
            } else {
                generateAssignMethod(w, cls.getName(), rel.otherClass, attributeName, rel.assoc, rel.isSource);
                generateRemoveAssignMethod(w, cls.getName(), rel.otherClass, attributeName, rel.assoc, rel.isSource);
            }
        }
    }

    private boolean hasInvariants(String className) {
        Class c = classMap.get(className);
        return c != null && c.hasAnyInvariants();
    }

    // Valida 'ownerVar' e 'otherVar' (se dado) antes do save, so os que têm invariantes. Se falhar,
    // imprime os erros, remove em memória (em.clear) e return. otherNullable -> valida o outro só se != null.
    private void generateValidation(PrintWriter w, String ownerClass, String ownerVar,
                                String otherClass, String otherVar, boolean otherNullable) {
        boolean ownerHas = hasInvariants(ownerClass);
        boolean otherHas = otherVar != null && hasInvariants(otherClass);
        if (!ownerHas && !otherHas) return;

        if (ownerHas) {
            w.println("        if (InputUtils.printErrors(" + ownerVar + ".validate())) { em.clear(); return; }");
        }
        if (otherHas) {
            String cond = "InputUtils.printErrors(" + otherVar + ".validate())";
            if (otherNullable) cond = otherVar + " != null && " + cond;
            w.println("        if (" + cond + ") { em.clear(); return; }");
        }
        w.println();
    }

    // Assign method (ManyToOne/OneToOne): entity.setOther(other)
    private void generateAssignMethod(PrintWriter w, String ownerClass, String otherClass, String attributeName, Association assoc, boolean isSource) {
        String capitalized = GeneratorUtils.capitalizeFirst(attributeName);
        boolean bidir = assoc.isSourceNavigable() && assoc.isTargetNavigable();
        w.println("    private void assign" + capitalized + "() {");
        writeReadOrReturn(w, ownerClass, "owner");
        w.println();
        writeReadOrReturn(w, otherClass, "other");
        w.println();
        w.println("        owner.set" + capitalized + "(other);");

        if (bidir) {
            // sincroniza o inverso em mem para poder validar o outro lado (substitui o em.refresh(other) pós-commit).
            // aqui esta ponta é single (assign), por isso o qualifier, se existir, está no inverso (Map) -> put por chave
            writeInverseLink(w, assoc, isSource, "other", "owner", "        ");
            generateValidation(w, ownerClass, "owner", otherClass, "other", false);
        } else {
            generateValidation(w, ownerClass, "owner", null, null, false);
        }

        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + GeneratorUtils.repoVariable(ownerClass) + ".save(owner);");
        w.println("            em.getTransaction().commit();");
        // só o owner é gravado, por isso o 'other' em memória pode ficar desatualizado -> em.refresh(other).
        // no bidir o inverso já foi atualizado acima, logo não precisa
        if (!bidir) w.println("            em.refresh(other);");
        w.println("            System.out.println(\"Assigned " + otherClass + " to " + ownerClass + ".\");");
        writeTransactionClose(w);
        w.println("    }");
        w.println();
    }

    // remove assign
    private void generateRemoveAssignMethod(PrintWriter w, String ownerClass, String otherClass, String attributeName, Association assoc, boolean isSource) {
        String capitalized = GeneratorUtils.capitalizeFirst(attributeName);
        boolean bidir = assoc.isSourceNavigable() && assoc.isTargetNavigable();
        w.println("    private void remove" + capitalized + "() {");
        w.println("        " + idTypeOf(ownerClass) + " id = InputUtils." + idReaderOf(ownerClass) + "(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " entity = " + GeneratorUtils.repoVariable(ownerClass) + ".findById(id);");
        w.println("        if (entity == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        " + otherClass + " previous = entity.get" + capitalized + "();");
        w.println("        entity.set" + capitalized + "(null);");

        if (bidir) {
            // remove o entity do inverso ('previous') em memória, para validar o lado antig
            String invMult = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
            String invCap = GeneratorUtils.capitalizeFirst(GeneratorUtils.getOtherSideAttributeName(assoc, isSource));
            boolean invColl = GeneratorUtils.checkMultiplicity(invMult);
            // inverso Map -> remove pela chave
            w.println("        if (previous != null) {");
            if (assoc.hasQualifier()) {
                String qualCap = GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
                w.println("            previous.get" + invCap + "().remove(entity.get" + qualCap + "());");
            } else {
                w.println(invColl ? "            previous.get" + invCap + "().remove(entity);" : "            previous.set" + invCap + "(null);");
            }
            w.println("        }");
            generateValidation(w, ownerClass, "entity", otherClass, "previous", true);
        } else {
            generateValidation(w, ownerClass, "entity", null, null, false);
        }

        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + GeneratorUtils.repoVariable(ownerClass) + ".save(entity);");
        w.println("            em.getTransaction().commit();");
        if (!bidir) w.println("            if (previous != null) em.refresh(previous);");   // bidir ent já sincronizado em memória
        w.println("            System.out.println(\"" + otherClass + " removed from \" + entity);");
        writeTransactionClose(w);
        w.println("    }");
        w.println();
    }

    // add (OneToMany/ManyToMany/Map): list.add(other) ou map.put(key, other)
    private void generateAddMethod(PrintWriter w, String ownerClass, String otherClass, String attributeName, Association assoc, boolean isSource) {
        String capitalized = GeneratorUtils.capitalizeFirst(attributeName);
        w.println("    private void add" + capitalized + "() {");
        writeReadOrReturn(w, ownerClass, "owner");
        w.println();
        writeReadOrReturn(w, otherClass, "other");
        w.println();

        // uma relação só é persistida se for escrita pelo lado owner
        // se o campo é o lado inverso (que tem mappedBy), gravar owner.getCampo().add(other) não escreve nada
        // temos de escrever o campo dono no 'other' e gravar o 'other'.
        // qualifier/Map é sempre owner
        boolean isOwner = GeneratorUtils.isOwningSide(assoc, isSource);
        boolean bidir = assoc.isSourceNavigable() && assoc.isTargetNavigable();
        String saveVar, saveRepo, refreshVar, saveClass;
        // 2ª entidade a validar (o inverso, quando o sincronizamos em memória); synced -> dispensa o refresh pós-commit
        String otherValClass = null, otherValVar = null;
        boolean synced = false;

        if (assoc.isMapEnd(isSource)) {
            String qualCap = GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            w.println("        owner.get" + capitalized + "().put(other.get" + qualCap + "(), other);");
            saveVar = "owner"; saveRepo = GeneratorUtils.repoVariable(ownerClass); refreshVar = "other"; saveClass = ownerClass;
            if (bidir) {   // sincroniza o inverso do other para o poder validar, igual ao ramo isOwner
                String invCap = GeneratorUtils.capitalizeFirst(GeneratorUtils.getOtherSideAttributeName(assoc, isSource));
                boolean invColl = GeneratorUtils.checkMultiplicity(isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity());
                w.println(invColl ? "        other.get" + invCap + "().add(owner);" : "        other.set" + invCap + "(owner);");
                otherValClass = otherClass; otherValVar = "other"; synced = true;
            }
        } else if (isOwner) {
            w.println("        owner.get" + capitalized + "().add(other);");
            saveVar = "owner"; saveRepo = GeneratorUtils.repoVariable(ownerClass); refreshVar = "other"; saveClass = ownerClass;
            if (bidir) {   // sincroniza o inverso do (other) para o poder validar
                String invCap = GeneratorUtils.capitalizeFirst(GeneratorUtils.getOtherSideAttributeName(assoc, isSource));
                boolean invColl = GeneratorUtils.checkMultiplicity(isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity());
                w.println(invColl ? "        other.get" + invCap + "().add(owner);" : "        other.set" + invCap + "(owner);");
                otherValClass = otherClass; otherValVar = "other"; synced = true;
            }
        } else {
            // lado inverso escrevemos pelo lado dono na var other
            String ownerCap = GeneratorUtils.capitalizeFirst(GeneratorUtils.getOtherSideAttributeName(assoc, isSource));
            boolean ownerIsCollection = GeneratorUtils.checkMultiplicity(
                    isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity());
            // o lado dono pode ser um Map (M:N com qualifier dono = ponta do qualifier) -> put por chave
            boolean ownerIsMapEnd = assoc.isMapEnd(!isSource);   // !isSource = a ponta oposta
            if (ownerIsMapEnd) {
                String qualCap = GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
                w.println("        other.get" + ownerCap + "().put(owner.get" + qualCap + "(), owner);   // lado DONO: é o que persiste");
            } else {
                w.println(ownerIsCollection
                        ? "        other.get" + ownerCap + "().add(owner);   // lado DONO: é o que persiste"
                        : "        other.set" + ownerCap + "(owner);   // lado DONO: é o que persiste");
            }
            saveVar = "other"; saveRepo = GeneratorUtils.repoVariable(otherClass); refreshVar = "owner"; saveClass = otherClass;
            // sincroniza a coleção do owner (inverso, sempre bidirecional aqui) para o  o poder validar
            w.println("        owner.get" + capitalized + "().add(other);");
            otherValClass = ownerClass; otherValVar = "owner"; synced = true;
        }

        generateValidation(w, saveClass, saveVar, otherValClass, otherValVar, false);
        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + saveRepo + ".save(" + saveVar + ");");
        w.println("            em.getTransaction().commit();");
        if (!synced) w.println("            em.refresh(" + refreshVar + ");");   // synced -> já correto em memória
        w.println("            System.out.println(\"Added " + otherClass + " to " + ownerClass + ".\");");
        writeTransactionClose(w);
        w.println("    }");
    }

    // remove da lista/map
    private void generateRemoveFromListMethod(PrintWriter w, String ownerClass, String otherClass, String attributeName, Association assoc, boolean isSource) {
        String capitalized = GeneratorUtils.capitalizeFirst(attributeName);
        w.println("    private void remove" + capitalized + "() {");
        writeReadOrReturn(w, ownerClass, "owner");
        w.println();
        writeReadOrReturn(w, otherClass, "other");
        w.println();

        // mesmo coisa do add a remoção só persiste pelo lado dono
        boolean isOwner = GeneratorUtils.isOwningSide(assoc, isSource);
        boolean bidir = assoc.isSourceNavigable() && assoc.isTargetNavigable();
        String saveVar, saveRepo, refreshVar, saveClass;
        String otherValClass = null, otherValVar = null;
        boolean synced = false;

        if (assoc.isMapEnd(isSource)) {
            String qualCap = GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
            w.println("        owner.get" + capitalized + "().remove(other.get" + qualCap + "());");
            saveVar = "owner"; saveRepo = GeneratorUtils.repoVariable(ownerClass); refreshVar = "other"; saveClass = ownerClass;
            if (bidir) {   // remove o owner do inverso (em other), para o poder validar
                String invCap = GeneratorUtils.capitalizeFirst(GeneratorUtils.getOtherSideAttributeName(assoc, isSource));
                boolean invColl = GeneratorUtils.checkMultiplicity(isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity());
                w.println(invColl ? "        other.get" + invCap + "().remove(owner);" : "        other.set" + invCap + "(null);");
                otherValClass = otherClass; otherValVar = "other"; synced = true;
            }
        } else if (isOwner) {
            w.println("        owner.get" + capitalized + "().remove(other);");
            saveVar = "owner"; saveRepo = GeneratorUtils.repoVariable(ownerClass); refreshVar = "other"; saveClass = ownerClass;
            if (bidir) {   // remove o owner do inverso (em other)
                String invCap = GeneratorUtils.capitalizeFirst(GeneratorUtils.getOtherSideAttributeName(assoc, isSource));
                boolean invColl = GeneratorUtils.checkMultiplicity(isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity());
                w.println(invColl ? "        other.get" + invCap + "().remove(owner);" : "        other.set" + invCap + "(null);");
                otherValClass = otherClass; otherValVar = "other"; synced = true;
            }
        } else {
            // lado inverso tem de remover pelo lado dono (em "other")
            String ownerCap = GeneratorUtils.capitalizeFirst(
                    GeneratorUtils.getOtherSideAttributeName(assoc, isSource));
            boolean ownerIsCollection = GeneratorUtils.checkMultiplicity(
                    isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity());
            // o lado dono pode ser um Map (M:N qualifier) -> remove pela CHAVE
            boolean ownerIsMapEnd = assoc.isMapEnd(!isSource);   // !isSource = a ponta oposta
            if (ownerIsMapEnd) {
                String qualCap = GeneratorUtils.capitalizeFirst(assoc.getQualifierName());
                w.println("        other.get" + ownerCap + "().remove(owner.get" + qualCap + "());   // lado DONO: é o que persiste");
            } else {
                w.println(ownerIsCollection
                        ? "        other.get" + ownerCap + "().remove(owner);   // lado DONO: é o que persiste"
                        : "        other.set" + ownerCap + "(null);   // lado DONO: é o que persiste");
            }
            saveVar = "other"; saveRepo = GeneratorUtils.repoVariable(otherClass); refreshVar = "owner"; saveClass = otherClass;
            // remove o other da coleção do owner (inverso, sempre bidirecional aqui), para o poder validar
            w.println("        owner.get" + capitalized + "().remove(other);");
            otherValClass = ownerClass; otherValVar = "owner"; synced = true;
        }

        generateValidation(w, saveClass, saveVar, otherValClass, otherValVar, false);
        w.println("        em.getTransaction().begin();");
        w.println("        try {");
        w.println("            " + saveRepo + ".save(" + saveVar + ");");
        w.println("            em.getTransaction().commit();");
        // orphanRemoval pode ter apagado o refreshVar no commit, ent so faz refresh se nao tiver sido removido
        if (!synced) w.println("            if (em.contains(" + refreshVar + ")) em.refresh(" + refreshVar + ");");
        w.println("            System.out.println(\"" + otherClass + " removed from " + ownerClass + ".\");");
        writeTransactionClose(w);
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
            writeReadOrReturn(w, cls.getName(), "owner");
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
            // retorno da operação usado com base na entry message e mostrado
            // se não for void. Só owner é persistido, um return entidade não é guardado
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
            // a transação está aberta, logo o rollback desfaz de facto o que a op mudou
            if (hasInvariants(cls.getName())) {
                w.println("            if (InputUtils.printErrors(owner.validate())) {");
                w.println("                em.getTransaction().rollback();");
                w.println("                em.clear();");
                w.println("                return;");
                w.println("            }");
            }
            w.println("            " + GeneratorUtils.repoVariable(cls.getName()) + ".save(owner);");
            w.println("            em.getTransaction().commit();");
            w.println("            System.out.println(\"" + op.getName() + " executed.\");");
            if (!voidReturn) w.println("            System.out.println(\"Result: \" + ret);");
            writeTransactionClose(w);
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

        // colecoes dos atributos: input separado por vírgulas
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
                w.println("        System.out.print(\"" + name + " (true/false): \");");
                w.println("        boolean " + name + " = Boolean.parseBoolean(scanner.nextLine());");
                break;
            default:
                if (enumNames.contains(attr.getType())) {
                    w.println("        " + attr.getType() + " " + name + " = null;");
                    w.println("        while (" + name + " == null) {");
                    w.println("            System.out.print(\"" + name + " \" + java.util.Arrays.toString(" + attr.getType() + ".values()) + \": \");");
                    // w.println("            try { " + name + " = " + attr.getType() + ".valueOf(scanner.nextLine().trim().toUpperCase()); }");
                    w.println("            try { " + name + " = " + attr.getType() + ".valueOf(scanner.nextLine().trim()); }");
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
     * gera código para atualizar um atributo .
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
                    //  w.println("            try { entity.set" + capitalized + "(" + attr.getType() + ".valueOf(" + name + "Str.trim().toUpperCase())); break; }");
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
