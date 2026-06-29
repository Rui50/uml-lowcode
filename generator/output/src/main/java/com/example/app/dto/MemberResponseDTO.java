package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.Member;
import com.example.app.models.Library;
import com.example.app.models.Loan;
import com.example.app.models.Book;
import com.example.app.models.LibraryCard;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponseDTO {

    private Long id;

    private int memberNumber;
    private String name;
    private String email;
    private int points;
    private int activeLoans;

    private Long libraryId;
    private List<Long> loansIds;
    private List<Long> reservedBookIds;
    private List<Long> borrowedBooksIds;
    private Long cardId;

    public static MemberResponseDTO fromEntity(Member member) {
        MemberResponseDTO dto = new MemberResponseDTO();
        dto.setId(member.getId());
        dto.setMemberNumber(member.getMemberNumber());
        dto.setName(member.getName());
        dto.setEmail(member.getEmail());
        dto.setPoints(member.getPoints());
        dto.setActiveLoans(member.getActiveLoans());
        dto.setLibraryId(member.getLibrary() != null
            ? member.getLibrary().getId() : null);
        dto.setLoansIds(member.getLoans() != null
            ? member.getLoans().stream().map(Loan::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setReservedBookIds(member.getReservedBook() != null
            ? member.getReservedBook().stream().map(Book::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setBorrowedBooksIds(member.getBorrowedBooks() != null
            ? member.getBorrowedBooks().stream().map(Book::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setCardId(member.getCard() != null
            ? member.getCard().getId() : null);
        return dto;
    }
}
