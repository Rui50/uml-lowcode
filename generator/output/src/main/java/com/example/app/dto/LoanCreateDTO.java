package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.Loan;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanCreateDTO {

    private int loanDay;
    private int dueDay;
    private boolean returned;
    private Long memberId;
    private Long bookId;

    public Loan toEntity() {
        Loan loan = new Loan();
        loan.setLoanDay(this.getLoanDay());
        loan.setDueDay(this.getDueDay());
        loan.setReturned(this.getReturned());
        return loan;
    }
}
