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
            writer.println("    public " + atr.getType() + " get" + cap + "() {");
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
                                          String fieldName,
                                          Association assoc,
                                          boolean isSource,
                                          boolean isBidirectional) {

        // numa auto associação as duas colunas seriam iguais ("employee_id") entao usa-se o campo na inversa
        String invJoinCol = (thisClass.equals(otherClass) ? fieldName.toLowerCase() : otherClass.toLowerCase()) + "_id";

        if (isBidirectional) {
            // decide se o campo gerado é @JoinTable (dono) ou mappedBy (inverso)
            boolean isOwner = GeneratorUtils.isOwningSide(assoc, isSource);

            if (isOwner) {
                writer.println("    @ManyToMany");
                writer.println("    @JoinTable(");
                writer.println("        name = \"" + thisClass.toLowerCase() + "_" + fieldName.toLowerCase() + "\",");
                writer.println("        joinColumns = @JoinColumn(name = \"" + thisClass.toLowerCase() + "_id\"),");
                writer.println("        inverseJoinColumns = @JoinColumn(name = \"" + invJoinCol + "\")");
                writer.println("    )");
            } else {
                writer.println("    @ManyToMany(mappedBy = \"" +
                        GeneratorUtils.getOtherSideAttributeName(assoc, isSource ? "target" : "source") + "\")");
            }
        } else {
            writer.println("    @ManyToMany");
            writer.println("    @JoinTable(");
            writer.println("        name = \"" + thisClass.toLowerCase() + "_" + fieldName.toLowerCase() + "\",");
            writer.println("        joinColumns = @JoinColumn(name = \"" + thisClass.toLowerCase() + "_id\"),");
            writer.println("        inverseJoinColumns = @JoinColumn(name = \"" + invJoinCol + "\")");
            writer.println("    )");
        }

        // numa M:N as duas pontas são coleções, por isso é preciso saber qual tem o qualifier
        if (assoc.isMapEnd(isSource)) {
            writer.println("    @MapKey(name = \"" + assoc.getQualifierName() + "\")");
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " +
                    fieldName + " = new HashMap<>();");
        } else {
            writer.println("    private List<" + otherClass + "> " + fieldName +
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
                                         String fieldName,
                                         Association assoc,
                                         boolean isSource,
                                         boolean isBidirectional) {

        boolean isComposite = "composite".equals(assoc.getAggregationType());
        String cascade = isComposite ? ", cascade = CascadeType.ALL, orphanRemoval = true" : "";

        if (isBidirectional) {
            writer.println("    @OneToMany(mappedBy = \"" +
                    GeneratorUtils.getOtherSideAttributeName(assoc, isSource ? "target" : "source") + "\"" + cascade + ")");
        } else {
            if (isComposite) {
                writer.println("    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)");
            } else {
                writer.println("    @OneToMany");
            }
            writer.println("    @JoinColumn(name = \"" + fieldName.toLowerCase() + "_id\")");
        }

        if (assoc.isMapEnd(isSource)) {
            writer.println("    @MapKey(name = \"" + assoc.getQualifierName() + "\")");
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " +
                    fieldName + " = new HashMap<>();");
        } else {
            writer.println("    private List<" + otherClass + "> " + fieldName +
                    " = new ArrayList<>();");
        }
    }

    /**
     * @ManyToOne com @JoinColumn
     * o lado singular nunca é Map
     */
    public static void generateManyToOne(PrintWriter writer,
                                         String otherClass,
                                         String fieldName,
                                         Association assoc) {

        // Ponta sing = referência única. O Map pertence só ao lado da coleção
        // aqui ignora-se, senão saía @ManyToOne sobre um Map (inválido).
        writer.println("    @ManyToOne");
        writer.println("    @JoinColumn(name = \"" + fieldName.toLowerCase() + "_id\")");
        writer.println("    private " + otherClass + " " + fieldName + ";");
    }

    /**
     * @OneToOne: se tiver qualifier, é OneToMany, senão é OneToOne normal
     * sem qualifie: owner tem @JoinColumn, inverse tem mappedBy
     * unidirecional: @JoinColumn
     */
    public static void generateOneToOne(PrintWriter writer,
                                        String thisClass,
                                        String otherClass,
                                        String fieldName,
                                        Association assoc,
                                        boolean isSource,
                                        boolean isBidirectional) {

        // tem qualifier meste ponta, é OneToMany.
        // Map como coleção, nunca no ramo 1:1
        if (assoc.isMapEnd(isSource)) {
            if (isBidirectional) {
                writer.println("    @OneToMany(mappedBy = \"" +
                        GeneratorUtils.getOtherSideAttributeName(assoc, isSource ? "target" : "source") + "\")");
            } else {
                writer.println("    @OneToMany");
                writer.println("    @JoinColumn(name = \"" + fieldName.toLowerCase() + "_id\")");
            }

            writer.println("    @MapKey(name = \"" + assoc.getQualifierName() + "\")");
            String qualifierType = assoc.getQualifierType();
            writer.println("    private Map<" + qualifierType + ", " + otherClass + "> " +
                    fieldName + " = new HashMap<>();");
        } else {
            // sem qualifier OneToOne normal
            if (isBidirectional) {
                // decide se o campo gerado é @OneToOne (dono) ou mappedBy (inverso)
                boolean isOwner = GeneratorUtils.isOwningSide(assoc, isSource);

                if (isOwner) {
                    writer.println("    @OneToOne");
                    writer.println("    @JoinColumn(name = \"" + fieldName.toLowerCase() + "_id\")");
                } else {
                    writer.println("    @OneToOne(mappedBy = \"" +
                            GeneratorUtils.getOtherSideAttributeName(assoc, isSource ? "target" : "source") + "\")");
                }
            } else {
                writer.println("    @OneToOne");
                writer.println("    @JoinColumn(name = \"" + fieldName.toLowerCase() + "_id\")");
            }

            writer.println("    private " + otherClass + " " + fieldName + ";");
        }
    }
}
