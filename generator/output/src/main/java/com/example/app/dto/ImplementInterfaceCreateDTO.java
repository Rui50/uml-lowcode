package com.example.app.dto;

import lombok.*;
import com.example.app.models.ImplementInterface;

@Getter
@Setter
@NoArgsConstructor
public class ImplementInterfaceCreateDTO {


    public ImplementInterface toEntity() {
        ImplementInterface implementInterface = new ImplementInterface();
        return implementInterface;
    }
}
