package com.example.app.menus;

import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.*;

public class MainMenu {

    private final Scanner scanner = new Scanner(System.in);
    private final EntityManager em;

    public MainMenu(EntityManager em) {
        this.em = em;
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Main Menu ===");
            System.out.println("1. Library Menu");
            System.out.println("2. StudentMember Menu");
            System.out.println("3. StaffMember Menu");
            System.out.println("4. Book Menu");
            System.out.println("5. Author Menu");
            System.out.println("6. Loan Menu");
            System.out.println("7. LibraryCard Menu");
            System.out.println("0. Exit");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: new LibraryMenu(scanner, em).run(); break;
                case 2: new StudentMemberMenu(scanner, em).run(); break;
                case 3: new StaffMemberMenu(scanner, em).run(); break;
                case 4: new BookMenu(scanner, em).run(); break;
                case 5: new AuthorMenu(scanner, em).run(); break;
                case 6: new LoanMenu(scanner, em).run(); break;
                case 7: new LibraryCardMenu(scanner, em).run(); break;
                case 0: System.out.println("Bye!"); return;
                default: System.out.println("Invalid option.");
            }
        }
    }
}
