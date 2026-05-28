package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "turma")
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "nome")
    private String nome;

    // relationship: Turma -> Aluno
    @OneToMany
    @JoinColumn(name = "turma_id")
    @MapKey(name = "numero")
    private Map<Integer, Aluno> alunos = new HashMap<>();

    // relationship: Turma -> Turno
    @OneToMany
    @JoinColumn(name = "turma_id")
    private List<Turno> turnos = new ArrayList<>();


    public Turma() {}

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Map<Integer, Aluno> getAlunos() { return alunos; }
    public void setAlunos(Map<Integer, Aluno> alunos) { this.alunos = alunos; }

    public List<Turno> getTurnos() { return turnos; }
    public void setTurnos(List<Turno> turnos) { this.turnos = turnos; }

    @Override
    public String toString() {
        return "Turma{" + "id=" + getId() + ", nome=" + nome + "}";
    }
    // Operations
    public boolean transferirAluno(
        Turma destino,
        int numero
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public int totalAlunos(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public void inicializarAlunosParaTeste(
        List<Aluno> alunos
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
