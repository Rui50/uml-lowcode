import java.util.*;

public class Turma {

    // Attributes
    private String nome;

    // Associations
   private Map<Integer, Aluno> alunos;
    private List<Turno> turnos;

    // Constructor
    public Turma() {
        this.alunos = new HashMap<>();
        this.turnos = new ArrayList<>();
    }

    // Getters and Setters
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }


    public void putAluno(Integer key, Aluno value) {
        this.alunos.put(key, value);
    }

    public Aluno getAluno(Integer key) {
        return this.alunos.get(key);
    }

    public void removeAluno(Integer key) {
        this.alunos.remove(key);
    }

    public Map<Integer, Aluno> getAllAlunos() {
        return this.alunos;
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

}
