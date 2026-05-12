package com.example.app.menus;

import com.example.app.models.*;
import com.example.app.repository.*;
import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;

public class ImplementInterfaceMenu {

    private Scanner scanner;
    private EntityManager em;
    private ImplementInterfaceRepository implementInterfaceRepository;

    public ImplementInterfaceMenu(Scanner scanner, EntityManager em) {
        this.scanner = scanner;
        this.em = em;
        this.implementInterfaceRepository = new ImplementInterfaceRepository(em);
    }

    public void run() {
        while (true) {
            System.out.println("\n=== ImplementInterface ===");
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

        ImplementInterface entity = new ImplementInterface();

        em.getTransaction().begin();
        try {
            implementInterfaceRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Created: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void viewAll() {
        List<ImplementInterface> all = implementInterfaceRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (ImplementInterface entity : all) {
            System.out.println(entity);
        }
    }

    private void viewById() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        ImplementInterface entity = implementInterfaceRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
    }

    private void update() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        ImplementInterface entity = implementInterfaceRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }

        System.out.println("Current: " + entity);
        System.out.println("(press Enter to keep current value)");


        em.getTransaction().begin();
        try {
            implementInterfaceRepository.save(entity);
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
        ImplementInterface entity = implementInterfaceRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        em.getTransaction().begin();
        try {
            implementInterfaceRepository.deleteById(id);
            em.getTransaction().commit();
            System.out.println("Deleted.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
}
