package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.LibraryCard;
import com.example.app.models.Member;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryCardResponseDTO {

    private Long id;

    private String cardNumber;
    private int issuedDay;

    private Long memberId;

    public static LibraryCardResponseDTO fromEntity(LibraryCard libraryCard) {
        LibraryCardResponseDTO dto = new LibraryCardResponseDTO();
        dto.setId(libraryCard.getId());
        dto.setCardNumber(libraryCard.getCardNumber());
        dto.setIssuedDay(libraryCard.getIssuedDay());
        dto.setMemberId(libraryCard.getMember() != null
            ? libraryCard.getMember().getId() : null);
        return dto;
    }
}
