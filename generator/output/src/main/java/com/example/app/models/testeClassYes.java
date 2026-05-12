package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "testeclassyes")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@DiscriminatorValue("testeClassYes")
public abstract class testeClassYes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "testar")
    private String testar;


    public testeClassYes() {}

    public String getTestar() { return testar; }
    public void setTestar(String testar) { this.testar = testar; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Override
    public String toString() {
        return "testeClassYes{id=" + getId() + ", testar=" + testar + "}";
    }
    // Operations
    public void testeOp(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
