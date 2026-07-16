package menus;

import models.*;
import repository.*;
import util.InputUtils;
import java.util.*;
import java.time.*;

public class LibraryCardMenu {

    private Scanner scanner;
    private LibraryCardRepository libraryCardRepository;
    private MemberRepository memberRepository;

    public LibraryCardMenu(Scanner scanner, LibraryCardRepository libraryCardRepository, MemberRepository memberRepository) {
        this.scanner = scanner;
        this.libraryCardRepository = libraryCardRepository;
        this.memberRepository = memberRepository;
    }

    public void run() {
        while (true) {
            System.out.println("\n=== LibraryCard ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Assign member");
            System.out.println("7. Remove member");
            System.out.println("0. Back");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: create(); break;
                case 2: viewAll(); break;
                case 3: viewById(); break;
                case 4: update(); break;
                case 5: delete(); break;
                case 6: assignMember(); break;
                case 7: removeMember(); break;
                case 0: return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void create() {
        System.out.print("cardNumber: ");
        String cardNumber = scanner.nextLine();
        int issuedDay = InputUtils.readInt(scanner, "issuedDay: ");

        LibraryCard entity = new LibraryCard(cardNumber, issuedDay);

        libraryCardRepository.save(entity);
        System.out.println("Created: " + entity);
    }

    private void viewAll() {
        List<LibraryCard> all = libraryCardRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (LibraryCard entity : all) {
            System.out.println(entity);
            System.out.println("  member: " + entity.getMember());
        }
    }

    private void viewById() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        LibraryCard entity = libraryCardRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
        System.out.println("  member: " + entity.getMember());
    }

    private void update() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        LibraryCard entity = libraryCardRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }

        System.out.println("Current: " + entity);
        System.out.println("(press Enter to keep current value)");

        System.out.print("cardNumber [" + entity.getCardNumber() +"]: ");
        String cardNumber = scanner.nextLine();
        if (!cardNumber.isEmpty()) entity.setCardNumber(cardNumber);
        System.out.print("issuedDay [" + entity.getIssuedDay() +"]: ");
        String issuedDayStr = scanner.nextLine();
        if (!issuedDayStr.isEmpty()) entity.setIssuedDay(Integer.parseInt(issuedDayStr));

        libraryCardRepository.save(entity);
        System.out.println("Updated: " + entity);
    }

    private void delete() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        if (libraryCardRepository.deleteById(id)) {
            System.out.println("Deleted.");
        } else {
            System.out.println("Not found.");
        }
    }

    private void assignMember() {
        Long ownerId = InputUtils.readLong(scanner, "LibraryCard ID: ");
        LibraryCard owner = libraryCardRepository.findById(ownerId);
        if (owner == null) { System.out.println("LibraryCard not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Member ID: ");
        Member other = memberRepository.findById(otherId);
        if (other == null) { System.out.println("Member not found."); return; }

        owner.setMember(other);
        libraryCardRepository.save(owner);
        System.out.println("Assigned Member to LibraryCard.");
    }

    private void removeMember() {
        Long id = InputUtils.readLong(scanner, "LibraryCard ID: ");
        LibraryCard entity = libraryCardRepository.findById(id);
        if (entity == null) { System.out.println("LibraryCard not found."); return; }

        entity.setMember(null);
        System.out.println("Member removed from " + entity);
    }

}
