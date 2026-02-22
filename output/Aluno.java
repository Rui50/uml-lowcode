import java.util.*;

public class Aluno {

    // Attributes
    private int numero;
    private String nome;
    private string email;

    // Associations
    private Turma turma;

    // Constructor
    public Aluno() {
    }

    // Getters and Setters

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public string getEmail() {
        return email;
    }

    public void setEmail(string email) {
        this.email = email;
    }


    public Turma getTurma() {
        return this.turma;
    }

    public void setTurma(Turma turma) {
        this.turma = turma;
    }

    // Operations
    public List<String> validar(
    ) {
        // TODO: implement this method
        return null;
    }

}
