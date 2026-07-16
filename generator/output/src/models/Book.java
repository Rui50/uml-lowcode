package models;

import java.util.*;
import java.time.*;

public class Book {

    private Long id;

    private String isbn;
    private String title;
    private BookStatus status;

    private List<Loan> loans = new ArrayList<>();
    private List<Author> authors = new ArrayList<>();
    public Book() {
    }

    public Book(String isbn, String title, BookStatus status) {
        this.isbn = isbn;
        this.title = title;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BookStatus getStatus() {
        return status;
    }

    public void setStatus(BookStatus status) {
        this.status = status;
    }

    public List<Loan> getLoans() {
        return loans;
    }

    public void setLoans(List<Loan> loans) {
        this.loans = loans;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(List<Author> authors) {
        this.authors = authors;
    }

    @Override
    public String toString() {
        return "Book{" +
            "id=" + getId() +
            ", isbn='" + getIsbn() + "'" +
            ", title='" + getTitle() + "'" +
            ", status=" + getStatus() +
            "}";
    }

}
