package generator;

import model.Class;
import model.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/*
* Gerar a esturura das classes
* */

public class ClassGenerator {
    private String outputDirectory;

    public ClassGenerator(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void generateClasses(List<Class> classes, List<Association> associations) throws IOException {
        for (Class cls : classes) {
            generateClass(cls, associations);
        }
    }

    private void generateClass(Class cls, List<Association> associations) throws IOException {
        String filename = outputDirectory + "/" + cls.getName() + ".java";

        try(PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // imports
            writer.println("import java.util.*;");
            writer.println();

            // class
            writer.println("public class " + cls.getName() + " {");
            writer.println();

            // attributes
            generateAttributes(writer, cls);
            writer.println();

            // associations
            generateAssociations(writer, cls, associations);
            writer.println();

            // constructor
            generateConstructors(writer, cls, associations);
            writer.println();

            // getters and setters
            generateGettersSetters(writer, cls);
            writer.println();

            // association methods
            generateAssociationMethods(writer, cls, associations);
            writer.println();

            writer.println("}");
        }

        System.out.println("Generated class: " + filename);
    }

    private void generateAttributes(PrintWriter writer, Class cls){
        writer.println("    // Attributes");

        for (Attribute attribute : cls.getAttributes()){
            writer.println("    " + attribute.getVisibility() + " " + attribute.getType() + " " + attribute.getName() + ";");
        }
    }

    private void generateAssociations(PrintWriter writer, Class cls, List<Association> associations){
        List<String> generatedAttributes = new ArrayList<>();

        writer.println("    // Associations");

        for (Association association : associations){

            // where class is source and navigable
            if (association.getSourceClass().equals(cls.getName()) && association.isSourceNavigable()){
                String targetClass = association.getTargetClass();
                String multiplicity = association.getTargetMultiplicity();
                // get the attribute name based on the target class and multiplicity
                String attributeName = generateAssociationAttributeName(targetClass, multiplicity);

                if (!generatedAttributes.contains(attributeName)){
                    if (checkMultiplicity(multiplicity)){
                        writer.println("    " + "private List<" + targetClass + "> " + attributeName + ";");
                    } else {
                        writer.println("    " + "private " + targetClass + " " + attributeName + ";");
                    }
                    generatedAttributes.add(attributeName);
                }
            }

            // where class is target and navigable
            if (association.getTargetClass().equals(cls.getName()) && association.isTargetNavigable()){
                String sourceClass = association.getSourceClass();
                String multiplicity = association.getSourceMultiplicity();
                String attributeName = generateAssociationAttributeName(sourceClass, multiplicity);

                if (!generatedAttributes.contains(attributeName)){
                    if (checkMultiplicity(multiplicity)){
                        writer.println("    " + "private List<" + sourceClass + "> " + attributeName + ";");
                    } else {
                        writer.println("    " + "private " + sourceClass + " " + attributeName + ";");
                    }
                    generatedAttributes.add(attributeName);
                }
            }
        }
    }

    private void generateConstructors(PrintWriter writer, Class cls, List<Association> associations){
        writer.println("    // Constructor");
        // only the default for now at least
        writer.println("    public " + cls.getName() + "() {");

        for (Association association : associations) {
            // where class is source and nav
            if (association.getSourceClass().equals(cls.getName()) && association.isSourceNavigable()) {
                String multiplicity = association.getTargetMultiplicity();

                if (checkMultiplicity(multiplicity)) {
                    String targetClass = association.getTargetClass();
                    String attributeName = generateAssociationAttributeName(targetClass, multiplicity);
                    writer.println("        this." + attributeName + " = new ArrayList<>();");
                }
            }

            // where class is target and nav
            if (association.getTargetClass().equals(cls.getName()) && association.isTargetNavigable()) {
                String multiplicity = association.getSourceMultiplicity();
                if (checkMultiplicity(multiplicity)) {
                    String sourceClass = association.getSourceClass();
                    String attributeName = generateAssociationAttributeName(sourceClass, multiplicity);
                    writer.println("        this." + attributeName + " = new ArrayList<>();");
                }
            }
        }

        writer.println("    }");
    }

    private void generateGettersSetters(PrintWriter writer, Class cls){
        writer.println("    // Getters and Setters");

        for (Attribute attribute : cls.getAttributes()) {
            String name = attribute.getName();
            String type = attribute.getType();
            String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);

            // get
            writer.println();
            writer.println("    public " + type + " get" + capitalizedName + "() {");
            writer.println("        return " + name + ";");
            writer.println("    }");

            // set
            writer.println();
            writer.println("    public void set" + capitalizedName + "(" + type + " " + name + ") {");
            writer.println("        this." + name + " = " + name + ";");
            writer.println("    }");
        }
    }

    private void generateAssociationMethods(PrintWriter writer, Class cls, List<Association> associations) {
        for (Association association: associations) {

            // where class is the source
            if (association.getSourceClass().equals(cls.getName()) && association.isSourceNavigable()) {
                String targetClass = association.getTargetClass();
                String multiplicity = association.getTargetMultiplicity();
                String attributeName = generateAssociationAttributeName(targetClass, multiplicity);

                if(checkMultiplicity(multiplicity)) {
                    generateListAssociationMethods(writer, targetClass, attributeName);
                } else {
                    generateSingleAssociationMethods(writer, targetClass, attributeName);
                }
            }

            if (association.getTargetClass().equals(cls.getName()) && association.isTargetNavigable()) {
                String sourceClass = association.getSourceClass();
                String multiplicity = association.getSourceMultiplicity();
                String attributeName = generateAssociationAttributeName(sourceClass, multiplicity);

                if (checkMultiplicity(multiplicity)) {
                    generateListAssociationMethods(writer, sourceClass, attributeName);
                } else {
                    generateSingleAssociationMethods(writer, sourceClass, attributeName);
                }
            }
        }
    }

    private void generateListAssociationMethods(PrintWriter wrriter, String className, String attributeName){
        // add method
        wrriter.println();
        wrriter.println("    public void add" + className + "(" + className + " " + attributeName + ") {");
        wrriter.println("        this." + attributeName + ".add(" + attributeName + ");");
        wrriter.println("    }");

        // remove method
        wrriter.println();
        wrriter.println("    public void remove" + className + "(" + className + " " + attributeName + ") {");
        wrriter.println("        this." + attributeName + ".remove(" + attributeName + ");");
        wrriter.println("    }");

        // get method
        wrriter.println();
        wrriter.println("    public List<" + className + "> get" + className + "s() {");
        wrriter.println("        return this." + attributeName + ";");
        wrriter.println("    }");
    }

    private void generateSingleAssociationMethods(PrintWriter wrriter, String className, String attributeName){
        // get method
        wrriter.println();
        wrriter.println("    public " + className + " get" + className + "() {");
        wrriter.println("        return this." + attributeName + ";");
        wrriter.println("    }");

        // set method
        wrriter.println();
        wrriter.println("    public void set" + className + "(" + className + " " + attributeName + ") {");
        wrriter.println("        this." + attributeName + " = " + attributeName + ";");
        wrriter.println("    }");
    }

    private String generateAssociationAttributeName(String targetClass, String multiplicity) {
        // make first letter lowercase
        String base = targetClass.substring(0, 1).toLowerCase() + targetClass.substring(1);

        return checkMultiplicity(multiplicity) ? base + "s" : base;
    }

    private boolean checkMultiplicity(String multiplicity) {
        if (multiplicity == null) return false;
        return multiplicity.contains("*") ||
                (multiplicity.contains("..") && !multiplicity.equals("0..1"));
    }
}
