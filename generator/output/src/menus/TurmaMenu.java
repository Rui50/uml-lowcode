package menus;

import models.*;
import repository.*;
import util.InputUtils;
import java.util.*;

public class TurmaMenu {

    private Scanner scanner;
    private TurmaRepository turmaRepository;
    private AlunoRepository alunoRepository;
    private TurnoRepository turnoRepository;

    public TurmaMenu(Scanner scanner, TurmaRepository turmaRepository, AlunoRepository alunoRepository, TurnoRepository turnoRepository) {
        this.scanner = scanner;
        this.turmaRepository = turmaRepository;
        this.alunoRepository = alunoRepository;
        this.turnoRepository = turnoRepository;
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Turma ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Add alunos");
            System.out.println("7. Remove alunos");
            System.out.println("8. Add turnos");
            System.out.println("9. Remove turnos");
            System.out.println("0. Back");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: create(); break;
                case 2: viewAll(); break;
                case 3: viewById(); break;
                case 4: update(); break;
                case 5: delete(); break;
                case 6: addAlunos(); break;
                case 7: removeAlunos(); break;
                case 8: addTurnos(); break;
                case 9: removeTurnos(); break;
                case 0: return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void create() {
        System.out.print("nome: ");
        String nome = scanner.nextLine();

        Turma entity = new Turma(nome);

        turmaRepository.save(entity);
        System.out.println("Created: " + entity);
    }

    private void viewAll() {
        List<Turma> all = turmaRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Turma entity : all) {
            System.out.println(entity);
            System.out.println("  alunos: " + entity.getAlunos().values());
            System.out.println("  turnos: " + entity.getTurnos());
        }
    }

    private void viewById() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Turma entity = turmaRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
        System.out.println("  alunos: " + entity.getAlunos().values());
        System.out.println("  turnos: " + entity.getTurnos());
    }

    private void update() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Turma entity = turmaRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }

        System.out.println("Current: " + entity);
        System.out.println("(press Enter to keep current value)");

        System.out.print("nome [" + entity.getNome() +"]: ");
        String nome = scanner.nextLine();
        if (!nome.isEmpty()) entity.setNome(nome);

        turmaRepository.save(entity);
        System.out.println("Updated: " + entity);
    }

    private void delete() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        if (turmaRepository.deleteById(id)) {
            System.out.println("Deleted.");
        } else {
            System.out.println("Not found.");
        }
    }

    private void addAlunos() {
        Long ownerId = InputUtils.readLong(scanner, "Turma ID: ");
        Turma owner = turmaRepository.findById(ownerId);
        if (owner == null) { System.out.println("Turma not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Aluno ID: ");
        Aluno other = alunoRepository.findById(otherId);
        if (other == null) { System.out.println("Aluno not found."); return; }

        owner.getAlunos().put(other.getNumero(), other);
        turmaRepository.save(owner);
        System.out.println("Added Aluno to Turma.");
    }

    private void removeAlunos() {
        Long ownerId = InputUtils.readLong(scanner, "Turma ID: ");
        Turma owner = turmaRepository.findById(ownerId);
        if (owner == null) { System.out.println("Turma not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Aluno ID: ");
        Aluno other = alunoRepository.findById(otherId);
        if (other == null) { System.out.println("Aluno not found."); return; }

        owner.getAlunos().remove(other.getNumero());
        System.out.println("Aluno removed from " + owner);
    }

    private void addTurnos() {
        Long ownerId = InputUtils.readLong(scanner, "Turma ID: ");
        Turma owner = turmaRepository.findById(ownerId);
        if (owner == null) { System.out.println("Turma not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Turno ID: ");
        Turno other = turnoRepository.findById(otherId);
        if (other == null) { System.out.println("Turno not found."); return; }

        owner.getTurnos().add(other);
        turmaRepository.save(owner);
        System.out.println("Added Turno to Turma.");
    }

    private void removeTurnos() {
        Long ownerId = InputUtils.readLong(scanner, "Turma ID: ");
        Turma owner = turmaRepository.findById(ownerId);
        if (owner == null) { System.out.println("Turma not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Turno ID: ");
        Turno other = turnoRepository.findById(otherId);
        if (other == null) { System.out.println("Turno not found."); return; }

        owner.getTurnos().remove(other);
        System.out.println("Turno removed from " + owner);
    }

}
