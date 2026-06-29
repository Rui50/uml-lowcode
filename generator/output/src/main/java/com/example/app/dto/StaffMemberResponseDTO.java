package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.StaffMember;
import com.example.app.models.Library;
import com.example.app.models.Loan;
import com.example.app.models.Book;
import com.example.app.models.LibraryCard;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffMemberResponseDTO {

    private Long id;

    private String department;
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

    public static StaffMemberResponseDTO fromEntity(StaffMember staffMember) {
        StaffMemberResponseDTO dto = new StaffMemberResponseDTO();
        dto.setId(staffMember.getId());
        dto.setDepartment(staffMember.getDepartment());
        dto.setMemberNumber(staffMember.getMemberNumber());
        dto.setName(staffMember.getName());
        dto.setEmail(staffMember.getEmail());
        dto.setPoints(staffMember.getPoints());
        dto.setActiveLoans(staffMember.getActiveLoans());
        dto.setLibraryId(staffMember.getLibrary() != null
            ? staffMember.getLibrary().getId() : null);
        dto.setLoansIds(staffMember.getLoans() != null
            ? staffMember.getLoans().stream().map(Loan::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setReservedBookIds(staffMember.getReservedBook() != null
            ? staffMember.getReservedBook().stream().map(Book::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setBorrowedBooksIds(staffMember.getBorrowedBooks() != null
            ? staffMember.getBorrowedBooks().stream().map(Book::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setCardId(staffMember.getCard() != null
            ? staffMember.getCard().getId() : null);
        return dto;
    }
}
