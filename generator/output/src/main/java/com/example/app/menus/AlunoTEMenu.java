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

    public AlunoTEMenu(Scanner scanner, EntityManager em) {
        this.scanner = scanner;
        this.em = em;
        this.alunoTERepository = new AlunoTERepository(em);
    }

    public void run() {
        while (true) {
            System.out.println("\n=== AlunoTE ===");
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
        System.out.print("estado " + java.util.Arrays.toString(EstadoAluno.values()) + ": ");
        EstadoAluno estado = EstadoAluno.valueOf(scanner.nextLine().trim().toUpperCase());
        System.out.print("email: ");
        String email = scanner.nextLine();
        System.out.print("nome: ");
        String nome = scanner.nextLine();
        int numero = InputUtils.readInt(scanner, "numero: ");
        System.out.print("empresa: ");
        String empresa = scanner.nextLine();

        AlunoTE entity = new AlunoTE();
        entity.setEstado(estado);
        entity.setEmail(email);
        entity.setNome(nome);
        entity.setNumero(numero);
        entity.setEmpresa(empresa);

        em.getTransaction().begin();
        try {
            alunoTERepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Created: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
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
        }
    }

    private void viewById() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        AlunoTE entity = alunoTERepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
    }

    private void update() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        AlunoTE entity = alunoTERepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }

        System.out.println("Current: " + entity);
        System.out.println("(press Enter to keep current value)");

        System.out.print("estado [" + entity.getEstado() + "] " + java.util.Arrays.toString(EstadoAluno.values()) + ": ");
        String estadoStr = scanner.nextLine();
        if (!estadoStr.isEmpty()) entity.setEstado(EstadoAluno.valueOf(estadoStr.trim().toUpperCase()));
        System.out.print("email [" + entity.getEmail() +"]: ");
        String email = scanner.nextLine();
        if (!email.isEmpty()) entity.setEmail(email);
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
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void delete() {
        Long id = InputUtils.readLong(scanner, "ID: ");
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
            System.out.println("Error: " + ex.getMessage());
        }
    }
}
