import java.util.*;

public class Aluno {

    // Attributes
    private String email;
    private int numero;
    private String nome;

    // Associations
    private Turno turno;

    // Constructor
    public Aluno() {
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

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


    public Turno getTurno() {
        return this.turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    // Operations
    public List<String> validar(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
