package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "implementinterface")
public class ImplementInterface implements InterfaceTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes

    public ImplementInterface() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Override
    public String toString() {
        return "ImplementInterface{id=" + getId() + "}";
    }
    // Operations
    public void start(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
