package models;

import java.util.*;

public class Aluno {

    private Long id;

    private int numero;
    private String email;
    private String nome;
    private EstadoAluno estado;

    private Turno turno;
    public Aluno() {}

    public Aluno(int numero, String email, String nome, EstadoAluno estado) {
        this.numero = numero;
        this.email = email;
        this.nome = nome;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public EstadoAluno getEstado() {
        return estado;
    }

    public void setEstado(EstadoAluno estado) {
        this.estado = estado;
    }

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    @Override
    public String toString() {
        return "Aluno{" +
            "id=" + getId() +
            ", numero=" + numero +
            ", email='" + email + "'" +
            ", nome='" + nome + "'" +
            ", estado=" + estado +
            "}";
    }

    // Operations
    public List<String> validar(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
