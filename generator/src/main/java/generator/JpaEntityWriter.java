package generator;

import model.Association;
import model.Attribute;
import model.Class;

import java.io.PrintWriter;

/**
 * Anotações JPA de entidade iguais no Spring e no CLI-JPA porque os dois usam Jakarta Persistence,
 * por isso @Id/@ManyToOne/@OneToMany/@ManyToMany/@OneToOne/@MapKey saem iguais
 *
 * Fica de fora o que depende do target (Bean Validation do OCL, @JsonIgnoreProperties do
 * Jackson,  Lombok)  isso sai em cada gerador antes de chamar estes writers.
 */

public final class JpaEntityWriter {

    private JpaEntityWriter() { }

    // id basico, só qnd não se define um id custom
    // se houver id custom, o @Id é colocado no atributo escolhido em generateAttributes
    public static void generateId(PrintWriter writer, Class cls) {
        if (cls.getIdAttributeName() != null) return;
        // id auto gerado Long (default) ou UUID gerado
        String idType = cls.getRoot().getIdAttributeType();
        writer.println("    @Id");
        if ("UUID".equals(idType)) {
            writer.println("    @GeneratedValue(strategy = GenerationType.UUID)");
            writer.println("    private UUID id;");
        } else {
            writer.println("    @GeneratedValue(strategy = GenerationType.IDENTITY)");
            writer.println("    private Long id;");
        }
        writer.println();
    }

    public static void generateDerivedGetters(PrintWriter writer, Class cls) {
        for (Attribute atr : cls.getAttributes()) {
            if (!atr.hasDerive()) continue;
            String cap = GeneratorUtils.capitalizeFirst(atr.getName());
            writer.println("    public " + atr.getAttributeType() + " get" + cap + "() {");
            writer.println("        return " + atr.getJavaDeriveExpression() + ";");
            writer.println("    }");
            writer.println();
        }
    }

