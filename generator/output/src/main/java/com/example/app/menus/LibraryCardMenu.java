package com.example.app.menus;

import com.example.app.models.*;
import com.example.app.repository.*;
import com.example.app.util.InputUtils;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.*;

public class LibraryCardMenu {

    private Scanner scanner;
    private EntityManager em;
    private LibraryCardRepository libraryCardRepository;
    private MemberRepository memberRepository;

    public LibraryCardMenu(Scanner scanner, EntityManager em) {
        this.scanner = scanner;
        this.em = em;
        this.libraryCardRepository = new LibraryCardRepository(em);
        this.memberRepository = new MemberRepository(em);
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

        LibraryCard entity = new LibraryCard();
        entity.setCardNumber(cardNumber);
        entity.setIssuedDay(issuedDay);

        em.getTransaction().begin();
        try {
            libraryCardRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Created: " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
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

        em.getTransaction().begin();
        try {
            libraryCardRepository.save(entity);
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
        LibraryCard entity = libraryCardRepository.findById(id);
        if (entity == null) {
            System.out.println("Not found.");
            return;
        }
        em.getTransaction().begin();
        try {
            libraryCardRepository.deleteById(id);
            em.getTransaction().commit();
            System.out.println("Deleted.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
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
        other.setCard(owner);
        em.getTransaction().begin();
        try {
            libraryCardRepository.save(owner);
            em.getTransaction().commit();
            System.out.println("Assigned Member to LibraryCard.");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void removeMember() {
        Long id = InputUtils.readLong(scanner, "LibraryCard ID: ");
        LibraryCard entity = libraryCardRepository.findById(id);
        if (entity == null) { System.out.println("LibraryCard not found."); return; }

        Member previous = entity.getMember();
        entity.setMember(null);
        if (previous != null) {
            previous.setCard(null);
        }
        em.getTransaction().begin();
        try {
            libraryCardRepository.save(entity);
            em.getTransaction().commit();
            System.out.println("Member removed from " + entity);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.clear();
            System.out.println("Error: " + ex.getMessage());
        }
    }

}
