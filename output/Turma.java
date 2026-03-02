import java.util.*;

public class Turma {

    // Attributes
    private String nome;

    // Associations
   private Map<int, Aluno> alunoss;
    private List<Turno> turnos;

    // Constructor
    public Turma() {
        this.alunoss = new HashMap<>();
        this.turnos = new ArrayList<>();
    }

    // Getters and Setters
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }


    public void putAluno(int key, Aluno value) {
        this.alunoss.put(key, value);
    }

    public Aluno getAluno(int key) {
        return this.alunoss.get(key);
    }

    public void removeAluno(int key) {
        this.alunoss.remove(key);
    }

    public Map<int, Aluno> getAllAlunos() {
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
