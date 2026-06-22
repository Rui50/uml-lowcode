package models;

import java.util.*;

public class Turma {

    private Long id;

    private String nome;

    private Map<Integer, Aluno> alunos = new HashMap<>();
    private List<Turno> turnos = new ArrayList<>();
    public Turma() {}

    public Turma(String nome) {
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Map<Integer, Aluno> getAlunos() {
        return alunos;
    }

    public void setAlunos(Map<Integer, Aluno> alunos) {
        this.alunos = alunos;
    }

    public List<Turno> getTurnos() {
        return turnos;
    }

    public void setTurnos(List<Turno> turnos) {
        this.turnos = turnos;
    }

    @Override
    public String toString() {
        return "Turma{" +
            "id=" + getId() +
            ", nome='" + nome + "'" +
            "}";
    }

    // Operations
    public int totalAlunos(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public boolean transferirAluno(
        int numero,
        Turma destino
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public void inicializarAlunosParaTeste(
        List<Aluno> alunos
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public void fecharTurma(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
