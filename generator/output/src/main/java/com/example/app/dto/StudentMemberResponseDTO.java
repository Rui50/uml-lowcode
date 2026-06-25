package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.StudentMember;
import com.example.app.models.Library;
import com.example.app.models.Loan;
import com.example.app.models.Book;
import com.example.app.models.LibraryCard;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentMemberResponseDTO {

    private Long id;

    private String course;
    private int maxLoans;
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

    public static StudentMemberResponseDTO fromEntity(StudentMember studentMember) {
        StudentMemberResponseDTO dto = new StudentMemberResponseDTO();
        dto.setId(studentMember.getId());
        dto.setCourse(studentMember.getCourse());
        dto.setMaxLoans(studentMember.getMaxLoans());
        dto.setMemberNumber(studentMember.getMemberNumber());
        dto.setName(studentMember.getName());
        dto.setEmail(studentMember.getEmail());
        dto.setPoints(studentMember.getPoints());
        dto.setActiveLoans(studentMember.getActiveLoans());
        dto.setLibraryId(studentMember.getLibrary() != null
            ? studentMember.getLibrary().getId() : null);
        dto.setLoansIds(studentMember.getLoans() != null
            ? studentMember.getLoans().stream().map(Loan::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setReservedBookIds(studentMember.getReservedBook() != null
            ? studentMember.getReservedBook().stream().map(Book::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setBorrowedBooksIds(studentMember.getBorrowedBooks() != null
            ? studentMember.getBorrowedBooks().stream().map(Book::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setCardId(studentMember.getCard() != null
            ? studentMember.getCard().getId() : null);
        return dto;
    }
}
