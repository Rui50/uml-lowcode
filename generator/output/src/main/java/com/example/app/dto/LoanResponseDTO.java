package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.Loan;
import com.example.app.models.Member;
import com.example.app.models.Book;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanResponseDTO {

    private Long id;

    private int loanDay;
    private int dueDay;
    private boolean returned;

    private Long memberId;
    private Long bookId;

    public static LoanResponseDTO fromEntity(Loan loan) {
        LoanResponseDTO dto = new LoanResponseDTO();
        dto.setId(loan.getId());
        dto.setLoanDay(loan.getLoanDay());
        dto.setDueDay(loan.getDueDay());
        dto.setReturned(loan.getReturned());
        dto.setMemberId(loan.getMember() != null
            ? loan.getMember().getId() : null);
        dto.setBookId(loan.getBook() != null
            ? loan.getBook().getId() : null);
        return dto;
    }
}
