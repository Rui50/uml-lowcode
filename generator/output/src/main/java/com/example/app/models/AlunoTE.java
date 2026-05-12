package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;

@Entity
public class AlunoTE extends Aluno {

    // attributes
    @Column(name = "empresa")
    private String empresa;


    public AlunoTE() {}

    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }

    @Override
    public String toString() {
        return "AlunoTE{id=" + getId() + ", empresa=" + empresa + "}";
    }
    // Operations
    public Boolean comprovativo(
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
