package generator.spring;

import generator.GeneratorUtils;
import generator.RelationshipInfo;
import model.Association;
import model.Attribute;
import model.Class;
import model.Enumeration;
import model.Operation;
import model.sequence.Message;
import model.sequence.MessageArgument;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UI simples (HTML + JS vanilla) servida pelo próprio Spring a partir de
 * src/main/resources/static/. Uma página CRUD por entidade concreta, cada uma a
 * falar com os endpoints REST já gerados (fetch /api/&lt;plural&gt;). Uma sidebar
 * (links normais, sem router) liga as páginas. Sem build nem npm: correr a app e
 * abrir localhost:8080/.
 *
 * Relações single (ManyToOne) aparecem como &lt;select&gt; no formulário: o id
 * escolhido vai no campo &lt;campo&gt;Id do CreateDTO (o service liga a relação no
 * create, sem endpoint extra). Coleções (OneToMany/ManyToMany) têm uma secção
 * add/remove que chama os endpoints /api/&lt;plural&gt;/{id}/&lt;campo&gt;.
 *
 * O JS gerado é explícito (um campo por linha) para se ler como código à mão.
 */
public class SpringFrontendGenerator {
    private final String outputDir;
    // nome -> Class, para resolver o id (campo e tipo) da outra ponta das relações
    private final Map<String, Class> classMap = new HashMap<>();
    // nome do enum -> literais, para gerar <select> nos atributos de tipo enum
    private final Map<String, List<String>> enumLiterals = new HashMap<>();

    public SpringFrontendGenerator(String outputDir) {
        this.outputDir = outputDir;
    }

    public void generateFrontend(List<Class> classes, List<Association> associations,
                                 List<Enumeration> enumerations) throws IOException {
        System.out.println("Generating Spring UI (static)...");
        String staticDir = outputDir + "/src/main/resources/static";
        new File(staticDir).mkdirs();

        classMap.clear();
        for (Class c : classes) classMap.put(c.getName(), c);
        enumLiterals.clear();
        for (Enumeration e : enumerations) enumLiterals.put(e.getName(), e.getLiterals());

        // só geram página as classes com endpoint REST, concretas E com controller gerado
        List<Class> pages = classes.stream()
                .filter(c -> !c.isAbstract() && c.isGenerateController())
                .collect(Collectors.toList());

        writeStyle(staticDir);
        writeIndex(staticDir, pages);
        for (Class cls : pages) {
            generatePage(cls, associations, staticDir, pages);
        }
        System.out.println("UI generation completed.");
    }

    // mesma pluralização do controller: /api/<lowercase>(s)
    private String plural(String className) {
        String p = className.toLowerCase();
        return p.endsWith("s") ? p : p + "s";
    }

    private boolean isNumeric(String type) {
        switch (type) {
            case "int": case "Integer": case "long": case "Long":
            case "short": case "Short": case "byte": case "Byte":
            case "double": case "Double": case "float": case "Float":
                return true;
            default:
                return false;
        }
    }

    private boolean isBoolean(String type) {
        return "boolean".equals(type) || "Boolean".equals(type);
    }

    // tipos java.time que serializam como string ISO compatível com os inputs HTML
    // (LocalDate -> "2026-06-24", LocalDateTime -> "2026-06-24T15:30", LocalTime -> "15:30").
    // Instant/Date ficam de fora (zona/formato não batem com os inputs) -> input de texto.
    private boolean isDate(String type) {
        return "LocalDate".equals(type) || "LocalDateTime".equals(type) || "LocalTime".equals(type);
    }

    private String inputType(String type) {
        if (isNumeric(type)) return "number";
        if (isBoolean(type)) return "checkbox";
        switch (type) {
            case "LocalDate": return "date";
            case "LocalDateTime": return "datetime-local";
            case "LocalTime": return "time";
            default: return "text";
        }
    }

    // nome do campo de id no CreateDTO/ResponseDTO desta relação single (ex: "memberId")
    private String idField(RelationshipInfo rel) {
        return GeneratorUtils.makeAttributeName(rel.otherClass, rel.role, rel.multiplicity) + "Id";
    }

