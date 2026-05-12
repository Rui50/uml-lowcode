package com.example.app.menus;

import com.example.app.models.*;
import com.example.app.repository.*;
import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;

public class testeSubAbMenu {

    private Scanner scanner;
    private EntityManager em;
    private testeSubAbRepository testeSubAbRepository;

    public testeSubAbMenu(Scanner scanner, EntityManager em) {
        this.scanner = scanner;
        this.em = em;
        this.testeSubAbRepository = new testeSubAbRepository(em);
    }

    public void run() {
        while (true) {
            System.out.println("\n=== testeSubAb ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("0. Back");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: create(); break;
                case 2: viewAll(); break;
                case 3: viewById(); break;
                case 4: update(); break;
                case 5: delete(); break;
                case 0: return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void create() {
        System.out.print("testar: ");
        String testar = scanner.nextLine();
        System.out.print("nada: ");
        String nada = scanner.nextLine();

        testeSubAb entity = new testeSubAb();
        entity.setTestar(testar);
        entity.setNada(nada);

        em.getTransaction().begin();
        try {
            testeSubAbRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Created: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void viewAll() {
        List<testeSubAb> all = testeSubAbRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (testeSubAb entity : all) {
            System.out.println(entity);
        }
    }

    private void viewById() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        testeSubAb entity = testeSubAbRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
    }

    private void update() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        testeSubAb entity = testeSubAbRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }

        System.out.println("Current: " + entity);
        System.out.println("(press Enter to keep current value)");

        System.out.print("testar [" + entity.getTestar() +"]: ");
        String testar = scanner.nextLine();
        if (!testar.isEmpty()) entity.setTestar(testar);
        System.out.print("nada [" + entity.getNada() +"]: ");
        String nada = scanner.nextLine();
        if (!nada.isEmpty()) entity.setNada(nada);

        em.getTransaction().begin();
        try {
            testeSubAbRepository.save(entity);
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
        testeSubAb entity = testeSubAbRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        em.getTransaction().begin();
        try {
            testeSubAbRepository.deleteById(id);
            em.getTransaction().commit();
            System.out.println("Deleted.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
}
