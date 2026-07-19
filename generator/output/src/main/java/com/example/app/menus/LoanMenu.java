package com.example.app.menus;

import com.example.app.models.*;
import com.example.app.repository.*;
import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.*;

public class LoanMenu {

    private Scanner scanner;
    private EntityManager em;
    private LoanRepository loanRepository;
    private MemberRepository memberRepository;
    private BookRepository bookRepository;

    public LoanMenu(Scanner scanner, EntityManager em) {
        this.scanner = scanner;
        this.em = em;
        this.loanRepository = new LoanRepository(em);
        this.memberRepository = new MemberRepository(em);
        this.bookRepository = new BookRepository(em);
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

        Loan entity = new Loan();
        entity.setLoanDay(loanDay);
        entity.setDueDay(dueDay);
        entity.setReturned(returned);

        em.getTransaction().begin();
        try {
            loanRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Created: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
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

        em.getTransaction().begin();
        try {
            loanRepository.save(entity);
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
        Loan entity = loanRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        em.getTransaction().begin();
        try {
            loanRepository.deleteById(id);
            em.getTransaction().commit();
            System.out.println("Deleted.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
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
        other.getLoans().add(owner);
        em.getTransaction().begin();
        try {
            loanRepository.save(owner);
            em.getTransaction().commit();
            System.out.println("Assigned Member to Loan.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void removeMember() {
        Long id = InputUtils.readLong(scanner, "Loan ID: ");
        Loan entity = loanRepository.findById(id);
        if (entity == null) { System.out.println("Loan not found."); return; }

        Member previous = entity.getMember();
        entity.setMember(null);
        if (previous != null) {
            previous.getLoans().remove(entity);
        }
        em.getTransaction().begin();
        try {
            loanRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Member removed from " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void assignBook() {
        Long ownerId = InputUtils.readLong(scanner, "Loan ID: ");
        Loan owner = loanRepository.findById(ownerId);
        if (owner == null) { System.out.println("Loan not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Book ID: ");
        Book other = bookRepository.findById(otherId);
        if (other == null) { System.out.println("Book not found."); return; }

        owner.setBook(other);
        other.getLoans().add(owner);
        em.getTransaction().begin();
        try {
            loanRepository.save(owner);
            em.getTransaction().commit();
            System.out.println("Assigned Book to Loan.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void removeBook() {
        Long id = InputUtils.readLong(scanner, "Loan ID: ");
        Loan entity = loanRepository.findById(id);
        if (entity == null) { System.out.println("Loan not found."); return; }

        Book previous = entity.getBook();
        entity.setBook(null);
        if (previous != null) {
            previous.getLoans().remove(entity);
        }
        em.getTransaction().begin();
        try {
            loanRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Book removed from " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

}
