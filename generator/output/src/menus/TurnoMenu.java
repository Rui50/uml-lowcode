package menus;

import models.*;
import repository.*;
import util.InputUtils;
import java.util.*;

public class TurnoMenu {

    private Scanner scanner;
    private TurnoRepository turnoRepository;
    private AlunoRepository alunoRepository;

    public TurnoMenu(Scanner scanner, TurnoRepository turnoRepository, AlunoRepository alunoRepository) {
        this.scanner = scanner;
        this.turnoRepository = turnoRepository;
        this.alunoRepository = alunoRepository;
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Turno ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Add alunos");
            System.out.println("7. Remove alunos");
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
                case 0: return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void create() {
        int capacidade = InputUtils.readInt(scanner, "capacidade: ");
        int numero = InputUtils.readInt(scanner, "numero: ");

        Turno entity = new Turno(capacidade, numero);

        turnoRepository.save(entity);
        System.out.println("Created: " + entity);
    }

    private void viewAll() {
        List<Turno> all = turnoRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Turno entity : all) {
            System.out.println(entity);
            System.out.println("  alunos: " + entity.getAlunos());
        }
    }

    private void viewById() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Turno entity = turnoRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
        System.out.println("  alunos: " + entity.getAlunos());
    }

    private void update() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Turno entity = turnoRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }

        System.out.println("Current: " + entity);
        System.out.println("(press Enter to keep current value)");

        System.out.print("capacidade [" + entity.getCapacidade() +"]: ");
        String capacidadeStr = scanner.nextLine();
        if (!capacidadeStr.isEmpty()) entity.setCapacidade(Integer.parseInt(capacidadeStr));
        System.out.print("numero [" + entity.getNumero() +"]: ");
        String numeroStr = scanner.nextLine();
        if (!numeroStr.isEmpty()) entity.setNumero(Integer.parseInt(numeroStr));

        turnoRepository.save(entity);
        System.out.println("Updated: " + entity);
    }

    private void delete() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        if (turnoRepository.deleteById(id)) {
            System.out.println("Deleted.");
        } else {
            System.out.println("Not found.");
        }
    }

    private void addAlunos() {
        Long ownerId = InputUtils.readLong(scanner, "Turno ID: ");
        Turno owner = turnoRepository.findById(ownerId);
        if (owner == null) { System.out.println("Turno not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Aluno ID: ");
        Aluno other = alunoRepository.findById(otherId);
        if (other == null) { System.out.println("Aluno not found."); return; }

        owner.getAlunos().add(other);
        turnoRepository.save(owner);
        System.out.println("Added Aluno to Turno.");
    }

    private void removeAlunos() {
        Long ownerId = InputUtils.readLong(scanner, "Turno ID: ");
        Turno owner = turnoRepository.findById(ownerId);
        if (owner == null) { System.out.println("Turno not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Aluno ID: ");
        Aluno other = alunoRepository.findById(otherId);
        if (other == null) { System.out.println("Aluno not found."); return; }

        owner.getAlunos().remove(other);
        System.out.println("Aluno removed from " + owner);
    }

}
