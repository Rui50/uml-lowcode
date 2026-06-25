package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.LibraryCard;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryCardCreateDTO {

    private String cardNumber;
    private int issuedDay;
    private Long memberId;

    public LibraryCard toEntity() {
        LibraryCard libraryCard = new LibraryCard();
        libraryCard.setCardNumber(this.getCardNumber());
        libraryCard.setIssuedDay(this.getIssuedDay());
        return libraryCard;
    }
}
