package menus;

import models.*;
import repository.*;
import util.InputUtils;
import java.util.*;
import java.time.*;

public class BookMenu {

    private Scanner scanner;
    private BookRepository bookRepository;
    private LoanRepository loanRepository;
    private AuthorRepository authorRepository;

    public BookMenu(Scanner scanner, BookRepository bookRepository, LoanRepository loanRepository, AuthorRepository authorRepository) {
        this.scanner = scanner;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.authorRepository = authorRepository;
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Book ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Add loans");
            System.out.println("7. Remove loans");
            System.out.println("8. Add authors");
            System.out.println("9. Remove authors");
            System.out.println("0. Back");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: create(); break;
                case 2: viewAll(); break;
                case 3: viewById(); break;
                case 4: update(); break;
                case 5: delete(); break;
                case 6: addLoans(); break;
                case 7: removeLoans(); break;
                case 8: addAuthors(); break;
                case 9: removeAuthors(); break;
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

        Book entity = new Book(isbn, title, status);

        bookRepository.save(entity);
        System.out.println("Created: " + entity);
    }

    private void viewAll() {
        List<Book> all = bookRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Book entity : all) {
            System.out.println(entity);
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

        bookRepository.save(entity);
        System.out.println("Updated: " + entity);
    }

    private void delete() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        if (bookRepository.deleteById(id)) {
            System.out.println("Deleted.");
        } else {
            System.out.println("Not found.");
        }
    }

    private void addLoans() {
        Long ownerId = InputUtils.readLong(scanner, "Book ID: ");
        Book owner = bookRepository.findById(ownerId);
        if (owner == null) { System.out.println("Book not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Loan ID: ");
        Loan other = loanRepository.findById(otherId);
        if (other == null) { System.out.println("Loan not found."); return; }

        owner.getLoans().add(other);
        bookRepository.save(owner);
        System.out.println("Added Loan to Book.");
    }

    private void removeLoans() {
        Long ownerId = InputUtils.readLong(scanner, "Book ID: ");
        Book owner = bookRepository.findById(ownerId);
        if (owner == null) { System.out.println("Book not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Loan ID: ");
        Loan other = loanRepository.findById(otherId);
        if (other == null) { System.out.println("Loan not found."); return; }

        owner.getLoans().remove(other);
        System.out.println("Loan removed from " + owner);
    }

    private void addAuthors() {
        Long ownerId = InputUtils.readLong(scanner, "Book ID: ");
        Book owner = bookRepository.findById(ownerId);
        if (owner == null) { System.out.println("Book not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Author ID: ");
        Author other = authorRepository.findById(otherId);
        if (other == null) { System.out.println("Author not found."); return; }

        owner.getAuthors().add(other);
        bookRepository.save(owner);
        System.out.println("Added Author to Book.");
    }

    private void removeAuthors() {
        Long ownerId = InputUtils.readLong(scanner, "Book ID: ");
        Book owner = bookRepository.findById(ownerId);
        if (owner == null) { System.out.println("Book not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Author ID: ");
        Author other = authorRepository.findById(otherId);
        if (other == null) { System.out.println("Author not found."); return; }

        owner.getAuthors().remove(other);
        System.out.println("Author removed from " + owner);
    }

}
