package com.example.app.menus;

import com.example.app.models.*;
import com.example.app.repository.*;
import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.*;

public class AuthorMenu {

    private Scanner scanner;
    private EntityManager em;
    private AuthorRepository authorRepository;
    private BookRepository bookRepository;

    public AuthorMenu(Scanner scanner, EntityManager em) {
        this.scanner = scanner;
        this.em = em;
        this.authorRepository = new AuthorRepository(em);
        this.bookRepository = new BookRepository(em);
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Author ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Add books");
            System.out.println("7. Remove books");
            System.out.println("0. Back");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: create(); break;
                case 2: viewAll(); break;
                case 3: viewById(); break;
                case 4: update(); break;
                case 5: delete(); break;
                case 6: addBooks(); break;
                case 7: removeBooks(); break;
                case 0: return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void create() {
        System.out.print("name: ");
        String name = scanner.nextLine();

        Author entity = new Author();
        entity.setName(name);

        em.getTransaction().begin();
        try {
            authorRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Created: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void viewAll() {
        List<Author> all = authorRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Author entity : all) {
            System.out.println(entity);
            System.out.println("  books: " + entity.getBooks());
        }
    }

    private void viewById() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Author entity = authorRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
        System.out.println("  books: " + entity.getBooks());
    }

    private void update() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Author entity = authorRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }

        System.out.println("Current: " + entity);
        System.out.println("(press Enter to keep current value)");

        System.out.print("name [" + entity.getName() +"]: ");
        String name = scanner.nextLine();
        if (!name.isEmpty()) entity.setName(name);

        em.getTransaction().begin();
        try {
            authorRepository.save(entity);
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
        Author entity = authorRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        em.getTransaction().begin();
        try {
            authorRepository.deleteById(id);
            em.getTransaction().commit();
            System.out.println("Deleted.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void addBooks() {
        Long ownerId = InputUtils.readLong(scanner, "Author ID: ");
        Author owner = authorRepository.findById(ownerId);
        if (owner == null) { System.out.println("Author not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Book ID: ");
        Book other = bookRepository.findById(otherId);
        if (other == null) { System.out.println("Book not found."); return; }

        other.getAuthors().add(owner);   // lado DONO: é o que persiste
        owner.getBooks().add(other);
        em.getTransaction().begin();
        try {
            bookRepository.save(other);
            em.getTransaction().commit();
            System.out.println("Added Book to Author.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void removeBooks() {
        Long ownerId = InputUtils.readLong(scanner, "Author ID: ");
        Author owner = authorRepository.findById(ownerId);
        if (owner == null) { System.out.println("Author not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Book ID: ");
        Book other = bookRepository.findById(otherId);
        if (other == null) { System.out.println("Book not found."); return; }

        other.getAuthors().remove(owner);   // lado DONO: é o que persiste
        owner.getBooks().remove(other);
        em.getTransaction().begin();
        try {
            bookRepository.save(other);
            em.getTransaction().commit();
            System.out.println("Book removed from Author.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
}
