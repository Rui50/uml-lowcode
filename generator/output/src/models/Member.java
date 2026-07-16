package models;

import java.util.*;
import java.time.*;

public abstract class Member {

    private Long id;

    private int memberNumber;
    private String name;
    private String email;
    private int points;
    private int activeLoans;

    private Library library;
    private List<Loan> loans = new ArrayList<>();
    private LibraryCard card;
    public Member() {
    }

    public Member(int memberNumber, String name, String email, int points, int activeLoans) {
        this.memberNumber = memberNumber;
        this.name = name;
        this.email = email;
        this.points = points;
        this.activeLoans = activeLoans;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getMemberNumber() {
        return memberNumber;
    }

    public void setMemberNumber(int memberNumber) {
        this.memberNumber = memberNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getActiveLoans() {
        return activeLoans;
    }

    public void setActiveLoans(int activeLoans) {
        this.activeLoans = activeLoans;
    }

    public Library getLibrary() {
        return library;
    }

    public void setLibrary(Library library) {
        this.library = library;
    }

    public List<Loan> getLoans() {
        return loans;
    }

    public void setLoans(List<Loan> loans) {
        this.loans = loans;
    }

    public LibraryCard getCard() {
        return card;
    }

    public void setCard(LibraryCard card) {
        this.card = card;
    }

    @Override
    public String toString() {
        return "Member{" +
            "id=" + getId() +
            ", memberNumber=" + getMemberNumber() +
            ", name='" + getName() + "'" +
            ", email='" + getEmail() + "'" +
            ", points=" + getPoints() +
            ", activeLoans=" + getActiveLoans() +
            "}";
    }

    // Operations

    public void redeemPoints(int amount) {
        points = points - amount;
    }
    public Loan borrowBook(
        Book book,
        int today
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
