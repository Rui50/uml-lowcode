package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;
import java.time.*;

@Entity
@Table(name = "member")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
@DiscriminatorValue("Member")
public abstract class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "memberNumber")
    private int memberNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "points")
    private int points;

    @Column(name = "activeLoans")
    private int activeLoans;

    // relationship: Member -> Library
    @ManyToOne
    @JoinColumn(name = "library_id")
    private Library library;

    // relationship: Member -> Loan
    @OneToMany(mappedBy = "member")
    private List<Loan> loans = new ArrayList<>();

    // relationship: Member -> Loan
    @OneToOne
    @JoinColumn(name = "main_loan_id")
    private Loan main_loan;

    // relationship: Member -> Loan
    @OneToOne
    @JoinColumn(name = "secondary_loan_id")
    private Loan secondary_loan;

    // relationship: Member -> LibraryCard
    @OneToOne
    @JoinColumn(name = "card_id")
    private LibraryCard card;


    public Member() {}

    public int getMemberNumber() { return memberNumber; }
    public void setMemberNumber(int memberNumber) { this.memberNumber = memberNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public int getActiveLoans() { return activeLoans; }
    public void setActiveLoans(int activeLoans) { this.activeLoans = activeLoans; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public List<Loan> getLoans() { return loans; }
    public void setLoans(List<Loan> loans) { this.loans = loans; }

    public Loan getMain_loan() { return main_loan; }
    public void setMain_loan(Loan main_loan) { this.main_loan = main_loan; }

    public Loan getSecondary_loan() { return secondary_loan; }
    public void setSecondary_loan(Loan secondary_loan) { this.secondary_loan = secondary_loan; }

    public LibraryCard getCard() { return card; }
    public void setCard(LibraryCard card) { this.card = card; }

    @Override
    public String toString() {
        return "Member{" + "id=" + getId() + ", memberNumber=" + getMemberNumber() + ", name=" + getName() + ", email=" + getEmail() + ", points=" + getPoints() + ", activeLoans=" + getActiveLoans() + "}";
    }
    // Operations
    public void redeemPoints(
        int memberId,
        int ammount
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public Loan borrowBook(
        Book book,
        int today
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
