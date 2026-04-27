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

    private EstadoAluno estado;
    private String nome;
    private String email;
    private int numero;
    private String empresa;

    public AlunoTE toEntity() {
        AlunoTE alunoTE = new AlunoTE();
        if (this.getEstado() != null) alunoTE.setEstado(this.getEstado());
        alunoTE.setNome(this.getNome());
        alunoTE.setEmail(this.getEmail());
        alunoTE.setNumero(this.getNumero());
        alunoTE.setEmpresa(this.getEmpresa());
        return alunoTE;
    }
}
