package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.Book;
import com.example.app.models.BookStatus;
import com.example.app.models.Loan;
import com.example.app.models.Author;
import com.example.app.models.Member;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookResponseDTO {

    private Long id;

    private String isbn;
    private String title;
    private BookStatus status;

    private List<Long> loansIds;
    private List<Long> authorsIds;
    private List<Long> reservedByIds;
    private Long borrowerId;
    private List<Long> editorsIds;

    public static BookResponseDTO fromEntity(Book book) {
        BookResponseDTO dto = new BookResponseDTO();
        dto.setId(book.getId());
        dto.setIsbn(book.getIsbn());
        dto.setTitle(book.getTitle());
        dto.setStatus(book.getStatus());
        dto.setLoansIds(book.getLoans() != null
            ? book.getLoans().stream().map(Loan::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setAuthorsIds(book.getAuthors() != null
            ? book.getAuthors().stream().map(Author::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setReservedByIds(book.getReservedBy() != null
            ? book.getReservedBy().stream().map(Member::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setBorrowerId(book.getBorrower() != null
            ? book.getBorrower().getId() : null);
        dto.setEditorsIds(book.getEditors() != null
            ? book.getEditors().stream().map(Author::getId).collect(Collectors.toList())
            : new ArrayList<>());
        return dto;
    }
}
