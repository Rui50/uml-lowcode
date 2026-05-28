package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "turno")
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "capacidade")
    private int capacidade;

    // relationship: Turno -> Aluno
    @OneToMany(mappedBy = "turno")
    private List<Aluno> alunos = new ArrayList<>();


    public Turno() {}

    public int getCapacidade() { return capacidade; }
    public void setCapacidade(int capacidade) { this.capacidade = capacidade; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<Aluno> getAlunos() { return alunos; }
    public void setAlunos(List<Aluno> alunos) { this.alunos = alunos; }

    @Override
    public String toString() {
        return "Turno{" + "id=" + getId() + ", capacidade=" + capacidade + "}";
    }
    // Operations
    public boolean temVaga(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
