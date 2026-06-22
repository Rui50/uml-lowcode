package menus;

import models.*;
import repository.*;
import util.InputUtils;
import java.util.*;

public class AlunoMenu {

    private Scanner scanner;
    private AlunoRepository alunoRepository;
    private TurnoRepository turnoRepository;

    public AlunoMenu(Scanner scanner, AlunoRepository alunoRepository, TurnoRepository turnoRepository) {
        this.scanner = scanner;
        this.alunoRepository = alunoRepository;
        this.turnoRepository = turnoRepository;
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Aluno ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Assign turno");
            System.out.println("7. Remove turno");
            System.out.println("0. Back");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: create(); break;
                case 2: viewAll(); break;
                case 3: viewById(); break;
                case 4: update(); break;
                case 5: delete(); break;
                case 6: assignTurno(); break;
                case 7: removeTurno(); break;
                case 0: return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void create() {
        int numero = InputUtils.readInt(scanner, "numero: ");
        System.out.print("email: ");
        String email = scanner.nextLine();
        System.out.print("nome: ");
        String nome = scanner.nextLine();
        EstadoAluno estado = null;
        while (estado == null) {
            System.out.print("estado " + java.util.Arrays.toString(EstadoAluno.values()) + ": ");
            try { estado = EstadoAluno.valueOf(scanner.nextLine().trim().toUpperCase()); }
            catch (IllegalArgumentException ex) { System.out.println("Invalid value. Try again."); }
        }

        Aluno entity = new Aluno(numero, email, nome, estado);

        alunoRepository.save(entity);
        System.out.println("Created: " + entity);
    }

    private void viewAll() {
        List<Aluno> all = alunoRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Aluno entity : all) {
            System.out.println(entity);
            System.out.println("  turno: " + entity.getTurno());
        }
    }

    private void viewById() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Aluno entity = alunoRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
        System.out.println("  turno: " + entity.getTurno());
    }

    private void update() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Aluno entity = alunoRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }

        System.out.println("Current: " + entity);
        System.out.println("(press Enter to keep current value)");

        System.out.print("numero [" + entity.getNumero() +"]: ");
        String numeroStr = scanner.nextLine();
        if (!numeroStr.isEmpty()) entity.setNumero(Integer.parseInt(numeroStr));
        System.out.print("email [" + entity.getEmail() +"]: ");
        String email = scanner.nextLine();
        if (!email.isEmpty()) entity.setEmail(email);
        System.out.print("nome [" + entity.getNome() +"]: ");
        String nome = scanner.nextLine();
        if (!nome.isEmpty()) entity.setNome(nome);
        while (true) {
            System.out.print("estado [" + entity.getEstado() + "] " + java.util.Arrays.toString(EstadoAluno.values()) + ": ");
            String estadoStr = scanner.nextLine();
            if (estadoStr.isEmpty()) break;
            try { entity.setEstado(EstadoAluno.valueOf(estadoStr.trim().toUpperCase())); break; }
            catch (IllegalArgumentException ex) { System.out.println("Invalid value. Try again."); }
        }

        alunoRepository.save(entity);
        System.out.println("Updated: " + entity);
    }

    private void delete() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        if (alunoRepository.deleteById(id)) {
            System.out.println("Deleted.");
        } else {
            System.out.println("Not found.");
        }
    }

    private void assignTurno() {
        Long ownerId = InputUtils.readLong(scanner, "Aluno ID: ");
        Aluno owner = alunoRepository.findById(ownerId);
        if (owner == null) { System.out.println("Aluno not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Turno ID: ");
        Turno other = turnoRepository.findById(otherId);
        if (other == null) { System.out.println("Turno not found."); return; }

        owner.setTurno(other);
        alunoRepository.save(owner);
        System.out.println("Assigned Turno to Aluno.");
    }

    private void removeTurno() {
        Long id = InputUtils.readLong(scanner, "Aluno ID: ");
        Aluno entity = alunoRepository.findById(id);
        if (entity == null) { System.out.println("Aluno not found."); return; }

        entity.setTurno(null);
        System.out.println("Turno removed from " + entity);
    }

}
