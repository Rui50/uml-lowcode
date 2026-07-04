package app.dto.dclass;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RelationFieldDTO {
    private String name;         // nome do campo da relação (role) — igual ao usado nos DTOs gerados
    private boolean collection;  // multivalorada (*) -> não entra no UpdateDTO; senão single ref
}
