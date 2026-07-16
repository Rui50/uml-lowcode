package menus;

import models.*;
import repository.*;
import util.InputUtils;
import java.util.*;
import java.time.*;

public class StaffMemberMenu {

    private Scanner scanner;
    private StaffMemberRepository staffMemberRepository;
    private LibraryRepository libraryRepository;
    private LoanRepository loanRepository;
    private LibraryCardRepository libraryCardRepository;

    public StaffMemberMenu(Scanner scanner, StaffMemberRepository staffMemberRepository, LibraryRepository libraryRepository, LoanRepository loanRepository, LibraryCardRepository libraryCardRepository) {
        this.scanner = scanner;
        this.staffMemberRepository = staffMemberRepository;
        this.libraryRepository = libraryRepository;
        this.loanRepository = loanRepository;
        this.libraryCardRepository = libraryCardRepository;
    }

    public void run() {
        while (true) {
            System.out.println("\n=== StaffMember ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Assign library");
            System.out.println("7. Remove library");
            System.out.println("8. Add loans");
            System.out.println("9. Remove loans");
            System.out.println("10. Assign card");
            System.out.println("11. Remove card");
            System.out.println("12. RedeemPoints");
            System.out.println("0. Back");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: create(); break;
                case 2: viewAll(); break;
                case 3: viewById(); break;
                case 4: update(); break;
                case 5: delete(); break;
                case 6: assignLibrary(); break;
                case 7: removeLibrary(); break;
                case 8: addLoans(); break;
                case 9: removeLoans(); break;
                case 10: assignCard(); break;
                case 11: removeCard(); break;
                case 12: redeemPoints(); break;
                case 0: return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void create() {
        int memberNumber = InputUtils.readInt(scanner, "memberNumber: ");
        System.out.print("name: ");
        String name = scanner.nextLine();
        System.out.print("email: ");
        String email = scanner.nextLine();
        int points = InputUtils.readInt(scanner, "points: ");
        int activeLoans = InputUtils.readInt(scanner, "activeLoans: ");
        System.out.print("department: ");
        String department = scanner.nextLine();

        StaffMember entity = new StaffMember(memberNumber, name, email, points, activeLoans, department);

        staffMemberRepository.save(entity);
        System.out.println("Created: " + entity);
    }

    private void viewAll() {
        List<StaffMember> all = staffMemberRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (StaffMember entity : all) {
            System.out.println(entity);
            System.out.println("  library: " + entity.getLibrary());
            System.out.println("  loans: " + entity.getLoans());
            System.out.println("  card: " + entity.getCard());
        }
    }

    private void viewById() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        StaffMember entity = staffMemberRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
        System.out.println("  library: " + entity.getLibrary());
        System.out.println("  loans: " + entity.getLoans());
        System.out.println("  card: " + entity.getCard());
    }

    private void update() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        StaffMember entity = staffMemberRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }

        System.out.println("Current: " + entity);
        System.out.println("(press Enter to keep current value)");

        System.out.print("memberNumber [" + entity.getMemberNumber() +"]: ");
        String memberNumberStr = scanner.nextLine();
        if (!memberNumberStr.isEmpty()) entity.setMemberNumber(Integer.parseInt(memberNumberStr));
        System.out.print("name [" + entity.getName() +"]: ");
        String name = scanner.nextLine();
        if (!name.isEmpty()) entity.setName(name);
        System.out.print("email [" + entity.getEmail() +"]: ");
        String email = scanner.nextLine();
        if (!email.isEmpty()) entity.setEmail(email);
        System.out.print("points [" + entity.getPoints() +"]: ");
        String pointsStr = scanner.nextLine();
        if (!pointsStr.isEmpty()) entity.setPoints(Integer.parseInt(pointsStr));
        System.out.print("activeLoans [" + entity.getActiveLoans() +"]: ");
        String activeLoansStr = scanner.nextLine();
        if (!activeLoansStr.isEmpty()) entity.setActiveLoans(Integer.parseInt(activeLoansStr));
        System.out.print("department [" + entity.getDepartment() +"]: ");
        String department = scanner.nextLine();
        if (!department.isEmpty()) entity.setDepartment(department);

        staffMemberRepository.save(entity);
        System.out.println("Updated: " + entity);
    }

    private void delete() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        if (staffMemberRepository.deleteById(id)) {
            System.out.println("Deleted.");
        } else {
            System.out.println("Not found.");
        }
    }

    private void assignLibrary() {
        Long ownerId = InputUtils.readLong(scanner, "StaffMember ID: ");
        StaffMember owner = staffMemberRepository.findById(ownerId);
        if (owner == null) { System.out.println("StaffMember not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Library ID: ");
        Library other = libraryRepository.findById(otherId);
        if (other == null) { System.out.println("Library not found."); return; }

        owner.setLibrary(other);
        staffMemberRepository.save(owner);
        System.out.println("Assigned Library to StaffMember.");
    }

    private void removeLibrary() {
        Long id = InputUtils.readLong(scanner, "StaffMember ID: ");
        StaffMember entity = staffMemberRepository.findById(id);
        if (entity == null) { System.out.println("StaffMember not found."); return; }

        entity.setLibrary(null);
        System.out.println("Library removed from " + entity);
    }

    private void addLoans() {
        Long ownerId = InputUtils.readLong(scanner, "StaffMember ID: ");
        StaffMember owner = staffMemberRepository.findById(ownerId);
        if (owner == null) { System.out.println("StaffMember not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Loan ID: ");
        Loan other = loanRepository.findById(otherId);
        if (other == null) { System.out.println("Loan not found."); return; }

        owner.getLoans().add(other);
        staffMemberRepository.save(owner);
        System.out.println("Added Loan to StaffMember.");
    }

    private void removeLoans() {
        Long ownerId = InputUtils.readLong(scanner, "StaffMember ID: ");
        StaffMember owner = staffMemberRepository.findById(ownerId);
        if (owner == null) { System.out.println("StaffMember not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Loan ID: ");
        Loan other = loanRepository.findById(otherId);
        if (other == null) { System.out.println("Loan not found."); return; }

        owner.getLoans().remove(other);
        System.out.println("Loan removed from " + owner);
    }

    private void assignCard() {
        Long ownerId = InputUtils.readLong(scanner, "StaffMember ID: ");
        StaffMember owner = staffMemberRepository.findById(ownerId);
        if (owner == null) { System.out.println("StaffMember not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "LibraryCard ID: ");
        LibraryCard other = libraryCardRepository.findById(otherId);
        if (other == null) { System.out.println("LibraryCard not found."); return; }

        owner.setCard(other);
        staffMemberRepository.save(owner);
        System.out.println("Assigned LibraryCard to StaffMember.");
    }

    private void removeCard() {
        Long id = InputUtils.readLong(scanner, "StaffMember ID: ");
        StaffMember entity = staffMemberRepository.findById(id);
        if (entity == null) { System.out.println("StaffMember not found."); return; }

        entity.setCard(null);
        System.out.println("LibraryCard removed from " + entity);
    }

    private void redeemPoints() {
        Long ownerId = InputUtils.readLong(scanner, "StaffMember ID: ");
        StaffMember owner = staffMemberRepository.findById(ownerId);
        if (owner == null) { System.out.println("StaffMember not found."); return; }

        int amount = InputUtils.readInt(scanner, "amount: ");

        try {
            owner.redeemPoints(amount);
            staffMemberRepository.save(owner);
            System.out.println("redeemPoints executed.");
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

}
