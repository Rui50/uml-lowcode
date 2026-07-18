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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gera os menus CLI (plain, interativos, sobre repositórios EM MEMÓRIA — sem BD).
 * Por entidade concreta: CRUD + opções de associação (add/remove | assign/remove) + operações custom
 * (ligadas a uma Interaction) + MainMenu que cria os repos (partilhados) e delega para os sub-menus.
 *
 * Notas:
 *  - sem persistência real: repos são Map em memória; ids sempre Long;
 *  - MainMenu cria UMA instância de cada repo e partilha-a (senão os dados fragmentavam).
 *
 * Lim
 *  - SEM transações: se uma op falha a meio, as mudanças à entidade não são revertidas (fica parcial);
 *  - input: boolean sem retry; short/byte/char/BigDecimal não suportados (lançam na geração);
 *  - info de relação recalculada em 3 sítios (risco de divergência — TODO refactor);
 *  - operação custom não valida invariantes e ignora o valor de retorno.
 */
/**
 * Gera os menus CLI
 *
 * para cada classe gera um menu com:
 *  - CRUD
 *  - opções de associação (add/remove ou assign/remove)
 *  - opções para operações custom (ligadas a uma Interaction)
 *
 * depois dar fix: esta versão é em memória, sem transações. Se uma operação falhar uma
 * pós-condição a meio, as alterações já feitas à entidade não são revertidas (a entidade
 * fica parcialmente modificada). "ja ta fix no clijpa".
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
    public void generateMenus(List<Class> classes, List<Association> associations, List<Enumeration> enumerations) throws IOException {
        enumNames.clear();
        for (Enumeration e : enumerations) {
            enumNames.add(e.getName());
        }
        System.out.println("Generating Java Menus...");

        String menusPath = baseOutputPath + "/src/menus";
        new File(menusPath).mkdirs();

        List<Class> classesToBuild = classes.stream()
                .filter(c -> !c.isAbstract())
                .toList();

        for (Class cls : classesToBuild) {
            generateEntityMenu(cls, classes, associations, menusPath);
        }

        // passa a lista completa, os repositórios (campos) incluem abstratas, porque outros
        // menus dependem delas (tipo o memberRepository)
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
            w.println("package menus;");
            w.println();
            w.println("import models.*;");
            w.println("import repository.*;");
            w.println("import util.InputUtils;");
            w.println("import java.util.*;");
            w.println("import java.time.*;");
            w.println();
            w.println("public class " + className + " {");
            w.println();

            // fields
            w.println("    private Scanner scanner;");
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
            // se a classe é source e target é navegável, inclui de pais
            if (cls.isOrInheritsFrom(assoc.getSourceClass()) && assoc.isTargetNavigable()) {
                String other = assoc.getTargetClass();
                // nome do campo baseado no role
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

        // inclui ops herdadas de pai abstrato (senão a subclasse concreta
        // não as oferecia no menu
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

        // recolher atributos do pai + próprios
        List<Attribute> allAttrs = new ArrayList<>();

        // atributos herdados de TODA a cadeia de pais (raiz-primeiro)
        allAttrs.addAll(GeneratorUtils.ancestorAttributes(cls).stream()
                .filter(a -> !a.hasDerive() && !a.hasInit())
                .toList());
        // atributos próprios da classe
        allAttrs.addAll(cls.getAttributes().stream()
                .filter(a -> !a.hasDerive() && !a.hasInit())
                .toList());

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
        // atributos herdados de TODA a cadeia de pais (raiz-primeiro), não só do pai directo (nota #30)
        allAttrs.addAll(GeneratorUtils.ancestorAttributes(cls).stream()
                .filter(a -> !a.hasDerive())
                .toList());
        allAttrs.addAll(cls.getAttributes().stream()
                .filter(a -> !a.hasDerive())
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
                    generateAddMethod(w, cls.getName(), other, fieldName, assoc);
                    generateRemoveFromListMethod(w, cls.getName(), other, fieldName, assoc);
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
        String capitalized = GeneratorUtils.capitalizeFirst(fieldName);
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
        w.println("        " + GeneratorUtils.repoVariable(ownerClass) + ".save(owner);");
        w.println("        System.out.println(\"Assigned " + otherClass + " to " + ownerClass + ".\");");
        w.println("    }");
        w.println();
    }

    // remove assign
    private void generateRemoveAssignMethod(PrintWriter w, String ownerClass, String otherClass, String fieldName) {
        String capitalized = GeneratorUtils.capitalizeFirst(fieldName);
        w.println("    private void remove" + capitalized + "() {");
        w.println("        Long id = InputUtils.readLong(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " entity = " + GeneratorUtils.repoVariable(ownerClass) + ".findById(id);");
        w.println("        if (entity == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        entity.set" + capitalized + "(null);");
        w.println("        System.out.println(\"" + otherClass + " removed from \" + entity);");
        w.println("    }");
        w.println();
    }

    // add (OneToMany/ManyToMany/Map): list.add(other) ou map.put(key, other)
    private void generateAddMethod(PrintWriter w, String ownerClass, String otherClass, String fieldName, Association assoc) {
        String capitalized = GeneratorUtils.capitalizeFirst(fieldName);
        w.println("    private void add" + capitalized + "() {");
        w.println("        Long ownerId = InputUtils.readLong(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " owner = " + GeneratorUtils.repoVariable(ownerClass) + ".findById(ownerId);");
        w.println("        if (owner == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        Long otherId = InputUtils.readLong(scanner, \"" + otherClass + " ID: \");");
        w.println("        " + otherClass + " other = " + GeneratorUtils.repoVariable(otherClass) + ".findById(otherId);");
        w.println("        if (other == null) { System.out.println(\"" + otherClass + " not found.\"); return; }");
        w.println();

        if (assoc.hasQualifier()) {
            // Map fazemos put com a chave sendo o qualifier do outro objeto
            // owner.getAlunos().put(other.getNumero(), other)
            String qualifierName = assoc.getQualifierName();
            String qualCap = GeneratorUtils.capitalizeFirst(qualifierName);
            w.println("        owner.get" + capitalized + "().put(other.get" + qualCap + "(), other);");
        } else {
            w.println("        owner.get" + capitalized + "().add(other);");
        }

        w.println("        " + GeneratorUtils.repoVariable(ownerClass) + ".save(owner);");
        w.println("        System.out.println(\"Added " + otherClass + " to " + ownerClass + ".\");");
        w.println("    }");
        w.println();
    }

    // remove da lista/map
    private void generateRemoveFromListMethod(PrintWriter w, String ownerClass, String otherClass, String fieldName, Association assoc) {
        String capitalized = GeneratorUtils.capitalizeFirst(fieldName);
        w.println("    private void remove" + capitalized + "() {");
        w.println("        Long ownerId = InputUtils.readLong(scanner, \"" + ownerClass + " ID: \");");
        w.println("        " + ownerClass + " owner = " + GeneratorUtils.repoVariable(ownerClass) + ".findById(ownerId);");
        w.println("        if (owner == null) { System.out.println(\"" + ownerClass + " not found.\"); return; }");
        w.println();
        w.println("        Long otherId = InputUtils.readLong(scanner, \"" + otherClass + " ID: \");");
        w.println("        " + otherClass + " other = " + GeneratorUtils.repoVariable(otherClass) + ".findById(otherId);");
        w.println("        if (other == null) { System.out.println(\"" + otherClass + " not found.\"); return; }");
        w.println();

        if (assoc.hasQualifier()) {
            // map remove pela chave
            String qualifierName = assoc.getQualifierName();
            String qualCap = GeneratorUtils.capitalizeFirst(qualifierName);
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
                    // primitivo: input direto
                    Attribute tmp = new Attribute();
                    tmp.setName(pName);
                    tmp.setType(pType);
                    generateReadInputForAttribute(w, tmp);
                }
                w.println();
            }

            // try/catch para que uma pré/pós-condição violada (IllegalStateException)
            // mostra o erro e volta ao menu, em vez de rebentar a app
            w.println("        try {");
            // retorno da op: capturado no tipo real (via entry/op, como o OperationGenerator) e mostrado
            // se não-void. Só owner é persistido — um return-entidade NÃO é guardado
            String call = "owner." + op.getName() + "(" + String.join(", ", argNames) + ")";
            String rt = (entry.getReturnType() != null && !entry.getReturnType().isBlank())
                    ? entry.getReturnType() : (op.getReturnType() != null ? op.getReturnType() : "void");
            boolean voidReturn = rt.isBlank() || "void".equals(rt);
            w.println("            " + (voidReturn ? "" : GeneratorUtils.removePackageNames(rt) + " ret = ") + call + ";");
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
            w.println("package menus;");
            w.println();
            w.println("import repository.*;");
            w.println("import util.InputUtils;");
            w.println("import java.util.*;");
            w.println("import java.time.*;");
            w.println();
            w.println("public class MainMenu {");
            w.println();
            w.println("    private Scanner scanner = new Scanner(System.in);");

            // um campo-repo por classe (inclui abstratas — outros menus precisam delas). pais primeiro:
            // o repo filho inicializa-se com o campo do repo do pai, que tem de já estar declarado acima.
            // Cada filho aponta só para a raiz (getRoot)
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
            List<Class> concrete = classes.stream().filter(c -> !c.isAbstract()).collect(Collectors.toList());

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
                // parseBoolean: tudo o que não for "true" assume-se false (sem validação nem retry)
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
                    // antes gerava entity.setX(String) para tipos não suportados -> não compilava
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
