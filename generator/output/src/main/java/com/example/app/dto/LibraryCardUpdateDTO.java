package com.example.app.dto;

import lombok.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryCardUpdateDTO {

    private String cardNumber;
    private Integer issuedDay;
    private Long memberId;
}
