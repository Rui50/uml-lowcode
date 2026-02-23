import java.util.*;

public class Aluno {

    // Attributes
    private String nome;
    private string email;
    private int numero;

    // Associations
    private Turno turno;

    // Constructor
    public Aluno() {
    }

    // Getters and Setters
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

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
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
        // TODO: implement this method
        return null;
    }

}