    private boolean hasApi(String className) {
        Class c = classMap.get(className);
        return c != null && !c.isAbstract() && c.isGenerateController();
    }
    // se id (Long/int) -> Number(); senão string (UUID/chave natural)
    private boolean idNumeric(String className) {
        return isNumeric(GeneratorUtils.idType(classMap.get(className)));
    }
    // nome do id na resposta json ("id" gerado, ou a chave natural)
    private String idJsonField(String className) {
        return GeneratorUtils.idFieldName(classMap.get(className));
    }

    // relações de coleção desta classe COM endpoint add/remove (mesma condição que o
    // SpringControllerGenerator: M:N do lado dono, ou O:M unidirecional/qualifier).
    // Replica o two-pass de auto-associação bidirecional do controller.
    private List<RelationshipInfo> collectionRelationships(Class cls, List<Association> associations) {
        List<RelationshipInfo> rels = new ArrayList<>();
        for (Association assoc : associations) {
            boolean isBidirectional = assoc.isSourceNavigable() && assoc.isTargetNavigable();
            String declarer = GeneratorUtils.assocDeclaringClass(cls, assoc);
            if (declarer == null) continue;
            boolean selfBidirectional = assoc.getSourceClass().equals(assoc.getTargetClass()) && isBidirectional;
            if (selfBidirectional) {
                addCollectionSide(rels, assoc, isBidirectional, true, declarer);
                addCollectionSide(rels, assoc, isBidirectional, false, declarer);
            } else {
                addCollectionSide(rels, assoc, isBidirectional, assoc.getSourceClass().equals(declarer), declarer);
            }
        }
        return rels;
    }

    private void addCollectionSide(List<RelationshipInfo> rels, Association assoc, boolean isBidirectional,
                                   boolean isSource, String declarer) {
        String thisMul = isSource ? assoc.getSourceMultiplicity() : assoc.getTargetMultiplicity();
        String otherMul = isSource ? assoc.getTargetMultiplicity() : assoc.getSourceMultiplicity();
        boolean thisMultiple = GeneratorUtils.checkMultiplicity(thisMul);
        boolean otherMultiple = GeneratorUtils.checkMultiplicity(otherMul);
        String otherClass = GeneratorUtils.otherClassOf(declarer, assoc);
        String role = isSource ? assoc.getTargetRole() : assoc.getSourceRole();

        boolean manyToMany = thisMultiple && otherMultiple && GeneratorUtils.isOwningSide(assoc, isSource);
        boolean oneToManyUni = (!thisMultiple || assoc.hasQualifier())
                && (otherMultiple || assoc.hasQualifier()) && !isBidirectional;
        if (!manyToMany && !oneToManyUni) return;

        rels.add(new RelationshipInfo(otherClass, role, otherMul, assoc, isSource));
    }

    // operações custom desta classe COM endpoint (mesma condição que o controller:
    // não-job e com diagrama de sequência ligado). Standalone não tem o link -> lista vazia.
    private List<Operation> customOperations(Class cls) {
        List<Operation> ops = new ArrayList<>();
        for (Operation op : GeneratorUtils.effectiveOperations(cls)) {
            if (op.isJob() || op.getSequenceDiagram() == null) continue;
            ops.add(op);
        }
        return ops;
    }

    // jobs desta classe tem endpoint de trigger no controller.
    // mesma scope que o SpringControllerGenerator (cls.getOperations(), não herdadas).
    private List<Operation> jobOperations(Class cls) {
        List<Operation> jobs = new ArrayList<>();
        for (Operation op : cls.getOperations()) {
            if (op.isJob()) jobs.add(op);
        }
        return jobs;
    }

    // cron do job (default igual ao SpringJobGenerator quando não definido)
    private String jobCron(Operation op) {
        return (op.getCron() != null && !op.getCron().isBlank()) ? op.getCron() : "0 * * * * *";
    }

