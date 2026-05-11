package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "aluno")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@DiscriminatorValue("Aluno")
public class Aluno{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoAluno estado;

    @Column(name = "email")
    private String email;

    @Column(name = "nome")
    private String nome;

    @Column(name = "numero")
    private int numero;

    // relationship: Aluno -> Turno
    @ManyToOne
    @JoinColumn(name = "turno_id")
    private Turno turno;


    public Aluno() {}

    public EstadoAluno getEstado() { return estado; }
    public void setEstado(EstadoAluno estado) { this.estado = estado; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Turno getTurno() { return turno; }
    public void setTurno(Turno turno) { this.turno = turno; }

    @Override
    public String toString() {
        return "Aluno{id=" + getId() + ", estado=" + estado + ", email=" + email + ", nome=" + nome + ", numero=" + numero + "}";
    }
}
