package app.service;

import app.dto.AnnotationsDTO;
import app.dto.CustomIdDTO;
import app.dto.EndpointConfigDTO;
import app.dto.HiddenFieldsDTO;
import app.dto.OclDTO;
import app.dto.ResponseOverrideDTO;

import model.*;
import model.Class;
import model.sequence.Interaction;
import parser.AnnotationParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnnotationService {
    public void apply(ParsedModel model, AnnotationsDTO payload) {
        if (payload == null) return;

        List<Class> classes = model.getClasses();
        List<Association> associations = model.getAssociations();
        clearAnnotations(classes, model.getInterfaces());

        applyInvariants(classes, associations, payload.getInvariantsByClass(), model.getInterfaces());
        applyInits(classes, associations, payload.getInitByAttribute());
        applyDerives(classes, associations, payload.getDeriveByAttribute());

        applyOperationLinks(classes, model.getAllInteractions(), payload.getOperationToSeqDiagram());
        applyPreconditions(classes, associations, payload.getPreconditionsByOperation());
        applyPostconditions(classes, associations, payload.getPostconditionsByOperation());
        applyInheritanceStrategies(classes, payload.getInheritanceStrategyByClass());
        applyIds(classes, payload.getIdByClass());
        applyEndpointConfig(classes, payload.getEndpointByOperation());
        applyControllerConfig(classes, payload.getGenerateControllerByClass());
        applyBeanValidationConfig(classes, payload.getBeanValidationByClass());
        applyTableNames(classes, payload.getTableNameByClass());
        applyDtoHidden(classes, payload.getDtoHiddenByClass());
        applyResponseOverrides(classes, payload.getResponseOverrideByClass());
    }

    // overrides do response por endpoint (endpointType -> {nome, escondidos}); não se herdam
    private void applyResponseOverrides(List<Class> classes, Map<String, Map<String, ResponseOverrideDTO>> map) {
        if (map == null) return;
        map.forEach((className, byEndpointType) -> {
            Class c = findClass(classes, className);
            if (c == null || byEndpointType == null) return;
            byEndpointType.forEach((endpointType, ovr) -> {
                if (ovr == null) return;
                String name = ovr.getName() != null ? ovr.getName().trim() : "";
                c.getResponseOverrides().put(endpointType, new ResponseOverride(name, new HashSet<>(ovr.getHidden())));
            });
        });
    }

    // atributos escondidos por DTO (painel Spring). Guardados como sets de NOMES na classe
    // (cobre atributos herdados; a consulta nos geradores faz união com os antepassados)
    private void applyDtoHidden(List<Class> classes, Map<String, HiddenFieldsDTO> map) {
        if (map == null) return;
        map.forEach((className, hidden) -> {
            Class c = findClass(classes, className);
            if (c == null || hidden == null) return;
            c.getHiddenInCreate().addAll(hidden.getCreate());
            c.getHiddenInUpdate().addAll(hidden.getUpdate());
            c.getHiddenInResponse().addAll(hidden.getResponse());
        });
    }

    // override do nome da tabela JPA, por classe (painel Persistence); vazio = nome derivado
    private void applyTableNames(List<Class> classes, Map<String, String> map) {
        if (map == null) return;
        map.forEach((className, tableName) -> {
            Class c = findClass(classes, className);
            if (c != null) c.setTableName(tableName);   // setter ignora null/vazio
        });
    }

    // gerar (ou não) o controller REST, por classe
    private void applyControllerConfig(List<Class> classes, Map<String, Boolean> map) {
        if (map == null) return;
        map.forEach((className, generate) -> {
            Class c = findClass(classes, className);
            if (c != null && generate != null) c.setGenerateController(generate);
        });
    }

    // anotações spring Validation (a partir das invariantes OCL), por classe
    private void applyBeanValidationConfig(List<Class> classes, Map<String, Boolean> map) {
        if (map == null) return;
        map.forEach((className, on) -> {
            Class c = findClass(classes, className);
            if (c != null && on != null) c.setBeanValidation(on);
        });
    }

    // config do endpoint REST / job, por operação
    private void applyEndpointConfig(List<Class> classes, Map<String, EndpointConfigDTO> map) {
        if (map == null) return;
        map.forEach((operationId, cfg) -> {
            Operation op = findOperation(classes, operationId);
            if (op == null || cfg == null) return;
            op.setJob(cfg.isJob());
            op.setEndpointName(cfg.getEndpointName());
            op.setHttpMethod(cfg.getHttpMethod() != null && !cfg.getHttpMethod().isBlank()
                    ? cfg.getHttpMethod() : "POST");
            op.setCron(cfg.getCron());
        });
    }

    // estratégia de herança JPA, anotada na superclasse
    private void applyInheritanceStrategies(List<Class> classes, Map<String, String> map) {
        if (map == null) return;
        map.forEach((className, strategy) -> {
            Class c = findClass(classes, className);
            if (c != null) c.setInheritanceStrategy(strategy);
        });
    }

    // @Id custom: atributo escolhido (int/String) ou id default (Long por defeito ou UUID)
    private void applyIds(List<Class> classes, Map<String, CustomIdDTO> map) {
        if (map == null) return;
        map.forEach((className, cfg) -> {
            Class c = findClass(classes, className);
            if (c == null || cfg == null) return;
            c.setIdAttributeName(cfg.getAttributeName());   // null/vazio = id sintético
            c.setIdAttributeType(cfg.getAttributeType());   // setter ignora null/vazio
        });
    }

    private void applyInvariants(List<Class> classes, List<Association> associations,
                                 Map<String, List<OclDTO>> map,
                                    List<Interface> interfaces
    ) {
        map.forEach((className, list) -> {
            Class c = findClass(classes, className);
            Interface iface = (c == null) ? findInterface(interfaces, className) : null;
            if (c == null && iface == null) return;
            for (int i = 0; i < list.size(); i++) {
                OclDTO dto = list.get(i);
                String name;

                // aplicar defaults se nao vierem da frontend

                if (dto.getName() == null || dto.getName().isEmpty()) {
                    name = "inv" + (i + 1);
                } else {
                    name = dto.getName();
                }

                String err_message;

                if (dto.getMessage() == null || dto.getMessage().isEmpty()) {
                    err_message = "Invariant " + name + " failed";
                } else {
                    err_message = dto.getMessage();
                }

                try {
                    if (c != null) {
                        c.addInvariant(AnnotationParser.parseInvariant(name, dto.getExpression(), err_message, c,
                                associations));
                    } else {
                        // interfaces have no attributes/associations of their own — pass empty list
                        iface.addInvariant(AnnotationParser.parseInvariant(name, dto.getExpression(), err_message,
                                null, associations));
                    }
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException(
                            "Invariante '" + name + "' em " + className + ": " + ex.getMessage());
                }
            }
        });
    }

    private Interface findInterface(List<Interface> interfaces, String name) {
        return interfaces.stream().filter(i -> i.getName().equals(name)).findFirst().orElse(null);
    }

    private void applyInits(List<Class> classes, List<Association> associations, Map<String, String> map) {
        map.forEach((key, expr) -> {
            Attribute a = findAttribute(classes, key);
            // owner class needed by parser for map detection
            Class c = findClass(classes, key.split("\\.", 2)[0]);
            if (a != null) {
                try {
                    AnnotationParser.applyInit(a, expr, c, associations);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Init em " + key + ": " + ex.getMessage());
                }
            }
        });
    }

    private void applyDerives(List<Class> classes, List<Association> associations, Map<String, String> map) {
        map.forEach((key, expr) -> {
            Attribute a = findAttribute(classes, key);
            // owner class needed by parser for map detection
            Class c = findClass(classes, key.split("\\.", 2)[0]);
            if (a != null) {
                try {
                    AnnotationParser.applyDerive(a, expr, c, associations);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Derive em " + key + ": " + ex.getMessage());
                }
            }
        });
    }

    private Class findClass(List<Class> classes, String name) {
        return classes.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }

    private Attribute findAttribute(List<Class> classes, String key) {
        String[] parts = key.split("\\.", 2);
        if (parts.length != 2) return null;
        Class c = findClass(classes, parts[0]);
        if (c == null) return null;
        for (Attribute a : c.getAttributes()) if (a.getName().equals(parts[1])) return a;
        return null;
    }

    private void applyOperationLinks(List<Class> classes, List<Interaction> interactions, Map<String, String> map) {
        if (map == null || map.isEmpty()) return;
        Map<String, Interaction> interactionsById = interactions.stream()
                .collect(Collectors.toMap(Interaction::getId, i -> i));
        for (Class c : classes) {
            for (Operation op : c.getOperations()) {
                String interactionId = map.get(op.getId());
                if (interactionId != null) op.setSequenceDiagram(interactionsById.get(interactionId));
            }
        }
    }

    private void applyPreconditions(List<Class> classes, List<Association> associations, Map<String, List<OclDTO>> map) {
        if (map == null || map.isEmpty()) return;
        map.forEach((operationId, list) -> {
            Class owner = findClassForOperation(classes, operationId);
            Operation op = findOperation(classes, operationId);
            if (op == null) return;
            for (int i = 0; i < list.size(); i++) {
                OclDTO dto = list.get(i);
                String name = (dto.getName() == null || dto.getName().isEmpty()) ? "pre" + (i + 1) : dto.getName();
                String errMsg = (dto.getMessage() == null || dto.getMessage().isEmpty())
                        ? "Precondition " + name + " failed" : dto.getMessage();
                try {
                    op.addPrecondition(AnnotationParser.parsePrecondition(name, dto.getExpression(), errMsg, owner, associations));
                } catch (IllegalArgumentException ex) {
                    String loc = (owner != null ? owner.getName() + "." : "") + op.getName();
                    throw new IllegalArgumentException("Pré-condição '" + name + "' em " + loc + ": " + ex.getMessage());
                }
            }
        });
    }

    private void applyPostconditions(List<Class> classes, List<Association> associations, Map<String, List<OclDTO>> map) {
        if (map == null || map.isEmpty()) return;
        map.forEach((operationId, list) -> {
            Class owner = findClassForOperation(classes, operationId);
            Operation op = findOperation(classes, operationId);
            if (op == null) return;
            for (int i = 0; i < list.size(); i++) {
                OclDTO dto = list.get(i);
                String name = (dto.getName() == null || dto.getName().isEmpty()) ? "post" + (i + 1) : dto.getName();
                String errMsg = (dto.getMessage() == null || dto.getMessage().isEmpty())
                        ? "Postcondition " + name + " failed" : dto.getMessage();
                try {
                    op.addPostcondition(AnnotationParser.parsePostcondition(name, dto.getExpression(), errMsg, owner, associations));
                } catch (IllegalArgumentException ex) {
                    String loc = (owner != null ? owner.getName() + "." : "") + op.getName();
                    throw new IllegalArgumentException("Pós-condição '" + name + "' em " + loc + ": " + ex.getMessage());
                }
            }
        });
    }

    private Class findClassForOperation(List<Class> classes, String opId) {
        for (Class c : classes) {
            for (Operation op : c.getOperations()) {
                if (opId.equals(op.getId())) return c;
            }
        }
        return null;
    }

    private Operation findOperation(List<Class> classes, String opId) {
        for (Class c : classes) {
            for (Operation op : c.getOperations()) {
                if (opId.equals(op.getId())) return op;
            }
        }
        return null;
    }

    private void clearAnnotations(List<Class> classes, List<Interface> interfaces) {        for (Class c : classes) {
            c.getInvariants().clear();

            for (Operation op : c.getOperations()) {
                op.getPreconditions().clear();
                op.getPostconditions().clear();
                op.setJob(false);
                op.setEndpointName(null);
                op.setHttpMethod("POST");
                op.setCron(null);
            }
            for (Attribute a : c.getAttributes()) {
                a.setInitExpression(null);
                a.setJavaInitExpression(null);
                a.setDeriveExpression(null);
                a.setJavaDeriveExpression(null);
            }
            c.getHiddenInCreate().clear();
            c.getHiddenInUpdate().clear();
            c.getHiddenInResponse().clear();
            c.getResponseOverrides().clear();
        }
        for (Interface iface : interfaces) {
            iface.getInvariants().clear();
        }
    }
}
