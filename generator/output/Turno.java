import java.util.*;

public class Turno {

    // Attributes
    private Integer capacidade;
    private int numero;

    // Associations
    private List<Aluno> alunos;

    // Constructor
    public Turno() {
        this.alunos = new ArrayList<>();
    }

    // Getters and Setters
    public Integer getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(Integer capacidade) {
        this.capacidade = capacidade;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }


    public void addAluno(Aluno alunos) {
        this.alunos.add(alunos);
    }

    public void removeAluno(Aluno alunos) {
        this.alunos.remove(alunos);
    }

    public List<Aluno> getAlunos() {
        return this.alunos;
    }

    // Operations
    public Boolean temVaga(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
