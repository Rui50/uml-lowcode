package com.example.app.dto;

import lombok.*;
import com.example.app.models.testeSubAb;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class testeSubAbCreateDTO {

    private String nada;
    private String testar;

    public testeSubAb toEntity() {
        testeSubAb testeSubAb = new testeSubAb();
        testeSubAb.setNada(this.getNada());
        testeSubAb.setTestar(this.getTestar());
        return testeSubAb;
    }
}
