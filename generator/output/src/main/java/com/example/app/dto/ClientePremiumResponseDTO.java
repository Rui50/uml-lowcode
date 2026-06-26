package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.ClientePremium;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientePremiumResponseDTO {

    private Long id;

    private double desconto;
    private int pontos;
    private String nome;
    private String email;

    public static ClientePremiumResponseDTO fromEntity(ClientePremium clientePremium) {
        ClientePremiumResponseDTO dto = new ClientePremiumResponseDTO();
        dto.setId(clientePremium.getId());
        dto.setDesconto(clientePremium.getDesconto());
        dto.setPontos(clientePremium.getPontos());
        dto.setNome(clientePremium.getNome());
        dto.setEmail(clientePremium.getEmail());
        return dto;
    }
}
