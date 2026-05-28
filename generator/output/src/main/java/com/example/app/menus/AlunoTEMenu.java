package com.example.app.menus;

import com.example.app.models.*;
import com.example.app.repository.*;
import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;

public class AlunoTEMenu {

    private Scanner scanner;
    private EntityManager em;
    private AlunoTERepository alunoTERepository;
    private TurnoRepository turnoRepository;

    public AlunoTEMenu(Scanner scanner, EntityManager em) {
        this.scanner = scanner;
        this.em = em;
        this.alunoTERepository = new AlunoTERepository(em);
        this.turnoRepository = new TurnoRepository(em);
    }

    public void run() {
        while (true) {
            System.out.println("\n=== AlunoTE ===");
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
        System.out.print("empresa: ");
        String empresa = scanner.nextLine();

        AlunoTE entity = new AlunoTE();
        entity.setNome(nome);
        entity.setEmail(email);
        entity.setNumero(numero);
        entity.setEmpresa(empresa);

        em.getTransaction().begin();
        try {
            alunoTERepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Created: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void viewAll() {
        List<AlunoTE> all = alunoTERepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (AlunoTE entity : all) {
            System.out.println(entity);
            System.out.println("  turno: " + entity.getTurno());
        }
    }

    private void viewById() {
        String id = InputUtils.readString(scanner, "ID: ");
        AlunoTE entity = alunoTERepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
        System.out.println("  turno: " + entity.getTurno());
    }

    private void update() {
        String id = InputUtils.readString(scanner, "ID: ");
        AlunoTE entity = alunoTERepository.findById(id);
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
        System.out.print("empresa [" + entity.getEmpresa() +"]: ");
        String empresa = scanner.nextLine();
        if (!empresa.isEmpty()) entity.setEmpresa(empresa);

        em.getTransaction().begin();
        try {
            alunoTERepository.save(entity);
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
        AlunoTE entity = alunoTERepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        em.getTransaction().begin();
        try {
            alunoTERepository.deleteById(id);
            em.getTransaction().commit();
            System.out.println("Deleted.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void assignTurno() {
        String ownerId = InputUtils.readString(scanner, "AlunoTE ID: ");
        AlunoTE owner = alunoTERepository.findById(ownerId);
        if (owner == null) { System.out.println("AlunoTE not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Turno ID: ");
        Turno other = turnoRepository.findById(otherId);
        if (other == null) { System.out.println("Turno not found."); return; }

        owner.setTurno(other);
        em.getTransaction().begin();
        try {
            alunoTERepository.save(owner);
            em.getTransaction().commit();
            em.refresh(other);
            System.out.println("Assigned Turno to AlunoTE.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void removeTurno() {
        String id = InputUtils.readString(scanner, "AlunoTE ID: ");
        AlunoTE entity = alunoTERepository.findById(id);
        if (entity == null) { System.out.println("AlunoTE not found."); return; }

        Turno previous = entity.getTurno();
        entity.setTurno(null);
        em.getTransaction().begin();
        try {
            alunoTERepository.save(entity);
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
