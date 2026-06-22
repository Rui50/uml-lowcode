package menus;

import repository.*;
import util.InputUtils;
import java.util.*;

public class MainMenu {

    private Scanner scanner = new Scanner(System.in);
    private AlunoRepository alunoRepository = new AlunoRepository();
    private TurmaRepository turmaRepository = new TurmaRepository();
    private AlunoTERepository alunoTERepository = new AlunoTERepository();
    private TurnoRepository turnoRepository = new TurnoRepository();

    public void run() {
        while (true) {
            System.out.println("\n=== Main Menu ===");
            System.out.println("1. Aluno Menu");
            System.out.println("2. Turma Menu");
            System.out.println("3. AlunoTE Menu");
            System.out.println("4. Turno Menu");
            System.out.println("0. Exit");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: new AlunoMenu(scanner, alunoRepository, turnoRepository).run(); break;
                case 2: new TurmaMenu(scanner, turmaRepository, alunoRepository, turnoRepository).run(); break;
                case 3: new AlunoTEMenu(scanner, alunoTERepository, turnoRepository).run(); break;
                case 4: new TurnoMenu(scanner, turnoRepository, alunoRepository).run(); break;
                case 0: System.out.println("Bye!"); return;
                default: System.out.println("Invalid option.");
            }
        }
    }

}
