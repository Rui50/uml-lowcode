package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;
import java.time.*;

@Entity
@Table(name = "librarycard")
public class LibraryCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "cardNumber")
    private String cardNumber;

    @Column(name = "issuedDay")
    private int issuedDay;

    // relationship: LibraryCard -> Member
    @OneToOne(mappedBy = "card")
    private Member member;


    public LibraryCard() {}

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public int getIssuedDay() { return issuedDay; }
    public void setIssuedDay(int issuedDay) { this.issuedDay = issuedDay; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    @Override
    public String toString() {
        return "LibraryCard{" + "id=" + getId() + ", cardNumber=" + getCardNumber() + ", issuedDay=" + getIssuedDay() + "}";
    }
}
