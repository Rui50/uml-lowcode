package com.example.app.util;

import java.util.List;
import java.util.Scanner;

public class InputUtils {

    public static int readInt(Scanner scanner, String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid. " + prompt);
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine();
        return value;
    }

    public static Long readLong(Scanner scanner, String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextLong()) {
            System.out.print("Invalid. " + prompt);
            scanner.next();
        }
        Long value = scanner.nextLong();
        scanner.nextLine();
        return value;
    }

    public static double readDouble(Scanner scanner, String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextDouble()) {
            System.out.print("Invalid. " + prompt);
            scanner.next();
        }
        double value = scanner.nextDouble();
        scanner.nextLine();
        return value;
    }

    public static String readString(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        while (value.isEmpty()) {
            System.out.print("Invalid. " + prompt);
            value = scanner.nextLine().trim();
        }
        return value;
    }

    public static java.util.UUID readUuid(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return java.util.UUID.fromString(scanner.nextLine().trim()); }
            catch (IllegalArgumentException e) { System.out.println("Invalid UUID."); }
        }
    }

    // imprime os erros de validação e devolve true se havia algum (para o menu abortar)
    public static boolean printErrors(List<String> errors) {
        if (errors.isEmpty()) return false;
        System.out.println("Validation errors:");
        errors.forEach(e -> System.out.println("  - " + e));
        return true;
    }
}
