package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;
import java.time.*;

@Entity
@Table(name = "loan")
public class Loan implements Payable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "loanDay")
    private int loanDay;

    @Column(name = "dueDay")
    private int dueDay;

    @Column(name = "returned")
    private boolean returned;

    // relationship: Loan -> Member
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    // relationship: Loan -> Book
    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;


    public Loan() {}

    public int getLoanDay() { return loanDay; }
    public void setLoanDay(int loanDay) { this.loanDay = loanDay; }

    public int getDueDay() { return dueDay; }
    public void setDueDay(int dueDay) { this.dueDay = dueDay; }

    public boolean getReturned() { return returned; }
    public void setReturned(boolean returned) { this.returned = returned; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    @Override
    public String toString() {
        return "Loan{" + "id=" + getId() + ", loanDay=" + getLoanDay() + ", dueDay=" + getDueDay() + ", returned=" + getReturned() + "}";
    }
    // Operations
    public void returnLoan(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public void pay(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
