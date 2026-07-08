package app.service;

import app.dto.AnnotationsDTO;
import app.dto.ParseResponse;
import app.dto.ProjectSettingsDTO;
import model.ProjectSettings;
import app.dto.dclass.*;
import app.dto.dsequence.*;
import generator.GeneratorUtils;
import generator.RelationshipInfo;
import generator.spring.SpringEndpointList;
import model.*;
import model.Class;
import model.Interface;
import model.sequence.*;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import service.GenerationService;
import service.ParsingService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ToolService {
    // para ja suportar 1, melhor par test
    private ParsedModel cachedModel;
    private final ParsingService parsingService = new ParsingService();
    private final GenerationService generationService = new GenerationService();
    private final AnnotationService annotationService = new AnnotationService();

    public byte[] generate(String target, AnnotationsDTO annotations, ProjectSettingsDTO settingsDto) throws Exception {
        if (cachedModel == null) {
            throw new IllegalStateException("No model parsed yet. Upload an XMI first.");
        }
        annotationService.apply(cachedModel, annotations);

        ProjectSettings opts = null;
        if (settingsDto != null) {
            opts = new ProjectSettings();
            opts.setBasePackage(settingsDto.getBasePackage());
            opts.setApplicationName(settingsDto.getApplicationName());
            opts.setDatabaseType(settingsDto.getDatabaseType());
            opts.setServerPort(settingsDto.getServerPort());
            opts.setOpenApi(settingsDto.getOpenApi());
            opts.setDockerfile(settingsDto.getDockerfile());
            opts.setTimestamps(settingsDto.getTimestamps());
            opts.setDemoFrontend(settingsDto.getDemoFrontend());
        }

        Path tempDir = Files.createTempDirectory("uml-gen-");
        try {
            generationService.generate(cachedModel, target, tempDir.toString(), opts);
            return zipDirectory(tempDir);
        } finally {
            FileSystemUtils.deleteRecursively(tempDir);
        }
    }

    public ParseResponse parse(String xmiFile) throws Exception{
        ParsedModel model = parsingService.parse(xmiFile);
        this.cachedModel = model;
        return toResponse(model);
        
    }

    private ParseResponse toResponse(ParsedModel model) {
        List<ClassDTO> classes = model.getClasses().stream()
                .map(c -> toClassDto(c, model.getAssociations()))
                .toList();

        List<AssociationDTO> associations = model.getAssociations().stream()
                .map(this::toAssociationDto)
                .toList();

        List<Interaction> allInteractions = new ArrayList<>();
        //allInteractions.addAll(model.getPimInteractions());
       // allInteractions.addAll(model.getPsmInteractions());
        allInteractions.addAll(model.getAllInteractions());
        List<InteractionDTO> interactions = allInteractions.stream()
                .map(this::toInteractionDTO)
                .toList();

        List<EnumerationDTO> enumerations = new ArrayList<>();
        for (Enumeration e : model.getEnumerations()) {
            enumerations.add(new EnumerationDTO(e.getName(), e.getLiterals()));
        }

        List<InterfaceDTO> interfaces = model.getInterfaces().stream()
                .map(this::toInterfaceDto).toList();

        return new ParseResponse(classes, associations, interactions, enumerations, interfaces);    }

    /**
     * MOVER DEPOIS PARA MAPPERSW

     */
    private ClassDTO toClassDto(Class c, List<Association> associations) {
        String parent = c.getParentClass() != null ? c.getParentClass().getName() : null;
        List<AttributeDTO> attrs = c.getAttributes().stream()
                .map(this::toAttributeDto)
                .toList();

        List<OperationDTO> ops = c.getOperations().stream()
                .map(o -> toOperationDTO(c.getName(), o))
                .toList();

        List<String> realizations = c.getRealizations().stream()
                .map(Interface::getName).toList();

        // endpoints REST previstos para o target Spring (CRUD + relações)
        List<SpringEndpointList.Endpoint> endpoints = SpringEndpointList.getClassEndpoints(c, associations);

        // relações próprias (declaradas nesta classe): filtra as herdadas (thisClass != c) da lista completa
        List<RelationFieldDTO> relations = RelationshipInfo.collect(c, associations).stream()
                .filter(rel -> rel.thisClass().equals(c.getName()))
                .map(rel -> new RelationFieldDTO(rel.field(), rel.isCollection(), rel.otherClass))
                .toList();

        return new ClassDTO(c.getName(), parent, attrs, ops, c.isAbstract(), realizations, endpoints, relations);
    }

    private OperationDTO toOperationDTO(String className, Operation o) {
        List<Parameter> raw = o.getParameters() != null ? o.getParameters() : List.of();
        List<ParameterDTO> params = raw.stream()
                .map(p -> new ParameterDTO(p.getName(), GeneratorUtils.removePackageNames(p.getType())))
                .toList();

        return new OperationDTO(
                o.getId(),
                o.getName(),
                GeneratorUtils.removePackageNames(o.getReturnType()),
                o.getVisibility(),
                params
        );
    }

    private AttributeDTO toAttributeDto(Attribute a) {
        return new AttributeDTO(a.getName(), a.getType());
    }

    private AssociationDTO toAssociationDto(Association a) {
        return new AssociationDTO(
                a.getSourceClass(),         // from Relationship
                a.getTargetClass(),         // from Relationship
                a.getSourceMultiplicity(),
                a.getTargetMultiplicity(),
                a.getSourceRole(),
                a.getTargetRole(),
                a.isSourceNavigable(),
                a.isTargetNavigable()
        );
    }

    private InterfaceDTO toInterfaceDto(Interface i) {
        List<OperationDTO> ops = i.getOperations().stream()
                .map(o -> toOperationDTO(i.getName(), o)).toList();
        return new InterfaceDTO(i.getName(), ops);
    }

    private InteractionDTO toInteractionDTO(Interaction i) {
        List<LifelineDTO> lifelines = i.getLifelines().stream().map(this::toLifelineDTO).toList();
        List<Object> elements = toElements(i.getElements());
        return new InteractionDTO(i.getId(), i.getName(), i.getContextClass(), lifelines, elements);
    }

    private LifelineDTO toLifelineDTO(model.sequence.Lifeline l){
        return new LifelineDTO(l.getId(), l.getName(), l.getRepresents(), l.getType());
    }

    private List<Object> toElements(List<Object> domainElements) {
        List<Object> result = new ArrayList<>();
        for (Object el : domainElements) {
            if (el instanceof Message m) {
                result.add(toMessageDTO(m));
            } else if (el instanceof Fragment f) {
                result.add(toFragmentDTO(f));
            }
        }
        return result;

    }

    private FragmentDTO toFragmentDTO(Fragment f) {
        List<OperandDTO> operands = new ArrayList<>();
        for (Operand op : f.getOperands()) {
            operands.add(new OperandDTO(op.getGuard(), toElements(op.getElements())));
        }
        return new FragmentDTO(f.getOperator(), operands);
    }

    private MessageDTO toMessageDTO(Message m) {
        return new MessageDTO(
            m.getId(),
            m.getRawName(),
            m.getMessageSort(),
            m.getSource() != null ? m.getSource().getId() : null,
            m.getTarget() != null ? m.getTarget().getId() : null
        );
    }

    private MessageArgumentDTO toMessageArgumentDTO(MessageArgument a) {
        return new MessageArgumentDTO(a.getName(), a.getValue(), a.getType());
    }

    /**
     * Mete todos os ficheiros do dir num zip e retorna como byte array para enviar na resposta
     */
    private byte[] zipDirectory(Path dir) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos);
             Stream<Path> paths = Files.walk(dir)) {

            paths.filter(Files::isRegularFile).forEach(path -> {
                String entryName = dir.relativize(path).toString().replace('\\', '/');
                try {
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        return baos.toByteArray();
    }

    /*
    private String formatOperation(Operation o) {
        String params = o.getParameters().stream()
                .map(p -> p.getName() + ": " + p.getType())
                .collect(Collectors.joining(", "));
        return o.getName() + "(" + params + "): " + o.getReturnType();
    }*/
}