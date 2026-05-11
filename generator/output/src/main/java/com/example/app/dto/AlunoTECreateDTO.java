package com.example.app.dto;

import lombok.*;
import com.example.app.models.AlunoTE;
import com.example.app.models.EstadoAluno;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlunoTECreateDTO {

    private String empresa;
    private String email;
    private EstadoAluno estado;
    private String nome;
    private int numero;

    public AlunoTE toEntity() {
        AlunoTE alunoTE = new AlunoTE();
        alunoTE.setEmpresa(this.getEmpresa());
        alunoTE.setEmail(this.getEmail());
        alunoTE.setEstado(this.getEstado());
        alunoTE.setNome(this.getNome());
        alunoTE.setNumero(this.getNumero());
        return alunoTE;
    }
}
