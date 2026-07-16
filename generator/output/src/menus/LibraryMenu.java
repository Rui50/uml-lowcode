package menus;

import models.*;
import repository.*;
import util.InputUtils;
import java.util.*;
import java.time.*;

public class LibraryMenu {

    private Scanner scanner;
    private LibraryRepository libraryRepository;
    private MemberRepository memberRepository;
    private BookRepository bookRepository;

    public LibraryMenu(Scanner scanner, LibraryRepository libraryRepository, MemberRepository memberRepository, BookRepository bookRepository) {
        this.scanner = scanner;
        this.libraryRepository = libraryRepository;
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
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

        Library entity = new Library(name);

        libraryRepository.save(entity);
        System.out.println("Created: " + entity);
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

        libraryRepository.save(entity);
        System.out.println("Updated: " + entity);
    }

    private void delete() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        if (libraryRepository.deleteById(id)) {
            System.out.println("Deleted.");
        } else {
            System.out.println("Not found.");
        }
    }

    private void addMembers() {
        Long ownerId = InputUtils.readLong(scanner, "Library ID: ");
        Library owner = libraryRepository.findById(ownerId);
        if (owner == null) { System.out.println("Library not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Member ID: ");
        Member other = memberRepository.findById(otherId);
        if (other == null) { System.out.println("Member not found."); return; }

        owner.getMembers().add(other);
        libraryRepository.save(owner);
        System.out.println("Added Member to Library.");
    }

    private void removeMembers() {
        Long ownerId = InputUtils.readLong(scanner, "Library ID: ");
        Library owner = libraryRepository.findById(ownerId);
        if (owner == null) { System.out.println("Library not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Member ID: ");
        Member other = memberRepository.findById(otherId);
        if (other == null) { System.out.println("Member not found."); return; }

        owner.getMembers().remove(other);
        System.out.println("Member removed from " + owner);
    }

    private void addCatalog() {
        Long ownerId = InputUtils.readLong(scanner, "Library ID: ");
        Library owner = libraryRepository.findById(ownerId);
        if (owner == null) { System.out.println("Library not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Book ID: ");
        Book other = bookRepository.findById(otherId);
        if (other == null) { System.out.println("Book not found."); return; }

        owner.getCatalog().put(other.getIsbn(), other);
        libraryRepository.save(owner);
        System.out.println("Added Book to Library.");
    }

    private void removeCatalog() {
        Long ownerId = InputUtils.readLong(scanner, "Library ID: ");
        Library owner = libraryRepository.findById(ownerId);
        if (owner == null) { System.out.println("Library not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Book ID: ");
        Book other = bookRepository.findById(otherId);
        if (other == null) { System.out.println("Book not found."); return; }

        owner.getCatalog().remove(other.getIsbn());
        System.out.println("Book removed from " + owner);
    }

}
