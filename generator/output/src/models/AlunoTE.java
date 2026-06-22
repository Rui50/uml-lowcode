package models;

import java.util.*;

public class AlunoTE extends Aluno {

    private String empresa;

    public AlunoTE() {}

    public AlunoTE(int numero, String email, String nome, EstadoAluno estado, String empresa) {
        super(numero, email, nome, estado);
        this.empresa = empresa;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    @Override
    public String toString() {
        return "AlunoTE{" +
            "id=" + getId() +
            ", empresa='" + empresa + "'" +
            "}";
    }

    // Operations
    public boolean comprovativo(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
