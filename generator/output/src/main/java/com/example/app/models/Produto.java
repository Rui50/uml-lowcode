package com.example.app.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "nome")
    private String nome;

    @Column(name = "preco")
    private double preco;

    @Column(name = "stock")
    private int stock;

    // Validation (para ja so invariantes OCL simples

    /**
     * Validate entity with OCL
     * 
     * - LetTest: let dobro = self.preco * 2 in dobro >= self.stock
     * 
     * @return list of validation errors (empty if valid)
     */
    public List<String> validate() {
       List<String> errors = new ArrayList<>();

        // Invariant: LetTest
        if (!((this.preco * 2) >= this.stock)) {
            errors.add("Invariant 'LetTest' violated.");
        }

        return errors;
    }

}
