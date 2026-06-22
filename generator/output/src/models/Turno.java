package models;

import java.util.*;

public class Turno {

    private Long id;

    private int capacidade;
    private int numero;

    private List<Aluno> alunos = new ArrayList<>();
    public Turno() {}

    public Turno(int capacidade, int numero) {
        this.capacidade = capacidade;
        this.numero = numero;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(int capacidade) {
        this.capacidade = capacidade;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public List<Aluno> getAlunos() {
        return alunos;
    }

    public void setAlunos(List<Aluno> alunos) {
        this.alunos = alunos;
    }

    @Override
    public String toString() {
        return "Turno{" +
            "id=" + getId() +
            ", capacidade=" + capacidade +
            ", numero=" + numero +
            "}";
    }

    // Operations
    public boolean temVaga(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
