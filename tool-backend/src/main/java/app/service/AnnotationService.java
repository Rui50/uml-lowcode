package app.service;

import app.dto.AnnotationsDTO;
import app.dto.OclDTO;

import model.Attribute;
import model.Class;
import model.Operation;
import model.ParsedModel;
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
        clearAnnotations(classes);

        applyInvariants(classes, payload.getInvariantsByClass());
        applyInits(classes, payload.getInitByAttribute());
        applyDerives(classes, payload.getDeriveByAttribute());

        List<Interaction> allInteractions = new ArrayList<>();
        allInteractions.addAll(model.getPimInteractions());
        allInteractions.addAll(model.getPsmInteractions());
        applyOperationLinks(classes, allInteractions, payload.getOperationToSeqDiagram());
        applyPreconditions(classes, payload.getPreconditionsByOperation());
    }

    private void applyInvariants(List<Class> classes, Map<String, List<OclDTO>> map) {
        map.forEach((className, list) -> {
            Class c = findClass(classes, className);
            if (c == null) return;
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

                c.addInvariant(AnnotationParser.parseInvariant(name, dto.getExpression(), err_message));
            }
        });
    }

    private void applyInits(List<Class> classes, Map<String, String> map) {
        map.forEach((key, expr) -> {
            if (expr == null || expr.isBlank()) return;
            Attribute a = findAttribute(classes, key);
            if (a != null) AnnotationParser.applyInit(a, expr);
        });
    }

    private void applyDerives(List<Class> classes, Map<String, String> map) {
        map.forEach((key, expr) -> {
            if (expr == null || expr.isBlank()) return;
            Attribute a = findAttribute(classes, key);
            if (a != null) AnnotationParser.applyDerive(a, expr);
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

    private void applyPreconditions(List<Class> classes, Map<String, List<OclDTO>> map) {
        if (map == null || map.isEmpty()) return;
        map.forEach((operationId, list) -> {
            Operation op = findOperation(classes, operationId);
            if (op == null) return;
            for (int i = 0; i < list.size(); i++) {
                OclDTO dto = list.get(i);
                String name = (dto.getName() == null || dto.getName().isEmpty()) ? "pre" + (i + 1) : dto.getName();
                String errMsg = (dto.getMessage() == null || dto.getMessage().isEmpty())
                        ? "Precondition " + name + " failed" : dto.getMessage();
                op.addPrecondition(AnnotationParser.parsePrecondition(name, dto.getExpression(), errMsg));
            }
        });
    }

    private Operation findOperation(List<Class> classes, String opId) {
        for (Class c : classes) {
            for (Operation op : c.getOperations()) {
                if (opId.equals(op.getId())) return op;
            }
        }
        return null;
    }

    private void clearAnnotations(List<Class> classes) {
        for (Class c : classes) {
            c.getInvariants().clear();

            for (Operation op : c.getOperations()) {
                op.getPreconditions().clear();
            }
            for (Attribute a : c.getAttributes()) {
                a.setInitExpression(null);
                a.setJavaInitExpression(null);
                a.setDeriveExpression(null);
                a.setJavaDeriveExpression(null);
            }
        }
    }
}
