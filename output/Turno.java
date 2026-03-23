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

    // Validation (para ja so invariantes OCL simples

    /**
     * Validate entity with OCL
     * 
     * - CapacidadePositiva: self.capacidade > 0
     * 
     * @return list of validation errors (empty if valid)
     */
    public List<String> validate() {
       List<String> errors = new ArrayList<>();

        // Invariant: CapacidadePositiva
        if (!(this.capacidade > 0)) {
            errors.add("Invariant 'CapacidadePositiva' violated.");
        }

        return errors;
    }

}
