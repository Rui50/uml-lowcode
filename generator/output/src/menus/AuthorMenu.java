package menus;

import models.*;
import repository.*;
import util.InputUtils;
import java.util.*;
import java.time.*;

public class AuthorMenu {

    private Scanner scanner;
    private AuthorRepository authorRepository;
    private BookRepository bookRepository;

    public AuthorMenu(Scanner scanner, AuthorRepository authorRepository, BookRepository bookRepository) {
        this.scanner = scanner;
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
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

        Author entity = new Author(name);

        authorRepository.save(entity);
        System.out.println("Created: " + entity);
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

        authorRepository.save(entity);
        System.out.println("Updated: " + entity);
    }

    private void delete() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        if (authorRepository.deleteById(id)) {
            System.out.println("Deleted.");
        } else {
            System.out.println("Not found.");
        }
    }

    private void addBooks() {
        Long ownerId = InputUtils.readLong(scanner, "Author ID: ");
        Author owner = authorRepository.findById(ownerId);
        if (owner == null) { System.out.println("Author not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Book ID: ");
        Book other = bookRepository.findById(otherId);
        if (other == null) { System.out.println("Book not found."); return; }

        owner.getBooks().add(other);
        authorRepository.save(owner);
        System.out.println("Added Book to Author.");
    }

    private void removeBooks() {
        Long ownerId = InputUtils.readLong(scanner, "Author ID: ");
        Author owner = authorRepository.findById(ownerId);
        if (owner == null) { System.out.println("Author not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Book ID: ");
        Book other = bookRepository.findById(otherId);
        if (other == null) { System.out.println("Book not found."); return; }

        owner.getBooks().remove(other);
        System.out.println("Book removed from " + owner);
    }

}
