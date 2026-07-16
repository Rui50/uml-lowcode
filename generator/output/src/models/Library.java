package models;

import java.util.*;
import java.time.*;

public class Library {

    private Long id;

    private String name;

    private List<Member> members = new ArrayList<>();
    private Map<String, Book> catalog = new HashMap<>();
    public Library() {
    }

    public Library(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(List<Member> members) {
        this.members = members;
    }

    public Map<String, Book> getCatalog() {
        return catalog;
    }

    public void setCatalog(Map<String, Book> catalog) {
        this.catalog = catalog;
    }

    @Override
    public String toString() {
        return "Library{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            "}";
    }

    // Operations
    public void markOverdue(
        int currentDay
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
