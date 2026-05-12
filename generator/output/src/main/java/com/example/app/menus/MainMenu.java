package com.example.app.menus;

import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;

public class MainMenu {

    private final Scanner scanner = new Scanner(System.in);
    private final EntityManager em;

    public MainMenu(EntityManager em) {
        this.em = em;
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Main Menu ===");
            System.out.println("1. Aluno Menu");
            System.out.println("2. Turma Menu");
            System.out.println("3. AlunoTE Menu");
            System.out.println("4. Turno Menu");
            System.out.println("5. testeSubAb Menu");
            System.out.println("6. ImplementInterface Menu");
            System.out.println("0. Exit");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: new AlunoMenu(scanner, em).run(); break;
                case 2: new TurmaMenu(scanner, em).run(); break;
                case 3: new AlunoTEMenu(scanner, em).run(); break;
                case 4: new TurnoMenu(scanner, em).run(); break;
                case 5: new testeSubAbMenu(scanner, em).run(); break;
                case 6: new ImplementInterfaceMenu(scanner, em).run(); break;
                case 0: System.out.println("Bye!"); return;
                default: System.out.println("Invalid option.");
            }
        }
    }
}
