package output.models;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "turno")
public class Turno{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "capacidade")
    private int capacidade;

    // relationship: Turno -> Aluno
    @OneToMany(mappedBy = "turno")
    private List<Aluno> alunos = new ArrayList<>();

}
