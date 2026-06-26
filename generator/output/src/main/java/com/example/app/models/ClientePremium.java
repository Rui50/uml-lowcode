package com.example.app.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
@Entity
public class ClientePremium extends Cliente {

    // attributes
    @Column(name = "desconto")
    private double desconto;

    @Column(name = "pontos")
    private int pontos;

}
