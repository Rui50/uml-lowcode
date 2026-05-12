package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;

@Entity
public class testeSubAb extends testeClassYes {

    // attributes
    @Column(name = "nada")
    private String nada;


    public testeSubAb() {}

    public String getNada() { return nada; }
    public void setNada(String nada) { this.nada = nada; }

    @Override
    public String toString() {
        return "testeSubAb{id=" + getId() + ", nada=" + nada + "}";
    }
}
