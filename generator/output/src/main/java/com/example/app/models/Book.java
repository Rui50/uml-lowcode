package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;
import java.time.*;

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

    // relationship: Book -> Library
    @ManyToOne
    @JoinColumn(name = "library_id")
    private Library library;

    // relationship: Book -> Loan
    @OneToMany(mappedBy = "book")
    private List<Loan> loans = new ArrayList<>();

    // relationship: Book -> Author
    @ManyToMany
    @JoinTable(
        name = "book_authors",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private List<Author> authors = new ArrayList<>();


    public Book() {}

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public BookStatus getStatus() { return status; }
    public void setStatus(BookStatus status) { this.status = status; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Map<String, Library> getLibrary() { return library; }
    public void setLibrary(Map<String, Library> library) { this.library = library; }

    public List<Loan> getLoans() { return loans; }
    public void setLoans(List<Loan> loans) { this.loans = loans; }

    public List<Author> getAuthors() { return authors; }
    public void setAuthors(List<Author> authors) { this.authors = authors; }

    @Override
    public String toString() {
        return "Book{" + "id=" + getId() + ", isbn=" + getIsbn() + ", title=" + getTitle() + ", status=" + getStatus() + "}";
    }
}