    // parâmetros do endpoint = argumentos da mensagem de entrada (igual ao controller)
    private List<MessageArgument> opArgs(Class cls, Operation op) {
        Message entry = SpringUtils.findEntryMessage(op.getSequenceDiagram(),
                GeneratorUtils.declaringClassOf(cls, op), op);
        return entry != null ? entry.getArguments() : new ArrayList<>();
    }

    // metodo http (default POST, como o controller) e path
    private String opMethod(Operation op) {
        return (op.getHttpMethod() == null || op.getHttpMethod().isBlank())
                ? "POST" : op.getHttpMethod().trim().toUpperCase();
    }
    private String opPath(Operation op) {
        return (op.getEndpointName() != null && !op.getEndpointName().isBlank())
                ? op.getEndpointName() : op.getName();
    }

    // sidebar partilhada por todas as páginas: links normais (sem router)
    // current = entidade da página atual, para a marcar como ativa (null no index)
    private void writeSidebar(PrintWriter w, List<Class> pages, String current) {
        w.println("  <nav class=\"sidebar\">");
        w.println("    <a href=\"index.html\">Home</a>");
        for (Class c : pages) {
            String name = c.getName();
            String active = name.equals(current) ? " class=\"active\"" : "";
            w.println("    <a href=\"" + name.toLowerCase() + ".html\"" + active + ">" + name + "</a>");
        }
        w.println("  </nav>");
    }

    private void writeStyle(String staticDir) throws IOException {
        try (PrintWriter w = new PrintWriter(new File(staticDir + "/style.css"))) {
            w.println("* { box-sizing: border-box; }");
            w.println("body { margin: 0; font-family: system-ui, sans-serif; display: flex; }");
            w.println(".sidebar { width: 180px; min-height: 100vh; background: #1e293b; padding: 16px 0; }");
            w.println(".sidebar a { display: block; padding: 10px 20px; color: #cbd5e1; text-decoration: none; }");
            w.println(".sidebar a:hover { background: #334155; color: #fff; }");
            w.println(".sidebar a.active { background: #2563eb; color: #fff; }");
            w.println("main { flex: 1; padding: 24px 32px; }");
            w.println("h1 { margin-top: 0; }");
            w.println("form { margin-bottom: 24px; display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-end; }");
            w.println("label { display: flex; flex-direction: column; font-size: 13px; color: #475569; gap: 4px; }");
            w.println("input, select { padding: 6px 8px; border: 1px solid #cbd5e1; border-radius: 4px; }");
            w.println("button { padding: 6px 12px; border: none; border-radius: 4px; background: #2563eb; color: #fff; cursor: pointer; }");
            w.println("button:hover { background: #1d4ed8; }");
            w.println("table { border-collapse: collapse; width: 100%; }");
            w.println("th, td { border: 1px solid #e2e8f0; padding: 8px 10px; text-align: left; }");
            w.println("th { background: #f1f5f9; }");
            w.println("tr:nth-child(even) td { background: #f8fafc; }");
            w.println("td button { background: #dc2626; padding: 4px 8px; }");
            w.println("td button:hover { background: #b91c1c; }");
        }
    }

    private void writeIndex(String staticDir, List<Class> pages) throws IOException {
        try (PrintWriter w = new PrintWriter(new File(staticDir + "/index.html"))) {
            w.println("<!DOCTYPE html>");
            w.println("<html lang=\"en\">");
            w.println("<head><meta charset=\"utf-8\"><title>App</title><link rel=\"stylesheet\" href=\"style.css\"></head>");
            w.println("<body>");
            writeSidebar(w, pages, null);
            w.println("  <main>");
            w.println("    <h1>App</h1>");
            w.println("    <p>Select an entity from the sidebar.</p>");
            w.println("  </main>");
            w.println("</body>");
            w.println("</html>");
        }
    }

