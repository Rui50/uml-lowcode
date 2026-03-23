import java.util.*;

public class Turma {

    // Attributes
    private String nome;

    // Associations
   private Map<Integer, Aluno> alunoss;
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


    public void putAluno(Integer key, Aluno value) {
        this.alunoss.put(key, value);
    }

    public Aluno getAluno(Integer key) {
        return this.alunoss.get(key);
    }

    public void removeAluno(Integer key) {
        this.alunoss.remove(key);
    }

    public Map<Integer, Aluno> getAllAlunos() {
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
    public int totalAlunos(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public void inicializarAlunosParaTeste(
        List<Aluno> alunos
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public boolean transferirAluno(
        Turma destino,
        int numero
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // Validation (para ja so invariantes OCL simples

    /**
     * Validate entity with OCL
     * 
     * - MaxAlunos: self.alunos -> size() <= 30
     * 
     * @return list of validation errors (empty if valid)
     */
    public List<String> validate() {
       List<String> errors = new ArrayList<>();

        // Invariant: MaxAlunos
        if (!(this.alunos.size() <= 30)) {
            errors.add("Invariant 'MaxAlunos' violated.");
        }

        return errors;
    }

}
