import java.util.*;

public class Turno {

    // Attributes
    private int capacidade;

    // Associations
    private List<Aluno> alunos;

    // Constructor
    public Turno() {
        this.alunos = new ArrayList<>();
    }

    // Getters and Setters
    public int getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(int capacidade) {
        this.capacidade = capacidade;
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
    public boolean temVaga(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
