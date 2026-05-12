package com.example.app.menus;

import com.example.app.models.*;
import com.example.app.repository.*;
import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;

public class TurmaMenu {

    private Scanner scanner;
    private EntityManager em;
    private TurmaRepository turmaRepository;
    private AlunoRepository alunoRepository;
    private TurnoRepository turnoRepository;

    public TurmaMenu(Scanner scanner, EntityManager em) {
        this.scanner = scanner;
        this.em = em;
        this.turmaRepository = new TurmaRepository(em);
        this.alunoRepository = new AlunoRepository(em);
        this.turnoRepository = new TurnoRepository(em);
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Turma ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Add Aluno");
            System.out.println("7. Remove Aluno");
            System.out.println("8. Add Turno");
            System.out.println("9. Remove Turno");
            System.out.println("0. Back");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: create(); break;
                case 2: viewAll(); break;
                case 3: viewById(); break;
                case 4: update(); break;
                case 5: delete(); break;
                case 6: addAluno(); break;
                case 7: removeAluno(); break;
                case 8: addTurno(); break;
                case 9: removeTurno(); break;
                case 0: return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void create() {
        System.out.print("nome: ");
        String nome = scanner.nextLine();
        int numAlunos = InputUtils.readInt(scanner, "numAlunos: ");

        Turma entity = new Turma();
        entity.setNome(nome);
        entity.setNumAlunos(numAlunos);

        em.getTransaction().begin();
        try {
            turmaRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Created: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
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
        System.out.print("numAlunos [" + entity.getNumAlunos() +"]: ");
        String numAlunosStr = scanner.nextLine();
        if (!numAlunosStr.isEmpty()) entity.setNumAlunos(Integer.parseInt(numAlunosStr));

        em.getTransaction().begin();
        try {
            turmaRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Updated: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void delete() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Turma entity = turmaRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        em.getTransaction().begin();
        try {
            turmaRepository.deleteById(id);
            em.getTransaction().commit();
            System.out.println("Deleted.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void addAluno() {
        Long ownerId = InputUtils.readLong(scanner, "Turma ID: ");
        Turma owner = turmaRepository.findById(ownerId);
        if (owner == null) { System.out.println("Turma not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Aluno ID: ");
        Aluno other = alunoRepository.findById(otherId);
        if (other == null) { System.out.println("Aluno not found."); return; }

        owner.getAlunos().put(other.getNumero(), other);
        em.getTransaction().begin();
        try {
            turmaRepository.save(owner);
            em.getTransaction().commit();
            System.out.println("Added Aluno to Turma.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void removeAluno() {
        Long ownerId = InputUtils.readLong(scanner, "Turma ID: ");
        Turma owner = turmaRepository.findById(ownerId);
        if (owner == null) { System.out.println("Turma not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Aluno ID: ");
        Aluno other = alunoRepository.findById(otherId);
        if (other == null) { System.out.println("Aluno not found."); return; }

        owner.getAlunos().remove(other.getNumero());
        em.getTransaction().begin();
        try {
            turmaRepository.save(owner);
            em.getTransaction().commit();
            System.out.println("Aluno removed from " + owner);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void addTurno() {
        Long ownerId = InputUtils.readLong(scanner, "Turma ID: ");
        Turma owner = turmaRepository.findById(ownerId);
        if (owner == null) { System.out.println("Turma not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Turno ID: ");
        Turno other = turnoRepository.findById(otherId);
        if (other == null) { System.out.println("Turno not found."); return; }

        owner.getTurnos().add(other);
        em.getTransaction().begin();
        try {
            turmaRepository.save(owner);
            em.getTransaction().commit();
            System.out.println("Added Turno to Turma.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void removeTurno() {
        Long ownerId = InputUtils.readLong(scanner, "Turma ID: ");
        Turma owner = turmaRepository.findById(ownerId);
        if (owner == null) { System.out.println("Turma not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Turno ID: ");
        Turno other = turnoRepository.findById(otherId);
        if (other == null) { System.out.println("Turno not found."); return; }

        owner.getTurnos().remove(other);
        em.getTransaction().begin();
        try {
            turmaRepository.save(owner);
            em.getTransaction().commit();
            System.out.println("Turno removed from " + owner);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
}
