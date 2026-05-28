package com.example.app.menus;

import com.example.app.models.*;
import com.example.app.repository.*;
import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;

public class AlunoMenu {

    private Scanner scanner;
    private EntityManager em;
    private AlunoRepository alunoRepository;
    private TurnoRepository turnoRepository;

    public AlunoMenu(Scanner scanner, EntityManager em) {
        this.scanner = scanner;
        this.em = em;
        this.alunoRepository = new AlunoRepository(em);
        this.turnoRepository = new TurnoRepository(em);
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Aluno ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Assign Turno");
            System.out.println("7. Remove Turno");
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
        System.out.print("nome: ");
        String nome = scanner.nextLine();
        System.out.print("email: ");
        String email = scanner.nextLine();
        int numero = InputUtils.readInt(scanner, "numero: ");

        Aluno entity = new Aluno();
        entity.setNome(nome);
        entity.setEmail(email);
        entity.setNumero(numero);

        em.getTransaction().begin();
        try {
            alunoRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Created: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
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
        String id = InputUtils.readString(scanner, "ID: ");
        Aluno entity = alunoRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
        System.out.println("  turno: " + entity.getTurno());
    }

    private void update() {
        String id = InputUtils.readString(scanner, "ID: ");
        Aluno entity = alunoRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }

        System.out.println("Current: " + entity);
        System.out.println("(press Enter to keep current value)");

        System.out.print("nome [" + entity.getNome() +"]: ");
        String nome = scanner.nextLine();
        if (!nome.isEmpty()) entity.setNome(nome);
        System.out.print("numero [" + entity.getNumero() +"]: ");
        String numeroStr = scanner.nextLine();
        if (!numeroStr.isEmpty()) entity.setNumero(Integer.parseInt(numeroStr));

        em.getTransaction().begin();
        try {
            alunoRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Updated: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void delete() {
        String id = InputUtils.readString(scanner, "ID: ");
        Aluno entity = alunoRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        em.getTransaction().begin();
        try {
            alunoRepository.deleteById(id);
            em.getTransaction().commit();
            System.out.println("Deleted.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void assignTurno() {
        String ownerId = InputUtils.readString(scanner, "Aluno ID: ");
        Aluno owner = alunoRepository.findById(ownerId);
        if (owner == null) { System.out.println("Aluno not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Turno ID: ");
        Turno other = turnoRepository.findById(otherId);
        if (other == null) { System.out.println("Turno not found."); return; }

        owner.setTurno(other);
        em.getTransaction().begin();
        try {
            alunoRepository.save(owner);
            em.getTransaction().commit();
            em.refresh(other);
            System.out.println("Assigned Turno to Aluno.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void removeTurno() {
        String id = InputUtils.readString(scanner, "Aluno ID: ");
        Aluno entity = alunoRepository.findById(id);
        if (entity == null) { System.out.println("Aluno not found."); return; }

        Turno previous = entity.getTurno();
        entity.setTurno(null);
        em.getTransaction().begin();
        try {
            alunoRepository.save(entity);
            em.getTransaction().commit();
            if (previous != null) em.refresh(previous);
            System.out.println("Turno removed from " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

}
