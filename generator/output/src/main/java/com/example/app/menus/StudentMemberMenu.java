package com.example.app.menus;

import com.example.app.models.*;
import com.example.app.repository.*;
import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.*;

public class StudentMemberMenu {

    private Scanner scanner;
    private EntityManager em;
    private StudentMemberRepository studentMemberRepository;
    private LibraryRepository libraryRepository;
    private LoanRepository loanRepository;
    private LibraryCardRepository libraryCardRepository;

    public StudentMemberMenu(Scanner scanner, EntityManager em) {
        this.scanner = scanner;
        this.em = em;
        this.studentMemberRepository = new StudentMemberRepository(em);
        this.libraryRepository = new LibraryRepository(em);
        this.loanRepository = new LoanRepository(em);
        this.libraryCardRepository = new LibraryCardRepository(em);
    }

    public void run() {
        while (true) {
            System.out.println("\n=== StudentMember ===");
            System.out.println("1. Create");
            System.out.println("2. View All");
            System.out.println("3. View by ID");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Assign library");
            System.out.println("7. Remove library");
            System.out.println("8. Add loans");
            System.out.println("9. Remove loans");
            System.out.println("10. Assign main_loan");
            System.out.println("11. Remove main_loan");
            System.out.println("12. Assign secondary_loan");
            System.out.println("13. Remove secondary_loan");
            System.out.println("14. Assign card");
            System.out.println("15. Remove card");
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
                case 10: assignMain_loan(); break;
                case 11: removeMain_loan(); break;
                case 12: assignSecondary_loan(); break;
                case 13: removeSecondary_loan(); break;
                case 14: assignCard(); break;
                case 15: removeCard(); break;
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
        System.out.print("course: ");
        String course = scanner.nextLine();
        int maxLoans = InputUtils.readInt(scanner, "maxLoans: ");

        StudentMember entity = new StudentMember();
        entity.setMemberNumber(memberNumber);
        entity.setName(name);
        entity.setEmail(email);
        entity.setPoints(points);
        entity.setActiveLoans(activeLoans);
        entity.setCourse(course);
        entity.setMaxLoans(maxLoans);

        em.getTransaction().begin();
        try {
            studentMemberRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Created: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void viewAll() {
        List<StudentMember> all = studentMemberRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (StudentMember entity : all) {
            System.out.println(entity);
            System.out.println("  library: " + entity.getLibrary());
            System.out.println("  loans: " + entity.getLoans());
            System.out.println("  main_loan: " + entity.getMain_loan());
            System.out.println("  secondary_loan: " + entity.getSecondary_loan());
            System.out.println("  card: " + entity.getCard());
        }
    }

    private void viewById() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        StudentMember entity = studentMemberRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        System.out.println(entity);
        System.out.println("  library: " + entity.getLibrary());
        System.out.println("  loans: " + entity.getLoans());
        System.out.println("  main_loan: " + entity.getMain_loan());
        System.out.println("  secondary_loan: " + entity.getSecondary_loan());
        System.out.println("  card: " + entity.getCard());
    }

    private void update() {
        Long id = InputUtils.readLong(scanner, "ID: ");
        StudentMember entity = studentMemberRepository.findById(id);
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
        System.out.print("course [" + entity.getCourse() +"]: ");
        String course = scanner.nextLine();
        if (!course.isEmpty()) entity.setCourse(course);
        System.out.print("maxLoans [" + entity.getMaxLoans() +"]: ");
        String maxLoansStr = scanner.nextLine();
        if (!maxLoansStr.isEmpty()) entity.setMaxLoans(Integer.parseInt(maxLoansStr));

        em.getTransaction().begin();
        try {
            studentMemberRepository.save(entity);
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
        StudentMember entity = studentMemberRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        em.getTransaction().begin();
        try {
            studentMemberRepository.deleteById(id);
            em.getTransaction().commit();
            System.out.println("Deleted.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void assignLibrary() {
        Long ownerId = InputUtils.readLong(scanner, "StudentMember ID: ");
        StudentMember owner = studentMemberRepository.findById(ownerId);
        if (owner == null) { System.out.println("StudentMember not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Library ID: ");
        Library other = libraryRepository.findById(otherId);
        if (other == null) { System.out.println("Library not found."); return; }

        owner.setLibrary(other);
        other.getMembers().add(owner);
        em.getTransaction().begin();
        try {
            studentMemberRepository.save(owner);
            em.getTransaction().commit();
            System.out.println("Assigned Library to StudentMember.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void removeLibrary() {
        Long id = InputUtils.readLong(scanner, "StudentMember ID: ");
        StudentMember entity = studentMemberRepository.findById(id);
        if (entity == null) { System.out.println("StudentMember not found."); return; }

        Library previous = entity.getLibrary();
        entity.setLibrary(null);
        if (previous != null) {
            previous.getMembers().remove(entity);
        }
        em.getTransaction().begin();
        try {
            studentMemberRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Library removed from " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void addLoans() {
        Long ownerId = InputUtils.readLong(scanner, "StudentMember ID: ");
        StudentMember owner = studentMemberRepository.findById(ownerId);
        if (owner == null) { System.out.println("StudentMember not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Loan ID: ");
        Loan other = loanRepository.findById(otherId);
        if (other == null) { System.out.println("Loan not found."); return; }

        other.setMember(owner);   // lado DONO: é o que persiste
        owner.getLoans().add(other);
        em.getTransaction().begin();
        try {
            loanRepository.save(other);
            em.getTransaction().commit();
            System.out.println("Added Loan to StudentMember.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void removeLoans() {
        Long ownerId = InputUtils.readLong(scanner, "StudentMember ID: ");
        StudentMember owner = studentMemberRepository.findById(ownerId);
        if (owner == null) { System.out.println("StudentMember not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Loan ID: ");
        Loan other = loanRepository.findById(otherId);
        if (other == null) { System.out.println("Loan not found."); return; }

        other.setMember(null);   // lado DONO: é o que persiste
        owner.getLoans().remove(other);
        em.getTransaction().begin();
        try {
            loanRepository.save(other);
            em.getTransaction().commit();
            System.out.println("Loan removed from StudentMember.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }
    private void assignMain_loan() {
        Long ownerId = InputUtils.readLong(scanner, "StudentMember ID: ");
        StudentMember owner = studentMemberRepository.findById(ownerId);
        if (owner == null) { System.out.println("StudentMember not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Loan ID: ");
        Loan other = loanRepository.findById(otherId);
        if (other == null) { System.out.println("Loan not found."); return; }

        owner.setMain_loan(other);
        em.getTransaction().begin();
        try {
            studentMemberRepository.save(owner);
            em.getTransaction().commit();
            em.refresh(other);
            System.out.println("Assigned Loan to StudentMember.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void removeMain_loan() {
        Long id = InputUtils.readLong(scanner, "StudentMember ID: ");
        StudentMember entity = studentMemberRepository.findById(id);
        if (entity == null) { System.out.println("StudentMember not found."); return; }

        Loan previous = entity.getMain_loan();
        entity.setMain_loan(null);
        em.getTransaction().begin();
        try {
            studentMemberRepository.save(entity);
            em.getTransaction().commit();
            if (previous != null) em.refresh(previous);
            System.out.println("Loan removed from " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void assignSecondary_loan() {
        Long ownerId = InputUtils.readLong(scanner, "StudentMember ID: ");
        StudentMember owner = studentMemberRepository.findById(ownerId);
        if (owner == null) { System.out.println("StudentMember not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "Loan ID: ");
        Loan other = loanRepository.findById(otherId);
        if (other == null) { System.out.println("Loan not found."); return; }

        owner.setSecondary_loan(other);
        em.getTransaction().begin();
        try {
            studentMemberRepository.save(owner);
            em.getTransaction().commit();
            em.refresh(other);
            System.out.println("Assigned Loan to StudentMember.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void removeSecondary_loan() {
        Long id = InputUtils.readLong(scanner, "StudentMember ID: ");
        StudentMember entity = studentMemberRepository.findById(id);
        if (entity == null) { System.out.println("StudentMember not found."); return; }

        Loan previous = entity.getSecondary_loan();
        entity.setSecondary_loan(null);
        em.getTransaction().begin();
        try {
            studentMemberRepository.save(entity);
            em.getTransaction().commit();
            if (previous != null) em.refresh(previous);
            System.out.println("Loan removed from " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void assignCard() {
        Long ownerId = InputUtils.readLong(scanner, "StudentMember ID: ");
        StudentMember owner = studentMemberRepository.findById(ownerId);
        if (owner == null) { System.out.println("StudentMember not found."); return; }

        Long otherId = InputUtils.readLong(scanner, "LibraryCard ID: ");
        LibraryCard other = libraryCardRepository.findById(otherId);
        if (other == null) { System.out.println("LibraryCard not found."); return; }

        owner.setCard(other);
        other.setMember(owner);
        em.getTransaction().begin();
        try {
            studentMemberRepository.save(owner);
            em.getTransaction().commit();
            System.out.println("Assigned LibraryCard to StudentMember.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void removeCard() {
        Long id = InputUtils.readLong(scanner, "StudentMember ID: ");
        StudentMember entity = studentMemberRepository.findById(id);
        if (entity == null) { System.out.println("StudentMember not found."); return; }

        LibraryCard previous = entity.getCard();
        entity.setCard(null);
        if (previous != null) {
            previous.setMember(null);
        }
        em.getTransaction().begin();
        try {
            studentMemberRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("LibraryCard removed from " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

}
