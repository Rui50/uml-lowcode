package util;

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
}
