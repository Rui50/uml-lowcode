package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;
import java.time.*;

@Entity
@Table(name = "library")
public class Library {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "name")
    private String name;

    // relationship: Library -> Member
    @OneToMany(mappedBy = "library")
    private List<Member> members = new ArrayList<>();

    // relationship: Library -> Book
    @OneToMany(mappedBy = "library")
    @MapKey(name = "isbn")
    private Map<String, Book> catalog = new HashMap<>();


    public Library() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<Member> getMembers() { return members; }
    public void setMembers(List<Member> members) { this.members = members; }

    public Map<String, Book> getCatalog() { return catalog; }
    public void setCatalog(Map<String, Book> catalog) { this.catalog = catalog; }

    @Override
    public String toString() {
        return "Library{" + "id=" + getId() + ", name=" + getName() + "}";
    }
    // Operations
    public void markOverdue(
        int currentDay
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
