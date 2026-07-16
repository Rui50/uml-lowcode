package menus;

import models.*;
import repository.*;
import util.InputUtils;
import java.util.*;
import java.time.*;

public class LoanMenu {

    private Scanner scanner;
    private LoanRepository loanRepository;
    private MemberRepository memberRepository;
    private BookRepository bookRepository;

    public LoanMenu(Scanner scanner, LoanRepository loanRepository, MemberRepository memberRepository, BookRepository bookRepository) {
        this.scanner = scanner;
        this.loanRepository = loanRepository;
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Loan ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Assign member");
            System.out.println("7. Remove member");
            System.out.println("8. Assign book");
            System.out.println("9. Remove book");
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
                case 8: assignBook(); break;
                case 9: removeBook(); break;
                case 0: return;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void create() {
        int loanDay = InputUtils.readInt(scanner, "loanDay: ");
        int dueDay = InputUtils.readInt(scanner, "dueDay: ");
        System.out.print("returned (true/false): ");
        boolean returned = Boolean.parseBoolean(scanner.nextLine());

        Loan entity = new Loan(loanDay, dueDay, returned);

        loanRepository.save(entity);
        System.out.println("Created: " + entity);
    }

    private void viewAll() {
        List<Loan> all = loanRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Loan entity : all) {
            System.out.println(entity);
            System.out.println("  member: " + entity.getMember());
            System.out.println("  book: " + entity.getBook());
        }
    }

    private void viewById() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Loan entity = loanRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
        System.out.println("  member: " + entity.getMember());
        System.out.println("  book: " + entity.getBook());
    }

    private void update() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        Loan entity = loanRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }

        System.out.println("Current: " + entity);
        System.out.println("(press Enter to keep current value)");

        System.out.print("loanDay [" + entity.getLoanDay() +"]: ");
        String loanDayStr = scanner.nextLine();
        if (!loanDayStr.isEmpty()) entity.setLoanDay(Integer.parseInt(loanDayStr));
        System.out.print("dueDay [" + entity.getDueDay() +"]: ");
        String dueDayStr = scanner.nextLine();
        if (!dueDayStr.isEmpty()) entity.setDueDay(Integer.parseInt(dueDayStr));
        System.out.print("returned [" + entity.getReturned() +"]: ");
        String returnedStr = scanner.nextLine();
        if (!returnedStr.isEmpty()) entity.setReturned(Boolean.parseBoolean(returnedStr));

        loanRepository.save(entity);
        System.out.println("Updated: " + entity);
    }

    private void delete() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        if (loanRepository.deleteById(id)) {
            System.out.println("Deleted.");
        } else {
            System.out.println("Not found.");
        }
    }

    private void assignMember() {
        Long ownerId = InputUtils.readLong(scanner, "Loan ID: ");
        Loan owner = loanRepository.findById(ownerId);
        if (owner == null) { System.out.println("Loan not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Member ID: ");
        Member other = memberRepository.findById(otherId);
        if (other == null) { System.out.println("Member not found."); return; }

        owner.setMember(other);
        loanRepository.save(owner);
        System.out.println("Assigned Member to Loan.");
    }

    private void removeMember() {
        Long id = InputUtils.readLong(scanner, "Loan ID: ");
        Loan entity = loanRepository.findById(id);
        if (entity == null) { System.out.println("Loan not found."); return; }

        entity.setMember(null);
        System.out.println("Member removed from " + entity);
    }

    private void assignBook() {
        Long ownerId = InputUtils.readLong(scanner, "Loan ID: ");
        Loan owner = loanRepository.findById(ownerId);
        if (owner == null) { System.out.println("Loan not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Book ID: ");
        Book other = bookRepository.findById(otherId);
        if (other == null) { System.out.println("Book not found."); return; }

        owner.setBook(other);
        loanRepository.save(owner);
        System.out.println("Assigned Book to Loan.");
    }

    private void removeBook() {
        Long id = InputUtils.readLong(scanner, "Loan ID: ");
        Loan entity = loanRepository.findById(id);
        if (entity == null) { System.out.println("Loan not found."); return; }

        entity.setBook(null);
        System.out.println("Book removed from " + entity);
    }

}