    private void generatePage(Class cls, List<Association> associations, String staticDir,
                              List<Class> pages) throws IOException {
        String name = cls.getName();
        String api = "/api/" + plural(name);
        String idName = cls.getRoot().getIdAttributeName();
        boolean defaultId = idName == null;

        // inputs do formulário (create) = atributos não-derivados, sem o id auto-gerado;
        // escondido do Create E do Update -> o form nunca o pode enviar, nem se renderiza
        List<Attribute> formFields = cls.getAllAttributes().stream()
                .filter(a -> !a.hasDerive())
                .filter(a -> !(cls.isHiddenInCreate(a.getName()) && cls.isHiddenInUpdate(a.getName())))
                .filter(a -> cls.isIdAssigned() || !a.getName().equals(idName))
                .collect(Collectors.toList());

        // colunas da tabela = id (se gerado) + atributos não-derivados visíveis na LISTA
        // (a tabela mostra o findAll -> segue o override do endpointType "list" quando existir)
        List<String> columns = new ArrayList<>();
        if (defaultId) columns.add("id");
        for (Attribute a : cls.getAllAttributes()) {
            boolean hiddenInList = cls.hasResponseOverride("list")
                    ? cls.responseOverrideHidden("list").contains(a.getName())
                    : cls.isHiddenInResponse(a.getName());
            if (!a.hasDerive() && !hiddenInList) columns.add(a.getName());
        }

        List<RelationshipInfo> rels = RelationshipInfo.collectSingle(cls, associations);
        List<RelationshipInfo> colls = collectionRelationships(cls, associations);
        List<Operation> ops = customOperations(cls);
        List<Operation> jobs = jobOperations(cls);
        boolean parentNumeric = idNumeric(name);   // id desta classe (pai nas coleções)

        try (PrintWriter w = new PrintWriter(new File(staticDir + "/" + name.toLowerCase() + ".html"))) {
            w.println("<!DOCTYPE html>");
            w.println("<html lang=\"en\">");
            w.println("<head><meta charset=\"utf-8\"><title>" + name + "</title><link rel=\"stylesheet\" href=\"style.css\"></head>");
            w.println("<body>");
            writeSidebar(w, pages, name);
            w.println("  <main>");
            w.println("    <h1>" + name + "</h1>");

            // formulário create/edit: o mesmo form serve para criar (POST) e editar (PUT);
            // o botão edit de cada linha carrega os valores e o submit passa a Update
            w.println("    <form onsubmit=\"save(event)\">");
            for (Attribute a : formFields) {
                // bloqueado no modo criar quando não faz parte do CreateDTO: init (valor obrigado)
                // ou escondido do Create; desbloqueia no modo editar se existir no UpdateDTO
                String locked = (a.getInitExpression() != null || cls.isHiddenInCreate(a.getName()))
                        ? " disabled title=\"not part of create\"" : "";
                if (enumLiterals.containsKey(a.getType())) {
                    // atributo enum -> <select> com os literais (o valor é o nome da constante)
                    StringBuilder opts = new StringBuilder("<option value=\"\">-- none --</option>");
                    for (String lit : enumLiterals.get(a.getType())) opts.append("<option>").append(lit).append("</option>");
                    w.println("      <label>" + a.getName()
                            + " <select name=\"" + a.getName() + "\"" + locked + ">" + opts + "</select></label>");
                } else {
                    w.println("      <label>" + a.getName()
                            + " <input name=\"" + a.getName() + "\" type=\"" + inputType(a.getType()) + "\"" + locked + "></label>");
                }
            }
            for (RelationshipInfo rel : rels) {
                String field = idField(rel);
                if (hasApi(rel.otherClass)) {
                    w.println("      <label>" + field + " <select name=\"" + field + "\"></select></label>");
                } else {
                    // sem endpoint (alvo abstrato ou controller desligado): não há lista -> escreve-se o id
                    w.println("      <label>" + field + " <input name=\"" + field
                            + "\" type=\"" + (idNumeric(rel.otherClass) ? "number" : "text") + "\"></label>");
                }
            }
            w.println("      <button type=\"submit\" id=\"submitBtn\">Create</button>");
            w.println("      <button type=\"button\" id=\"cancelBtn\" onclick=\"cancelEdit()\" style=\"display:none\">Cancel</button>");
            w.println("    </form>");

            // tabela
            w.println("    <p> a tabela e os selects de relação mostram só a");
            w.println("         1ª página do findAll </p>");
            w.println("    <table id=\"list\">");
            w.print("      <thead><tr>");
            for (String c : columns) w.print("<th>" + c + "</th>");
            for (RelationshipInfo rel : rels) w.print("<th>" + idField(rel) + "</th>");
            for (RelationshipInfo coll : colls) {
                w.print("<th>" + GeneratorUtils.makeAttributeName(coll.otherClass, coll.role, coll.multiplicity) + "Ids</th>");
            }
            w.println("<th></th></tr></thead>");
            w.println("      <tbody></tbody>");
            w.println("    </table>");

            // secção de coleções: cada coleção é um <form> que leva o endpoint em data-path
            // (e data-numeric p/ coagir o id); addItem/removeItem leem esses atributos, por isso
            // não há função por coleção. Escolhe-se o id do pai (esta entidade) e o filho a ligar.
            if (!colls.isEmpty()) {
                w.println("    <h2>Relationships</h2>");
                for (RelationshipInfo coll : colls) {
                    String field = GeneratorUtils.makeAttributeName(coll.otherClass, coll.role, coll.multiplicity);
                    boolean numeric = idNumeric(coll.otherClass);
                    w.println("    <form class=\"rel\" data-path=\"" + field.toLowerCase()
                            + "\" data-numeric=\"" + numeric + "\" onsubmit=\"addItem(event)\">");
                    w.println("      <label>" + name + " id <input name=\"parent\" type=\""
                            + (parentNumeric ? "number" : "text") + "\"></label>");
                    if (hasApi(coll.otherClass)) {
                        w.println("      <label>" + field + " <select name=\"child\"></select></label>");
                    } else {
                        w.println("      <label>" + field + " <input name=\"child\" type=\""
                                + (numeric ? "number" : "text") + "\"></label>");
                    }
                    w.println("      <button type=\"submit\">Add</button>");
                    w.println("      <button type=\"button\" onclick=\"removeItem(this)\">Remove</button>");
                    w.println("    </form>");
                }
            }

            // secção de operações custom: um form por op (params como query string), botão = nome da op.
            // runOp lê data-path/data-method, por isso não há função por operação.
            if (!ops.isEmpty()) {
                w.println("    <h2>Operations</h2>");
                for (Operation op : ops) {
                    w.println("    <form class=\"op\" data-path=\"" + opPath(op) + "\" data-method=\""
                            + opMethod(op) + "\" onsubmit=\"runOp(event)\">");
                    for (MessageArgument arg : opArgs(cls, op)) {
                        String type = GeneratorUtils.removePackageNames(arg.getType() != null ? arg.getType() : "Object");
                        w.println("      <label>" + arg.getName() + " <input name=\"" + arg.getName()
                                + "\" type=\"" + inputType(type) + "\"></label>");
                    }
                    w.println("      <button type=\"submit\">" + op.getName() + "</button>");
                    w.println("    </form>");
                }
            }

            // correm @Scheduled mas têm endpoint de trigger -> botão "Run".
            // mostra o cron só como info (read-only). runJob faz POST /api/<plural>/<op>.
            if (!jobs.isEmpty()) {
                w.println("    <h2>Scheduled Jobs</h2>");
                for (Operation op : jobs) {
                    w.println("    <form class=\"job\" data-path=\"" + opPath(op) + "\" onsubmit=\"runJob(event)\">");
                    w.println("      <span>" + op.getName() + " <small>(" + jobCron(op) + ")</small></span>");
                    w.println("      <button type=\"submit\">Run</button>");
                    w.println("    </form>");
                }
            }
            w.println("  </main>");

            // ---- script (explícito, um campo por linha) ----
            w.println("<script>");
            w.println("const API = '" + api + "';");
            w.println("let editId = null;   // null = a criar (POST); senão o id a editar (PATCH)");
            w.println();
            w.println("async function load() {");
            w.println("  const data = await (await fetch(API)).json();");
            w.println("  const items = data.content ?? data;  // Page -> content; plain array -> itself");
            w.println("  const tbody = document.querySelector('#list tbody');");
            w.println("  tbody.innerHTML = '';");
            w.println("  for (const item of items) {");
            w.println("    const row = document.createElement('tr');");
            w.println("    row.innerHTML = `");
            for (String c : columns) {
                w.println("      <td>${item." + c + " ?? ''}</td>");
            }
            for (RelationshipInfo rel : rels) {
                w.println("      <td>${item." + idField(rel) + " ?? ''}</td>");
            }
            for (RelationshipInfo coll : colls) {
                w.println("      <td>${item." + GeneratorUtils.makeAttributeName(coll.otherClass, coll.role, coll.multiplicity) + "Ids ?? ''}</td>");
            }
            // id da linha: "id" gerado, ou a chave natural (que não aparece como "id" no json).
            // Entre aspas para funcionar com id numérico (Long) e string (UUID/chave natural).
            String selfId = defaultId ? "id" : idName;
            w.println("      <td><button onclick=\"edit('${item." + selfId + "}')\">edit</button> <button onclick=\"remove('${item." + selfId + "}')\">delete</button></td>`;");
            w.println("    tbody.appendChild(row);");
            w.println("  }");
            w.println("}");
            w.println();

            // selects das relações (só alvos concretos): preenchidos com a lista da outra entidade.
            // single -> [name="<field>"]; coleção -> o child do form com aquele data-path
            boolean hasSelect = rels.stream().anyMatch(r -> hasApi(r.otherClass))
                    || colls.stream().anyMatch(r -> hasApi(r.otherClass));
            if (hasSelect) {
                w.println("async function loadOptions() {");
                for (RelationshipInfo rel : rels) {
                    if (!hasApi(rel.otherClass)) continue;
                    w.println("  fillSelect('[name=\"" + idField(rel) + "\"]', '/api/" + plural(rel.otherClass)
                            + "', '" + idJsonField(rel.otherClass) + "');");
                }
                for (RelationshipInfo coll : colls) {
                    if (!hasApi(coll.otherClass)) continue;
                    String path = GeneratorUtils.makeAttributeName(coll.otherClass, coll.role, coll.multiplicity).toLowerCase();
                    w.println("  fillSelect('form[data-path=\"" + path + "\"] [name=\"child\"]', '/api/"
                            + plural(coll.otherClass) + "', '" + idJsonField(coll.otherClass) + "');");
                }
                w.println("}");
                w.println();
                w.println("async function fillSelect(selector, url, idField) {");
                w.println("  const data = await (await fetch(url)).json();");
                w.println("  const items = data.content ?? data;");
                w.println("  const select = document.querySelector(selector);");
                w.println("  select.innerHTML = '<option value=\"\">-- none --</option>';");
                w.println("  for (const item of items) {");
                w.println("    const opt = document.createElement('option');");
                w.println("    opt.value = item[idField];");
                w.println("    opt.textContent = item[idField];");
                w.println("    select.appendChild(opt);");
                w.println("  }");
                w.println("}");
                w.println();
            }

            // submit do form: POST se a criar, PATCH /{editId} se a editar (atualização parcial)
            w.println("async function save(event) {");
            w.println("  event.preventDefault();");
            w.println("  const form = event.target;");
            w.println("  const body = {");
            for (Attribute a : formFields) {
                String field = a.getName();
                String value = isNumeric(a.getType()) ? "Number(form." + field + ".value)"
                        : isBoolean(a.getType()) ? "form." + field + ".checked"
                        // data vazia -> null (uma string vazia rebenta o parse de LocalDate/etc no backend)
                        : isDate(a.getType()) ? "form." + field + ".value || null"
                        : "form." + field + ".value";
                w.println("    " + field + ": " + value + ",");
            }
            for (RelationshipInfo rel : rels) {
                // select vazio -> null (não ligar); senão o id (Number para ids numéricos)
                String field = idField(rel);
                String value = idNumeric(rel.otherClass)
                        ? "form." + field + ".value ? Number(form." + field + ".value) : null"
                        : "form." + field + ".value || null";
                w.println("    " + field + ": " + value + ",");
            }
            w.println("  };");
            // campos fora do CreateDTO (init / escondidos do Create) só vão no PATCH;
            // campos escondidos do Update só vão no POST
            for (Attribute a : formFields) {
                if (a.getInitExpression() != null || cls.isHiddenInCreate(a.getName())) {
                    w.println("  if (editId === null) delete body." + a.getName() + ";");
                }
                if (cls.isHiddenInUpdate(a.getName())) {
                    w.println("  if (editId !== null) delete body." + a.getName() + ";");
                }
            }
            w.println("  const method = editId === null ? 'POST' : 'PATCH';");
            w.println("  const url = editId === null ? API : `${API}/${editId}`;");
            w.println("  const res = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });");
            w.println("  if (!res.ok) {");
            w.println("    // o backend devolve ProblemDetail (RFC 7807): a mensagem (validação OCL, etc.) vem em 'detail'");
            w.println("    const err = await res.json().catch(() => null);");
            w.println("    alert(err && (err.detail || err.error) ? (err.detail || err.error) : 'Save failed: ' + res.status);");
            w.println("    return;");
            w.println("  }");
            // relações single: o PATCH ignora null (não limpa); se o select ficou vazio na edição,
            // limpa a relação pelo endpoint dedicado DELETE /{id}/{role}
            if (!rels.isEmpty()) {
                w.println("  if (editId !== null) {");
                for (RelationshipInfo rel : rels) {
                    String rolePath = GeneratorUtils.makeAttributeName(rel.otherClass, rel.role, rel.multiplicity).toLowerCase();
                    w.println("    if (!form." + idField(rel) + ".value) await fetch(`${API}/${editId}/" + rolePath + "`, { method: 'DELETE' });");
                }
                w.println("  }");
            }
            w.println("  cancelEdit();   // limpa o form e volta ao modo criar");
            w.println("  load();");
            w.println("}");
            w.println();

            // edit: carrega a entidade no form e entra em modo edição
            w.println("async function edit(id) {");
            w.println("  const item = await (await fetch(`${API}/${id}`)).json();");
            w.println("  const form = document.querySelector('form');");
            for (Attribute a : formFields) {
                String field = a.getName();
                if (isBoolean(a.getType())) {
                    w.println("  form." + field + ".checked = !!item." + field + ";");
                } else {
                    w.println("  form." + field + ".value = item." + field + " ?? '';");
                }
            }
            for (RelationshipInfo rel : rels) {
                String field = idField(rel);
                w.println("  form." + field + ".value = item." + field + " ?? '';");
            }
            // modo editar: destranca o que faz parte do UpdateDTO, tranca o que não faz
            for (Attribute a : formFields) {
                if (cls.isHiddenInUpdate(a.getName())) {
                    w.println("  form." + a.getName() + ".disabled = true;");
                } else if (a.getInitExpression() != null || cls.isHiddenInCreate(a.getName())) {
                    w.println("  form." + a.getName() + ".disabled = false;");
                }
            }
            w.println("  editId = id;");
            w.println("  document.getElementById('submitBtn').textContent = 'Update';");
            w.println("  document.getElementById('cancelBtn').style.display = '';");
            w.println("}");
            w.println();

            // cancelEdit: volta ao modo criar (limpa o form e o estado)
            w.println("function cancelEdit() {");
            w.println("  const form = document.querySelector('form');");
            w.println("  form.reset();");
            // de volta ao modo criar: tranca o que não faz parte do CreateDTO, destranca o resto
            for (Attribute a : formFields) {
                if (a.getInitExpression() != null || cls.isHiddenInCreate(a.getName())) {
                    w.println("  form." + a.getName() + ".disabled = true;");
                } else if (cls.isHiddenInUpdate(a.getName())) {
                    w.println("  form." + a.getName() + ".disabled = false;");
                }
            }
            w.println("  editId = null;");
            w.println("  document.getElementById('submitBtn').textContent = 'Create';");
            w.println("  document.getElementById('cancelBtn').style.display = 'none';");
            w.println("}");
            w.println();
            w.println("async function remove(id) {");
            w.println("  if (!confirm('Delete ' + id + '?')) return;");
            w.println("  await fetch(`${API}/${id}`, { method: 'DELETE' });");
            w.println("  load();");
            w.println("}");
            w.println();

            // coleções: add/remove genéricos. O endpoint vem do data-path do form, o id do
            // ADD (POST) manda [childId] no body; REMOVE (DELETE)
            // manda os ids na query (?ids=), porque um body em DELETE pode ser ignorado.
            if (!colls.isEmpty()) {
                w.println("async function addItem(event) {");
                w.println("  event.preventDefault();");
                w.println("  await sendItem(event.target, 'POST');");
                w.println("}");
                w.println();
                w.println("async function removeItem(btn) {");
                w.println("  await sendItem(btn.closest('form'), 'DELETE');");
                w.println("}");
                w.println();
                w.println("async function sendItem(form, method) {");
                w.println("  const child = form.dataset.numeric === 'true' ? Number(form.child.value) : form.child.value;");
                w.println("  const base = `${API}/${form.parent.value}/${form.dataset.path}`;");
                w.println("  // DELETE: ids na query (?ids=...); POST: id no body)");
                w.println("  const res = method === 'DELETE'");
                w.println("    ? await fetch(`${base}?ids=${child}`, { method })");
                w.println("    : await fetch(base, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify([child]) });");
                w.println("  if (!res.ok) {");
                w.println("    const err = await res.json().catch(() => null);");
                w.println("    alert(err && (err.detail || err.error) ? (err.detail || err.error) : method + ' failed: ' + res.status);");
                w.println("    return;");
                w.println("  }");
                w.println("  load();");
                w.println("}");
                w.println();
            }

            // operações custom: chama /api/<plural>/<op> com os params em query string e mostra o resultado
            if (!ops.isEmpty()) {
                w.println("async function runOp(event) {");
                w.println("  event.preventDefault();");
                w.println("  const form = event.target;");
                w.println("  const params = new URLSearchParams();");
                w.println("  for (const el of form.elements) {");
                w.println("    if (el.name) params.set(el.name, el.type === 'checkbox' ? el.checked : el.value);");
                w.println("  }");
                w.println("  const res = await fetch(`${API}/${form.dataset.path}?${params}`, { method: form.dataset.method });");
                w.println("  const text = await res.text();");
                w.println("  if (!res.ok) {");
                w.println("    let msg = text; try { const j = JSON.parse(text); msg = j.detail ?? j.error ?? text; } catch {}");
                w.println("    alert('Operation failed: ' + msg);");
                w.println("    return;");
                w.println("  }");
                w.println("  alert(text || 'OK');");
                w.println("  load();");
                w.println("}");
                w.println();
            }

            // jobs: dispara o trigger (POST /api/<plural>/<op>, sem params) e atualiza a lista
            if (!jobs.isEmpty()) {
                w.println("async function runJob(event) {");
                w.println("  event.preventDefault();");
                w.println("  const res = await fetch(`${API}/${event.target.dataset.path}`, { method: 'POST' });");
                w.println("  if (!res.ok) {");
                w.println("    const err = await res.json().catch(() => null);");
                w.println("    alert(err && (err.detail || err.error) ? (err.detail || err.error) : 'Job failed: ' + res.status);");
                w.println("    return;");
                w.println("  }");
                w.println("  alert('Job ran');");
                w.println("  load();");
                w.println("}");
                w.println();
            }

            if (hasSelect) w.println("loadOptions();");
            w.println("load();");
            w.println("</script>");
            w.println("</body>");
            w.println("</html>");
        }
        System.out.println("Generated UI: " + name.toLowerCase() + ".html");
    }
}
