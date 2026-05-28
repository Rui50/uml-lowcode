package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "aluno")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@DiscriminatorValue("Aluno")
public class Aluno {

    // attributes
    @Column(name = "nome")
    private String nome;

    @Id
    @Column(name = "email")
    private String email;

    @Column(name = "numero")
    private int numero;

    // relationship: Aluno -> Turno
    @ManyToOne
    @JoinColumn(name = "turno_id")
    private Turno turno;


    public Aluno() {}

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public Turno getTurno() { return turno; }
    public void setTurno(Turno turno) { this.turno = turno; }

    @Override
    public String toString() {
        return "Aluno{" + "nome=" + nome + ", email=" + email + ", numero=" + numero + "}";
    }
    // Operations
    public List<String> validar(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
