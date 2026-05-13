package app.service;

import app.dto.AnnotationsDTO;
import app.dto.OclDTO;

import model.*;
import model.Class;
import model.sequence.Interaction;
import parser.AnnotationParser;

import java.util.ArrayList;
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

                if (c != null) {
                    c.addInvariant(AnnotationParser.parseInvariant(name, dto.getExpression(), err_message, c,
                            associations));
                } else {
                    // interfaces have no attributes/associations of their own — pass empty list
                    iface.addInvariant(AnnotationParser.parseInvariant(name, dto.getExpression(), err_message,
                            null, associations));
                }
            }
        });
    }

    private Interface findInterface(List<Interface> interfaces, String name) {
        return interfaces.stream().filter(i -> i.getName().equals(name)).findFirst().orElse(null);
    }

    private void applyInits(List<Class> classes, List<Association> associations, Map<String, String> map) {
        map.forEach((key, expr) -> {
            if (expr == null || expr.isBlank()) return;
            Attribute a = findAttribute(classes, key);
            // owner class needed by parser for map detection
            Class c = findClass(classes, key.split("\\.", 2)[0]);
            if (a != null) AnnotationParser.applyInit(a, expr, c, associations);
        });
    }

    private void applyDerives(List<Class> classes, List<Association> associations, Map<String, String> map) {
        map.forEach((key, expr) -> {
            if (expr == null || expr.isBlank()) return;
            Attribute a = findAttribute(classes, key);
            // owner class needed by parser for map detection
            Class c = findClass(classes, key.split("\\.", 2)[0]);
            if (a != null) AnnotationParser.applyDerive(a, expr, c, associations);
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
                op.addPrecondition(AnnotationParser.parsePrecondition(name, dto.getExpression(), errMsg, owner, associations));
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
                op.addPostcondition(AnnotationParser.parsePostcondition(name, dto.getExpression(), errMsg, owner, associations));
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
            }
            for (Attribute a : c.getAttributes()) {
                a.setInitExpression(null);
                a.setJavaInitExpression(null);
                a.setDeriveExpression(null);
                a.setJavaDeriveExpression(null);
            }
        }
        for (Interface iface : interfaces) {
            iface.getInvariants().clear();
        }
    }
}
