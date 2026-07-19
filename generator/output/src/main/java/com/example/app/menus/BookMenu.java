package com.example.app.menus;

import com.example.app.models.*;
import com.example.app.repository.*;
import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.*;

public class BookMenu {

    private Scanner scanner;
    private EntityManager em;
    private BookRepository bookRepository;
    private LibraryRepository libraryRepository;
    private LoanRepository loanRepository;
    private AuthorRepository authorRepository;

    public BookMenu(Scanner scanner, EntityManager em) {
        this.scanner = scanner;
        this.em = em;
        this.bookRepository = new BookRepository(em);
        this.libraryRepository = new LibraryRepository(em);
        this.loanRepository = new LoanRepository(em);
        this.authorRepository = new AuthorRepository(em);
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Book ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Add library");
            System.out.println("7. Remove library");
            System.out.println("8. Add loans");
            System.out.println("9. Remove loans");
            System.out.println("10. Add authors");
            System.out.println("11. Remove authors");
            System.out.println("0. Back");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: create(); break;
                case 2: viewAll(); break;
                case 3: viewById(); break;
                case 4: update(); break;
                case 5: delete(); break;
                case 6: addLibrary(); break;
                case 7: removeLibrary(); break;
                case 8: addLoans(); break;
                case 9: removeLoans(); break;
                case 10: addAuthors(); break;
                case 11: removeAuthors(); break;
                case 0: return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void create() {
        System.out.print("isbn: ");
        String isbn = scanner.nextLine();
        System.out.print("title: ");
        String title = scanner.nextLine();
        BookStatus status = null;
        while (status == null) {
            System.out.print("status " + java.util.Arrays.toString(BookStatus.values()) + ": ");
            try { status = BookStatus.valueOf(scanner.nextLine().trim().toUpperCase()); }
            catch (IllegalArgumentException ex) { System.out.println("Invalid value. Try again."); }
        }

        Book entity = new Book();
        entity.setIsbn(isbn);
        entity.setTitle(title);
        entity.setStatus(status);

        em.getTransaction().begin();
        try {
            bookRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Created: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void viewAll() {
        List<Book> all = bookRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Book entity : all) {
            System.out.println(entity);
            System.out.println("  library: " + entity.getLibrary().values());
            System.out.println("  loans: " + entity.getLoans());
            System.out.println("  authors: " + entity.getAuthors());
        }
    }

    private void viewById() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Book entity = bookRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
        System.out.println("  library: " + entity.getLibrary().values());
        System.out.println("  loans: " + entity.getLoans());
        System.out.println("  authors: " + entity.getAuthors());
    }

    private void update() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Book entity = bookRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }

        System.out.println("Current: " + entity);
        System.out.println("(press Enter to keep current value)");

        System.out.print("isbn [" + entity.getIsbn() +"]: ");
        String isbn = scanner.nextLine();
        if (!isbn.isEmpty()) entity.setIsbn(isbn);
        System.out.print("title [" + entity.getTitle() +"]: ");
        String title = scanner.nextLine();
        if (!title.isEmpty()) entity.setTitle(title);
        while (true) {
            System.out.print("status [" + entity.getStatus() + "] " + java.util.Arrays.toString(BookStatus.values()) + ": ");
            String statusStr = scanner.nextLine();
            if (statusStr.isEmpty()) break;
            try { entity.setStatus(BookStatus.valueOf(statusStr.trim().toUpperCase())); break; }
            catch (IllegalArgumentException ex) { System.out.println("Invalid value. Try again."); }
        }

        em.getTransaction().begin();
        try {
            bookRepository.save(entity);
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
        Book entity = bookRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        em.getTransaction().begin();
        try {
            bookRepository.deleteById(id);
            em.getTransaction().commit();
            System.out.println("Deleted.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void addLibrary() {
        Long ownerId = InputUtils.readLong(scanner, "Book ID: ");
        Book owner = bookRepository.findById(ownerId);
        if (owner == null) { System.out.println("Book not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Library ID: ");
        Library other = libraryRepository.findById(otherId);
        if (other == null) { System.out.println("Library not found."); return; }

        owner.getLibrary().put(other.getIsbn(), other);
        em.getTransaction().begin();
        try {
            bookRepository.save(owner);
            em.getTransaction().commit();
            em.refresh(other);
            System.out.println("Added Library to Book.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void removeLibrary() {
        Long ownerId = InputUtils.readLong(scanner, "Book ID: ");
        Book owner = bookRepository.findById(ownerId);
        if (owner == null) { System.out.println("Book not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Library ID: ");
        Library other = libraryRepository.findById(otherId);
        if (other == null) { System.out.println("Library not found."); return; }

        owner.getLibrary().remove(other.getIsbn());
        em.getTransaction().begin();
        try {
            bookRepository.save(owner);
            em.getTransaction().commit();
            em.refresh(other);
            System.out.println("Library removed from Book.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void addLoans() {
        Long ownerId = InputUtils.readLong(scanner, "Book ID: ");
        Book owner = bookRepository.findById(ownerId);
        if (owner == null) { System.out.println("Book not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Loan ID: ");
        Loan other = loanRepository.findById(otherId);
        if (other == null) { System.out.println("Loan not found."); return; }

        other.setBook(owner);   // lado DONO: é o que persiste
        owner.getLoans().add(other);
        em.getTransaction().begin();
        try {
            loanRepository.save(other);
            em.getTransaction().commit();
            System.out.println("Added Loan to Book.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void removeLoans() {
        Long ownerId = InputUtils.readLong(scanner, "Book ID: ");
        Book owner = bookRepository.findById(ownerId);
        if (owner == null) { System.out.println("Book not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Loan ID: ");
        Loan other = loanRepository.findById(otherId);
        if (other == null) { System.out.println("Loan not found."); return; }

        other.setBook(null);   // lado DONO: é o que persiste
        owner.getLoans().remove(other);
        em.getTransaction().begin();
        try {
            loanRepository.save(other);
            em.getTransaction().commit();
            System.out.println("Loan removed from Book.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void addAuthors() {
        Long ownerId = InputUtils.readLong(scanner, "Book ID: ");
        Book owner = bookRepository.findById(ownerId);
        if (owner == null) { System.out.println("Book not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Author ID: ");
        Author other = authorRepository.findById(otherId);
        if (other == null) { System.out.println("Author not found."); return; }

        owner.getAuthors().add(other);
        other.getBooks().add(owner);
        em.getTransaction().begin();
        try {
            bookRepository.save(owner);
            em.getTransaction().commit();
            System.out.println("Added Author to Book.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void removeAuthors() {
        Long ownerId = InputUtils.readLong(scanner, "Book ID: ");
        Book owner = bookRepository.findById(ownerId);
        if (owner == null) { System.out.println("Book not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Author ID: ");
        Author other = authorRepository.findById(otherId);
        if (other == null) { System.out.println("Author not found."); return; }

        owner.getAuthors().remove(other);
        other.getBooks().remove(owner);
        em.getTransaction().begin();
        try {
            bookRepository.save(owner);
            em.getTransaction().commit();
            System.out.println("Author removed from Book.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
}