    /**
     * @ManyToMany: bidirecional: dono tem @JoinTable, inverso tem mappedBy
     * unidirecional: @JoinTable
     * Com qualifier nesta ponta: @MapKey e Map<qualifier, otherClass>
     */
    public static void generateManyToMany(PrintWriter writer,
                                          String thisClass,
                                          String otherClass,
                                          String attributeName,
                                          Association assoc,
                                          boolean isSource,
                                          boolean isBidirectional) {

        // numa auto associação as duas colunas seriam iguais ("employee_id") entao usa-se o campo na inversa
        String invJoinCol = (thisClass.equals(otherClass) ? attributeName.toLowerCase() : otherClass.toLowerCase()) + "_id";

        // só o lado inverso (bidirecional não dono) leva mappedBy
        // tudo o resto (dono ou unidirecional) leva @JoinTable
        boolean mappedBy = isBidirectional && !GeneratorUtils.isOwningSide(assoc, isSource);
        if (mappedBy) {
            writer.println("    @ManyToMany(mappedBy = \"" +
                    GeneratorUtils.getOtherSideAttributeName(assoc, isSource) + "\")");
        } else {
            writer.println("    @ManyToMany");
            writer.println("    @JoinTable(");
            writer.println("        name = \"" + thisClass.toLowerCase() + "_" + attributeName.toLowerCase() + "\",");
            writer.println("        joinColumns = @JoinColumn(name = \"" + thisClass.toLowerCase() + "_id\"),");
            writer.println("        inverseJoinColumns = @JoinColumn(name = \"" + invJoinCol + "\")");
            writer.println("    )");
        }

        // numa M:N as duas pontas são coleções, por isso é preciso saber qual tem o qualifier
        if (assoc.isMapEnd(isSource)) {
            writer.println("    @MapKey(name = \"" + assoc.getQualifierName() + "\")");
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " +
                    attributeName + " = new HashMap<>();");
        } else {
            writer.println("    private List<" + otherClass + "> " + attributeName +
                    " = new ArrayList<>();");
        }
    }

    /**
     * @OneToMany: se for bidirecional, tem mappedBy, senão tem @JoinColumn
     * se for composição, tem cascade all e orphanRemoval
     * se assoc tiver qualifier, é Map<qualifier, otherClass>, senão é List<otherClass>
     */
    public static void generateOneToMany(PrintWriter writer,
                                         String thisClass,
                                         String otherClass,
                                         String attributeName,
                                         Association assoc,
                                         boolean isSource,
                                         boolean isBidirectional) {

        boolean isComposite = "composite".equals(assoc.getAggregationType());
        String cascade = isComposite ? ", cascade = CascadeType.ALL, orphanRemoval = true" : "";

        if (isBidirectional) {
            writer.println("    @OneToMany(mappedBy = \"" +
                    GeneratorUtils.getOtherSideAttributeName(assoc, isSource) + "\"" + cascade + ")");
        } else {
            if (isComposite) {
                writer.println("    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)");
            } else {
                writer.println("    @OneToMany");
            }

            // @OneToMany unidirecional: a FK fica na tabela do outro lado e aponta para esta classe
            writer.println("    @JoinColumn(name = \"" + thisClass.toLowerCase() + "_" + attributeName.toLowerCase() + "_id\")");
        }

        if (assoc.isMapEnd(isSource)) {
            writer.println("    @MapKey(name = \"" + assoc.getQualifierName() + "\")");
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " +
                    attributeName + " = new HashMap<>();");
        } else {
            writer.println("    private List<" + otherClass + "> " + attributeName +
                    " = new ArrayList<>();");
        }
    }

    /**
     * @ManyToOne com @JoinColumn
     * o lado singular nunca é Map
     */
    public static void generateManyToOne(PrintWriter writer,
                                         String otherClass,
                                         String attributeName,
                                         Association assoc,
                                         String fetch) {
        // Ponta sing = referência única. O Map pertence só ao lado da coleção
        // aqui ignora-se, senão saía @ManyToOne sobre um Map (inválido)
        writer.println("    @ManyToOne(fetch = FetchType." + fetch + ")");
        writer.println("    @JoinColumn(name = \"" + attributeName.toLowerCase() + "_id\")");
        writer.println("    private " + otherClass + " " + attributeName + ";");
    }

    /**
     * @OneToOne: se tiver qualifier, é OneToMany, senão é OneToOne normal
     * sem qualifie: owner tem @JoinColumn, inverse tem mappedBy
     * unidirecional: @JoinColumn
     */
    public static void generateOneToOne(PrintWriter writer,
                                        String thisClass,
                                        String otherClass,
                                        String attributeName,
                                        Association assoc,
                                        boolean isSource,
                                        boolean isBidirectional,
                                        String fetch) {
        // tem qualifier meste ponta, é OneToMany
        // Map como coleção, nunca no ramo 1:1
        if (assoc.isMapEnd(isSource)) {
            if (isBidirectional) {
                writer.println("    @OneToMany(mappedBy = \"" +
                        GeneratorUtils.getOtherSideAttributeName(assoc, isSource) + "\")");
            } else {
                writer.println("    @OneToMany");
                writer.println("    @JoinColumn(name = \"" + attributeName.toLowerCase() + "_id\")");
            }

            writer.println("    @MapKey(name = \"" + assoc.getQualifierName() + "\")");
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " +
                    attributeName + " = new HashMap<>();");
        } else {
            // sem qualifier OneToOne normal
            // composição: cascade/orphanRemoval só no lado do todoo (delete o todoo apaga a "parte")
            boolean cascadeHere = assoc.cascadesFrom(isSource);
            String cascade = cascadeHere ? ", cascade = CascadeType.ALL, orphanRemoval = true" : "";
            if (isBidirectional) {
                // decide se o campo gerado é @OneToOne (dono) ou mappedBy (inverso)
                boolean isOwner = GeneratorUtils.isOwningSide(assoc, isSource);

                if (isOwner) {
                    writer.println("    @OneToOne(fetch = FetchType." + fetch + cascade + ")");
                    writer.println("    @JoinColumn(name = \"" + attributeName.toLowerCase() + "_id\")");
                } else {
                    writer.println("    @OneToOne(mappedBy = \"" +
                            GeneratorUtils.getOtherSideAttributeName(assoc, isSource) + "\", fetch = FetchType." + fetch + cascade + ")");
                }
            } else {
                writer.println("    @OneToOne(fetch = FetchType." + fetch + cascade + ")");
                writer.println("    @JoinColumn(name = \"" + attributeName.toLowerCase() + "_id\")");
            }

            writer.println("    private " + otherClass + " " + attributeName + ";");
        }
    }
}
