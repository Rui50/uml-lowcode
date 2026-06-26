package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.Cliente;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteCreateDTO {

    private String nome;
    private String email;
    private List<Long> encomendasIds;

    public Cliente toEntity() {
        Cliente cliente = new Cliente();
        cliente.setNome(this.getNome());
        cliente.setEmail(this.getEmail());
        return cliente;
    }
}
