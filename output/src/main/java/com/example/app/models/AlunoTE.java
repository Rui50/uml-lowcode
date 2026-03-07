package com.example.app.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "alunote")
public class AlunoTE extends Aluno {

    // attributes
    @Column(name = "empresa")
    private String empresa;

}
