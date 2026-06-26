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
@Table(name = "encomenda")
public class Encomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "data")
    private String data;

    @Column(name = "estado")
    private String estado;

    // relationship: Encomenda -> LinhaEncomenda
    @OneToMany
    @JoinColumn(name = "encomenda_id")
    @Builder.Default
    private List<LinhaEncomenda> linhas = new ArrayList<>();

}
