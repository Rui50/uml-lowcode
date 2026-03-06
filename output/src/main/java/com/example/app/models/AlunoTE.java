package output.models;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "alunote")
public class AlunoTE extends Aluno {

    // attributes
    @Column(name = "empresa")
    private String empresa;

}
