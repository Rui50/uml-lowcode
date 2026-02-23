import java.util.*;

public class Turma {

    // Attributes
    private String nome;

    // Associations
    private List<Aluno> alunoss;
    private List<Turno> turnos;

    // Constructor
    public Turma() {
        this.alunoss = new ArrayList<>();
        this.turnos = new ArrayList<>();
    }

    // Getters and Setters
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }


    public void addAluno(Aluno alunoss) {
        this.alunoss.add(alunoss);
    }

    public void removeAluno(Aluno alunoss) {
        this.alunoss.remove(alunoss);
    }

    public List<Aluno> getAlunos() {
        return this.alunoss;
    }

    public void addTurno(Turno turnos) {
        this.turnos.add(turnos);
    }

    public void removeTurno(Turno turnos) {
        this.turnos.remove(turnos);
    }

    public List<Turno> getTurnos() {
        return this.turnos;
    }

    // Operations
    public void inicializarAlunosParaTeste(
        List<Aluno> alunos
    ) {
        // TODO: implement this method
    }
    public boolean transferirAluno(
        Turma destino,
        int numero
    ) {
        // TODO: implement this method
        return false;
    }
    public Int totalAlunos(
    ) {
        // TODO: implement this method
        return 0;
    }

}
