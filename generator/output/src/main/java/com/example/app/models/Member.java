package com.example.app.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
@Entity
@Table(name = "member")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
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
    @JsonIgnoreProperties("members")
    @ManyToOne
    @JoinColumn(name = "library_id")
    private Library library;

    // relationship: Member -> Loan
    @JsonIgnoreProperties("member")
    @OneToMany(mappedBy = "member")
    @Builder.Default
    private List<Loan> loans = new ArrayList<>();

    // relationship: Member -> Book
    @JsonIgnoreProperties("reservedBy")
    @ManyToMany
    @JoinTable(
        name = "member_reservedbook",
        joinColumns = @JoinColumn(name = "member_id"),
        inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    @Builder.Default
    private List<Book> reservedBook = new ArrayList<>();

    // relationship: Member -> Book
    @JsonIgnoreProperties("borrower")
    @OneToMany(mappedBy = "borrower")
    @Builder.Default
    private List<Book> borrowedBooks = new ArrayList<>();

    // relationship: Member -> LibraryCard
    @JsonIgnoreProperties("member")
    @OneToOne
    @JoinColumn(name = "librarycard_id")
    private LibraryCard card;

}
