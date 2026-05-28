package com.example.app.menus;

import com.example.app.models.*;
import com.example.app.repository.*;
import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;

public class TurnoMenu {

    private Scanner scanner;
    private EntityManager em;
    private TurnoRepository turnoRepository;
    private AlunoRepository alunoRepository;

    public TurnoMenu(Scanner scanner, EntityManager em) {
        this.scanner = scanner;
        this.em = em;
        this.turnoRepository = new TurnoRepository(em);
        this.alunoRepository = new AlunoRepository(em);
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Turno ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Add Aluno");
            System.out.println("7. Remove Aluno");
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
                case 0: return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void create() {
        int capacidade = InputUtils.readInt(scanner, "capacidade: ");

        Turno entity = new Turno();
        entity.setCapacidade(capacidade);

        em.getTransaction().begin();
        try {
            turnoRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Created: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
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

        em.getTransaction().begin();
        try {
            turnoRepository.save(entity);
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
        Turno entity = turnoRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        em.getTransaction().begin();
        try {
            turnoRepository.deleteById(id);
            em.getTransaction().commit();
            System.out.println("Deleted.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void addAluno() {
        Long ownerId = InputUtils.readLong(scanner, "Turno ID: ");
        Turno owner = turnoRepository.findById(ownerId);
        if (owner == null) { System.out.println("Turno not found."); return; }

        String otherId = InputUtils.readString(scanner, "Aluno ID: ");
        Aluno other = alunoRepository.findById(otherId);
        if (other == null) { System.out.println("Aluno not found."); return; }

        owner.getAlunos().add(other);
        em.getTransaction().begin();
        try {
            turnoRepository.save(owner);
            em.getTransaction().commit();
            em.refresh(other);
            System.out.println("Added Aluno to Turno.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void removeAluno() {
        Long ownerId = InputUtils.readLong(scanner, "Turno ID: ");
        Turno owner = turnoRepository.findById(ownerId);
        if (owner == null) { System.out.println("Turno not found."); return; }

        String otherId = InputUtils.readString(scanner, "Aluno ID: ");
        Aluno other = alunoRepository.findById(otherId);
        if (other == null) { System.out.println("Aluno not found."); return; }

        owner.getAlunos().remove(other);
        em.getTransaction().begin();
        try {
            turnoRepository.save(owner);
            em.getTransaction().commit();
            em.refresh(other);
            System.out.println("Aluno removed from " + owner);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
}
