package app.dto.dclass;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RelationFieldDTO {
    private String name;         // nome do campo da relação (role) — igual ao usado nos DTOs gerados
    private boolean collection;  // se (*) -> não entra no UpdateDTO -> senão single ref
    private String targetClass;  // classe do outro lado (p/ listar os seus atributos no order-by)
    private boolean mandatory;   // single com lower bound >= 1 (mult "1") -> relação obrigatória
}
