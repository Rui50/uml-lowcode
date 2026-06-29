package com.example.app.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "isbn")
    private String isbn;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BookStatus status;

    // relationship: Book -> Loan
    @JsonIgnoreProperties("book")
    @OneToMany(mappedBy = "book")
    @Builder.Default
    private List<Loan> loans = new ArrayList<>();

    // relationship: Book -> Author
    @ManyToMany
    @JoinTable(
        name = "book_authors",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @Builder.Default
    private List<Author> authors = new ArrayList<>();

    // relationship: Book -> Member
    @JsonIgnoreProperties("reservedBook")
    @ManyToMany(mappedBy = "reservedBook")
    @Builder.Default
    private List<Member> reservedBy = new ArrayList<>();

    // relationship: Book -> Member
    @JsonIgnoreProperties("borrowedBooks")
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member borrower;

    // relationship: Book -> Author
    @ManyToMany
    @JoinTable(
        name = "book_editors",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @Builder.Default
    private List<Author> editors = new ArrayList<>();

}
