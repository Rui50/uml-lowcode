package com.example.app.menus;

import com.example.app.models.*;
import com.example.app.repository.*;
import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.*;

public class LibraryMenu {

    private Scanner scanner;
    private EntityManager em;
    private LibraryRepository libraryRepository;
    private MemberRepository memberRepository;
    private BookRepository bookRepository;

    public LibraryMenu(Scanner scanner, EntityManager em) {
        this.scanner = scanner;
        this.em = em;
        this.libraryRepository = new LibraryRepository(em);
        this.memberRepository = new MemberRepository(em);
        this.bookRepository = new BookRepository(em);
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Library ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Add members");
            System.out.println("7. Remove members");
            System.out.println("8. Add catalog");
            System.out.println("9. Remove catalog");
            System.out.println("0. Back");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: create(); break;
                case 2: viewAll(); break;
                case 3: viewById(); break;
                case 4: update(); break;
                case 5: delete(); break;
                case 6: addMembers(); break;
                case 7: removeMembers(); break;
                case 8: addCatalog(); break;
                case 9: removeCatalog(); break;
                case 0: return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void create() {
        System.out.print("name: ");
        String name = scanner.nextLine();

        Library entity = new Library();
        entity.setName(name);

        em.getTransaction().begin();
        try {
            libraryRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Created: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void viewAll() {
        List<Library> all = libraryRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Library entity : all) {
            System.out.println(entity);
            System.out.println("  members: " + entity.getMembers());
            System.out.println("  catalog: " + entity.getCatalog().values());
        }
    }

    private void viewById() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Library entity = libraryRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
        System.out.println("  members: " + entity.getMembers());
        System.out.println("  catalog: " + entity.getCatalog().values());
    }

    private void update() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Library entity = libraryRepository.findById(id);
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
            libraryRepository.save(entity);
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
        Library entity = libraryRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        em.getTransaction().begin();
        try {
            libraryRepository.deleteById(id);
            em.getTransaction().commit();
            System.out.println("Deleted.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void addMembers() {
        Long ownerId = InputUtils.readLong(scanner, "Library ID: ");
        Library owner = libraryRepository.findById(ownerId);
        if (owner == null) { System.out.println("Library not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Member ID: ");
        Member other = memberRepository.findById(otherId);
        if (other == null) { System.out.println("Member not found."); return; }

        other.setLibrary(owner);   // lado DONO: é o que persiste
        owner.getMembers().add(other);
        em.getTransaction().begin();
        try {
            memberRepository.save(other);
            em.getTransaction().commit();
            System.out.println("Added Member to Library.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void removeMembers() {
        Long ownerId = InputUtils.readLong(scanner, "Library ID: ");
        Library owner = libraryRepository.findById(ownerId);
        if (owner == null) { System.out.println("Library not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Member ID: ");
        Member other = memberRepository.findById(otherId);
        if (other == null) { System.out.println("Member not found."); return; }

        other.setLibrary(null);   // lado DONO: é o que persiste
        owner.getMembers().remove(other);
        em.getTransaction().begin();
        try {
            memberRepository.save(other);
            em.getTransaction().commit();
            System.out.println("Member removed from Library.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void addCatalog() {
        Long ownerId = InputUtils.readLong(scanner, "Library ID: ");
        Library owner = libraryRepository.findById(ownerId);
        if (owner == null) { System.out.println("Library not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Book ID: ");
        Book other = bookRepository.findById(otherId);
        if (other == null) { System.out.println("Book not found."); return; }

        owner.getCatalog().put(other.getIsbn(), other);
        em.getTransaction().begin();
        try {
            libraryRepository.save(owner);
            em.getTransaction().commit();
            em.refresh(other);
            System.out.println("Added Book to Library.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void removeCatalog() {
        Long ownerId = InputUtils.readLong(scanner, "Library ID: ");
        Library owner = libraryRepository.findById(ownerId);
        if (owner == null) { System.out.println("Library not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Book ID: ");
        Book other = bookRepository.findById(otherId);
        if (other == null) { System.out.println("Book not found."); return; }

        owner.getCatalog().remove(other.getIsbn());
        em.getTransaction().begin();
        try {
            libraryRepository.save(owner);
            em.getTransaction().commit();
            em.refresh(other);
            System.out.println("Book removed from Library.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
}
