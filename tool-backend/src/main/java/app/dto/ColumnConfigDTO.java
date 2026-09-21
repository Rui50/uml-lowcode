package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// config de coluna JPA (persistence tab) por atributo — só o que a OCL não exprime
@Getter
@Setter
@NoArgsConstructor
public class ColumnConfigDTO {
    private boolean unique;     // @Column(unique = true)
    private Integer precision;  // decimais: total de dígitos
    private Integer scale;      // decimais: casas decimais
    private String name;        // @Column(name) override se vazio = nome do atributo
}
