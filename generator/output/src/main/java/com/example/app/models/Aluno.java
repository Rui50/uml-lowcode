package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "aluno")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@DiscriminatorValue("Aluno")
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "email")
    private String email;

    @Column(name = "nome")
    private String nome;

    @Column(name = "numero")
    private int numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoAluno estado;

    // relationship: Aluno -> Turno
    @ManyToOne
    @JoinColumn(name = "turno_id")
    private Turno turno;


    public Aluno() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public EstadoAluno getEstado() { return estado; }
    public void setEstado(EstadoAluno estado) { this.estado = estado; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Turno getTurno() { return turno; }
    public void setTurno(Turno turno) { this.turno = turno; }

    @Override
    public String toString() {
        return "Aluno{id=" + getId() + ", email=" + email + ", nome=" + nome + ", numero=" + numero + ", estado=" + estado + "}";
    }
    // Operations
    public List<String> validar(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
