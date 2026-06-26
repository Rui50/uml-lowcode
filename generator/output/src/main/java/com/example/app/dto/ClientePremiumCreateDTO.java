package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.ClientePremium;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientePremiumCreateDTO {

    private double desconto;
    private int pontos;
    private String nome;
    private String email;

    public ClientePremium toEntity() {
        ClientePremium clientePremium = new ClientePremium();
        clientePremium.setDesconto(this.getDesconto());
        clientePremium.setPontos(this.getPontos());
        clientePremium.setNome(this.getNome());
        clientePremium.setEmail(this.getEmail());
        return clientePremium;
    }
}
