package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.Cliente;
import com.example.app.models.Encomenda;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteResponseDTO {

    private Long id;

    private String nome;
    private String email;

    private List<Long> encomendasIds;

    public static ClienteResponseDTO fromEntity(Cliente cliente) {
        ClienteResponseDTO dto = new ClienteResponseDTO();
        dto.setId(cliente.getId());
        dto.setNome(cliente.getNome());
        dto.setEmail(cliente.getEmail());
        dto.setEncomendasIds(cliente.getEncomendas() != null
            ? cliente.getEncomendas().stream().map(Encomenda::getId).collect(Collectors.toList())
            : new ArrayList<>());
        return dto;
    }
}
