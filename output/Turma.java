import java.util.*;

public class Turma {

    // Attributes
    private String nome;

    // Associations
    private List<Aluno> alunos;

    // Constructor
    public Turma() {
        this.alunos = new ArrayList<>();
    }

    // Getters and Setters

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
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

}
