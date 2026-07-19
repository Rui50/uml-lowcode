package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;
import java.time.*;

@Entity
@Table(name = "author")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "name")
    private String name;

    // relationship: Author -> Book
    @ManyToMany(mappedBy = "authors")
    private List<Book> books = new ArrayList<>();


    public Author() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<Book> getBooks() { return books; }
    public void setBooks(List<Book> books) { this.books = books; }

    @Override
    public String toString() {
        return "Author{" + "id=" + getId() + ", name=" + getName() + "}";
    }
}
